/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.iamservice.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException;
import com.iqkv.foundation.iamservice.tenancy.TenantContext;
import com.iqkv.foundation.iamservice.user.UserService;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/auth")
@Tag(name = "Authentication", description = "Signup, signin, token refresh, signout, and token validation")
public class AuthenticationRestResource {

  private final AuthenticationService authenticationService;
  private final UserService userService;

  public AuthenticationRestResource(final AuthenticationService authenticationService,
                                    final UserService userService) {
    this.authenticationService = authenticationService;
    this.userService = userService;
  }

  @PostMapping("/signup")
  @Operation(summary = "Register a new user and create a tenant",
             description = "Creates a global user account, a new tenant, and a TENANT_OWNER membership in one step. "
                           + "Returns 201 with tenantStatus=PROVISIONING; poll GET /api/v1/iam/auth/signup/status/{tenantKey} until ACTIVE.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "User and tenant created"),
      @ApiResponse(responseCode = "409", description = "Tenant name already taken or user already a member")
  })
  public ResponseEntity<UserDtos.SignupResponse> signup(
      @Valid @RequestBody final UserDtos.RegisterUserRequest request) {
    final var response = userService.registerUser(request);
    return ResponseEntity
        .created(URI.create("/api/v1/iam/users/me"))
        .body(response);
  }

  @GetMapping("/signup/status/{tenantKey}")
  @Operation(summary = "Poll tenant provisioning status after signup",
             description = "Public endpoint — returns only the provisioning status of the tenant. "
                           + "Poll until tenantStatus is ACTIVE before attempting sign-in. "
                           + "Returns PROVISIONING, ACTIVE, or PROVISIONING_FAILED.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Status returned"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<AuthenticationDtos.SignupStatusResponse> signupStatus(
      @PathVariable final String tenantKey) {
    final String status = authenticationService.getProvisioningStatus(tenantKey);
    return ResponseEntity.ok(new AuthenticationDtos.SignupStatusResponse(tenantKey, status));
  }

  @PostMapping("/signin")
  @Operation(summary = "Sign in with email and password",
             description = "Authenticates a user within the tenant identified by X-Tenant-ID header. "
                           + "Returns an RS256 access token (15 min) and refresh token (7 days).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens issued"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "403", description = "Account not active (suspended/locked/deleted), account locked by brute-force policy, tenant suspended, or no membership")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> signIn(
      @Valid @RequestBody final AuthenticationDtos.SignInRequest request,
      @RequestHeader("X-Tenant-ID") final String tenantKey) {
    try {
      TenantContext.setCurrentTenant(tenantKey);
      return ResponseEntity.ok(authenticationService.signIn(request));
    } finally {
      TenantContext.clear();
    }
  }

  @PostMapping("/exchange")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Exchange tokens for a different tenant",
             description = "Issues a new access/refresh token pair for the requested tenantKey. "
                           + "Requires a valid Bearer access token and an ACTIVE membership in the target tenant.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens issued"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "No membership or tenant not available"),
      @ApiResponse(responseCode = "400", description = "Validation error")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> exchangeTenant(
      @AuthenticationPrincipal final Jwt jwt,
      @Valid @RequestBody final AuthenticationDtos.TenantExchangeRequest request) {
    final String type = jwt.getClaimAsString(JwtClaimNames.TYPE);
    if (!JwtClaimNames.TYPE_ACCESS.equals(type)) {
      throw new InvalidTokenTypeException();
    }

    final String userIdClaim = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    if (userIdClaim == null || userIdClaim.isBlank()) {
      throw new InvalidTokenTypeException();
    }
    final UUID userId = UUID.fromString(userIdClaim);
    return ResponseEntity.ok(authenticationService.exchangeTenant(userId, request.tenantKey()));
  }

  @PostMapping("/admin/signin")
  @Operation(summary = "Platform admin sign-in",
             description = "Authenticates a user using platform-level authortities only. "
                           + "No X-Tenant-ID header is required or used. "
                           + "Returns 403 if the user has no platform-level authortities (e.g. PLATFORM_ADMIN). "
                           + "The issued access token carries a null tenant_id claim.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens issued"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "403", description = "Account not active (suspended/locked/deleted), account locked by brute-force policy, or no platform-level authortities")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> adminSignIn(
      @Valid @RequestBody final AuthenticationDtos.SignInRequest request) {
    return ResponseEntity.ok(authenticationService.adminSignIn(request));
  }

  @PostMapping("/admin/refresh")
  @Operation(summary = "Platform admin token refresh",
             description = "Issues a new platform-scoped access token and refresh token pair using a valid refresh token. "
                           + "No X-Tenant-ID header is required or used. "
                           + "Returns 403 if the user no longer holds any platform-level authorities.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "New token pair issued"),
      @ApiResponse(responseCode = "401", description = "Invalid, expired, or wrong-type token"),
      @ApiResponse(responseCode = "403", description = "User no longer has platform-level authorities")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> adminRefresh(
      @Valid @RequestBody final AuthenticationDtos.RefreshTokenRequest request) {
    return ResponseEntity.ok(authenticationService.adminRefresh(request));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token",
             description = "Issues a new access token and refresh token pair using a valid refresh token. "
                           + "The refresh token type claim must equal 'refresh'.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "New token pair issued"),
      @ApiResponse(responseCode = "401", description = "Invalid, expired, or wrong-type token"),
      @ApiResponse(responseCode = "403", description = "Tenant context mismatch or tenant suspended")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> refresh(
      @Valid @RequestBody final AuthenticationDtos.RefreshTokenRequest request,
      @RequestHeader("X-Tenant-ID") final String tenantKey) {
    try {
      TenantContext.setCurrentTenant(tenantKey);
      return ResponseEntity.ok(authenticationService.refresh(request));
    } finally {
      TenantContext.clear();
    }
  }

  @PostMapping("/signout")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Sign out (revoke current token)",
             description = "Adds the current token's JTI to the denylist. Subsequent requests with this token return 401.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Signed out"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> signOut(@AuthenticationPrincipal final Jwt jwt) {
    final String jti = jwt.getId();
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final Instant expiresAt = jwt.getExpiresAt();
    // interface: signOut(jti, userId, expiresAt)
    authenticationService.signOut(jti, userId, expiresAt != null ? expiresAt.toString() : Instant.now().toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/signout-all")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Sign out from all sessions",
             description = "Sets last_global_signout_at on the user record. All tokens issued before this timestamp are revoked.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "All sessions invalidated"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> signOutAll(@AuthenticationPrincipal final Jwt jwt) {
    final String jti = jwt.getId();
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final Instant expiresAt = jwt.getExpiresAt();
    authenticationService.signOutAll(userId, jti, expiresAt != null ? expiresAt.toString() : Instant.now().toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/validate")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Validate token and return user context",
             description = "Decodes the Bearer token, checks the denylist, and returns userId, email, tenantId, and authorities. "
                           + "Intended for API gateway introspection.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token valid, user context returned"),
      @ApiResponse(responseCode = "401", description = "Token invalid or revoked")
  })
  public ResponseEntity<AuthenticationDtos.ValidateTokenResponse> validate(
      final HttpServletRequest request) {
    final String authorization = request.getHeader("Authorization");
    final String token = authorization != null && authorization.startsWith("Bearer ")
        ? authorization.substring(7) : "";
    return ResponseEntity.ok(authenticationService.validateToken(token));
  }
}
