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

package com.iqscaffold.iam.authentication;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqscaffold.iam.authentication.dto.AuthenticationDtos;
import com.iqscaffold.iam.security.JwtClaimNames;
import com.iqscaffold.iam.user.UserService;
import com.iqscaffold.iam.user.dto.UserDtos;

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
          + "Returns 201 with tenantStatus=PROVISIONING; poll GET /api/v1/iam/tenants/{tenantKey} until ACTIVE.")
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

  @PostMapping("/signin")
  @Operation(summary = "Sign in with email and password",
      description = "Authenticates a user within the tenant identified by X-Tenant-ID header. "
          + "Returns an RS256 access token (15 min) and refresh token (7 days).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens issued"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "403", description = "Account locked, tenant suspended, or no membership")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> signIn(
      @Valid @RequestBody final AuthenticationDtos.SignInRequest request) {
    return ResponseEntity.ok(authenticationService.signIn(request));
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
      @Valid @RequestBody final AuthenticationDtos.RefreshTokenRequest request) {
    return ResponseEntity.ok(authenticationService.refresh(request));
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
      description = "Decodes the Bearer token and returns userId, email, tenantId, and authorities. "
          + "Intended for API gateway introspection.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token valid, user context returned"),
      @ApiResponse(responseCode = "401", description = "Token invalid or revoked")
  })
  public ResponseEntity<AuthenticationDtos.ValidateTokenResponse> validate(
      @AuthenticationPrincipal final Jwt jwt) {
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final String email = jwt.getClaimAsString(JwtClaimNames.EMAIL);
    final String tenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final List<String> authorities = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);
    return ResponseEntity.ok(new AuthenticationDtos.ValidateTokenResponse(
        UUID.fromString(userId), email, tenantId,
        authorities != null ? authorities : List.of()));
  }
}
