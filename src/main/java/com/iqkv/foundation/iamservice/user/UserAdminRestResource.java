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

package com.iqkv.foundation.iamservice.user;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.ban.BanService;
import com.iqkv.foundation.iamservice.ban.dto.BanDtos;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityService;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/iam/admin/users")
@Tag(name = "User Admin", description = "Platform operator CRUD operations for users — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class UserAdminRestResource {

  private final UserService userService;
  private final PlatformAuthorityService platformAuthorityService;
  private final MembershipService membershipService;
  private final BanService banService;

  public UserAdminRestResource(final UserService userService,
                               final PlatformAuthorityService platformAuthorityService,
                               final MembershipService membershipService,
                               final BanService banService) {
    this.userService = userService;
    this.platformAuthorityService = platformAuthorityService;
    this.membershipService = membershipService;
    this.banService = banService;
  }

  @GetMapping
  @Operation(summary = "List users", description = "Returns a paginated, sorted, and optionally filtered list of users across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of users returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<UserDtos.PagedUserResponse> listUsers(
      @ModelAttribute @Valid UserDtos.UserListQuery query) {
    return ResponseEntity.ok(userService.listUsers(query));
  }

  @GetMapping("/count")
  @Operation(summary = "Count users", description = "Returns the total number of user accounts across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Total user count returned",
                   content = @Content(schema = @Schema(implementation = UserDtos.UserCountResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<UserDtos.UserCountResponse> countUsers() {
    return ResponseEntity.ok(userService.countUsers());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> getUser(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @PostMapping
  @Operation(summary = "Create user", description = "Creates a new user with a random temporary password. The user should reset their password on first login.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "User created",
                   headers = @Header(name = "Location", description = "URL of the created user",
                                     schema = @Schema(type = "string", format = "uri"))),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> createUser(
      @Valid @RequestBody UserDtos.AdminCreateUserRequest request) {
    final var created = userService.createUser(request);
    final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Replace user", description = "Full replacement of firstName and lastName. All fields are required.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> replaceUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @Valid @RequestBody UserDtos.UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request.firstName(), request.lastName(), request.locale(), "system"));
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Partially update user", description = "Updates only the provided fields. Omitted fields are left unchanged.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User patched"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> patchUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @RequestBody UserDtos.AdminUpdateUserRequest request) {
    return ResponseEntity.ok(userService.patchUser(id, request));
  }

  @GetMapping("/{id}/authorities")
  @Operation(summary = "Get user platform authorities")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Authorities found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserAuthoritiesResponse> getUserAuthorities(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    List<String> authorities = platformAuthorityService.getUserAuthorities(id);
    return ResponseEntity.ok(new UserDtos.UserAuthoritiesResponse(id, authorities));
  }

  @PutMapping("/{id}/authorities")
  @Operation(summary = "Update user platform authorities", description = "Full replacement of platform authorities for the user. "
                                                                         + "A platform admin cannot modify their own platform authorities.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Authorities updated"),
      @ApiResponse(responseCode = "400", description = "Validation error or self-modification attempt", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserAuthoritiesResponse> updateUserAuthorities(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @Valid @RequestBody UserDtos.AdminUpdateUserAuthoritiesRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    final UUID actorId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    if (actorId.equals(id)) {
      return ResponseEntity.badRequest().build();
    }
    platformAuthorityService.updateUserAuthorities(id, request.authorities(), actorId.toString());
    List<String> authorities = platformAuthorityService.getUserAuthorities(id);
    return ResponseEntity.ok(new UserDtos.UserAuthoritiesResponse(id, authorities));
  }

  @GetMapping("/{id}/memberships")
  @Operation(summary = "Get user tenant memberships")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Memberships found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<List<UserDtos.UserMembershipResponse>> getUserMemberships(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    userService.getUserById(id);
    return ResponseEntity.ok(membershipService.getUserMemberships(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user", description = "Permanently deletes the user and all associated memberships (cascade).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "User deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    userService.deleteUserById(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/password")
  @Operation(
      summary = "Set user password",
      description = "Forcibly sets a new password for any user. "
                    + "No current password is required — this is an administrative override. "
                    + "The new password must satisfy the platform password policy. "
                    + "All existing sessions for the target user are invalidated on success.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Password set — all user sessions invalidated"),
      @ApiResponse(responseCode = "400", description = "Validation error or password does not meet policy", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> setUserPassword(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @Valid @RequestBody UserDtos.AdminSetUserPasswordRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    final String actorId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    userService.setUserPassword(id, request.newPassword(), actorId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/ban")
  @Operation(
      summary = "Ban user globally",
      description = "Bans a user from the entire platform. The user will be logged out immediately and cannot log in again until unbanned.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User banned successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<BanDtos.BanResponse> banUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @RequestBody(required = false) BanDtos.CreateBanRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    final String actorId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final BanDtos.BanResponse ban = banService.banUserPlatform(id, UUID.fromString(actorId), request != null ? request : new BanDtos.CreateBanRequest(null, null));
    return ResponseEntity.ok(ban);
  }

  @PostMapping("/{id}/unban")
  @Operation(
      summary = "Unban user globally",
      description = "Unbans a user from the entire platform, allowing them to log in again.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "User unbanned successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> unbanUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @AuthenticationPrincipal Jwt jwt) {
    final String actorId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    banService.unbanUserPlatform(id, UUID.fromString(actorId));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/unlock")
  @Operation(
      summary = "Unlock user",
      description = "Unlocks a user's account by resetting failed login attempts, allowing them to log in again.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "User unlocked successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> unlockUser(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    userService.unlockUser(id);
    return ResponseEntity.noContent().build();
  }
}
