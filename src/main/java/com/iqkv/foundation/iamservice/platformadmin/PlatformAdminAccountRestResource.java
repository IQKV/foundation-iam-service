/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.iamservice.platformadmin;

import jakarta.validation.Valid;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform operator self-service account API.
 *
 * <p>Separate from tenant-scoped {@code /users/me}. No {@code X-Tenant-ID} header is required
 * or used — platform operators are not members of any organization.
 */
@RestController
@RequestMapping("/api/v1/iam/auth/admin/me")
@Tag(name = "Platform Admin Account",
     description = "Self-service profile for platform operators — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class PlatformAdminAccountRestResource {

  private final PlatformAdminAccountService platformAdminAccountService;

  public PlatformAdminAccountRestResource(final PlatformAdminAccountService platformAdminAccountService) {
    this.platformAdminAccountService = platformAdminAccountService;
  }

  @GetMapping
  @Operation(
      summary = "Get platform admin account",
      description = "Returns the authenticated platform operator profile and platform-level authorities. "
                    + "Does not include tenant memberships or organization context.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Account profile returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<PlatformAdminDtos.AdminAccountResponse> getAccount(
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(platformAdminAccountService.getAccount(userId));
  }

  @PatchMapping
  @Operation(
      summary = "Update platform admin account",
      description = "Updates the authenticated platform operator's first and last name.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Account updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<PlatformAdminDtos.AdminAccountResponse> updateAccount(
      @AuthenticationPrincipal final Jwt jwt,
      @Valid @RequestBody final PlatformAdminDtos.AdminUpdateAccountRequest request) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(platformAdminAccountService.updateAccount(userId, request));
  }

  @PostMapping("/password")
  @Operation(
      summary = "Change own password",
      description = "Allows the authenticated platform operator to change their own password. "
                    + "Requires the current password for re-authentication. "
                    + "On success, all existing sessions are invalidated.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Password changed — all sessions invalidated"),
      @ApiResponse(responseCode = "400", description = "Validation error or new password does not meet policy", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized or current password incorrect", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal final Jwt jwt,
      @Valid @RequestBody final PlatformAdminDtos.AdminChangePasswordRequest request) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    platformAdminAccountService.changePassword(userId, request.currentPassword(), request.newPassword());
    return ResponseEntity.noContent().build();
  }
}
