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

package com.iqkv.iamservice.user;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqkv.iamservice.authentication.AuthenticationService;
import com.iqkv.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.iamservice.security.JwtClaimNames;
import com.iqkv.iamservice.user.dto.UserDtos;

@RestController
@RequestMapping("/api/v1/iam/users")
@Tag(name = "User Management", description = "User profile and tenant discovery operations")
public class UserRestResource {

  private final UserService userService;
  private final AuthenticationService authenticationService;

  public UserRestResource(final UserService userService,
                          final AuthenticationService authenticationService) {
    this.userService = userService;
    this.authenticationService = authenticationService;
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get current user profile")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Profile retrieved"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<UserDtos.UserResponse> getProfile(@AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(userService.getUserById(userId));
  }

  @PatchMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update current user profile")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Profile updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<UserDtos.UserResponse> updateProfile(
      @AuthenticationPrincipal final Jwt jwt,
      @Valid @RequestBody final UserDtos.UpdateUserRequest request) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(
        userService.updateUser(userId, request.firstName(), request.lastName(), userId.toString()));
  }

  @DeleteMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Delete current user membership from tenant")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Membership removed"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Membership not found")
  })
  public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    userService.deleteUser(userId, tenantKey);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/tenants")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Discover tenants for a user (credential-gated, no JWT required)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant list returned"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  public ResponseEntity<List<AuthenticationDtos.TenantMembershipSummary>> listUserTenants(
      @Valid @RequestBody final AuthenticationDtos.TenantDiscoveryRequest request) {
    return ResponseEntity.ok(
        authenticationService.listUserTenants(request.email(), request.password()));
  }
}
