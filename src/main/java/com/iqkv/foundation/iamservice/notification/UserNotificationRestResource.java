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

package com.iqkv.foundation.iamservice.notification;

import java.util.UUID;

import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtos;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/users/notifications")
@Tag(name = "User Notifications", description = "Endpoints for managing personal in-app notifications")
@SecurityRequirement(name = "bearerAuth")
public class UserNotificationRestResource {

  private final UserNotificationService notificationService;

  public UserNotificationRestResource(final UserNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  // ---------------------------------------------------------------------------
  // Collection
  // ---------------------------------------------------------------------------

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "List notifications",
      description = "Returns a paginated list of notifications for the current user. "
                    + "Filter by read state with the optional `isRead` parameter.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<UserNotificationDtos.UserNotificationListResponse> getNotifications(
      @AuthenticationPrincipal final Jwt jwt,
      @RequestParam(defaultValue = "10") final int limit,
      @RequestParam(defaultValue = "0") final int offset,
      @RequestParam(required = false) final Boolean isRead) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(notificationService.getNotifications(userId, limit, offset, isRead));
  }

  @PatchMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Bulk-update notifications",
      description = "Applies a partial update to all notifications of the current user. "
                    + "Currently supports marking all as read (`isRead: true`).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Update applied"),
      @ApiResponse(responseCode = "400", description = "Invalid patch body"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> patchAll(
      @AuthenticationPrincipal final Jwt jwt,
      @RequestBody final UserNotificationDtos.NotificationPatchRequest patch) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    if (Boolean.TRUE.equals(patch.isRead())) {
      notificationService.markAllAsRead(userId);
    }
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Delete all notifications",
      description = "Permanently removes all notifications for the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    notificationService.deleteAllNotifications(userId);
    return ResponseEntity.noContent().build();
  }

  // ---------------------------------------------------------------------------
  // Unread sub-resource  GET /unread/count
  // ---------------------------------------------------------------------------

  @GetMapping("/unread/count")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get unread count",
      description = "Returns the number of unread notifications for the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<UserNotificationDtos.UnreadCountResponse> getUnreadCount(
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(new UserNotificationDtos.UnreadCountResponse(notificationService.countUnread(userId)));
  }

  // ---------------------------------------------------------------------------
  // Single item
  // ---------------------------------------------------------------------------

  @PatchMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Partially update a notification",
      description = "Applies a partial update to a single notification. "
                    + "Currently supports marking it as read (`isRead: true`).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Update applied"),
      @ApiResponse(responseCode = "400", description = "Invalid patch body"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Notification not found")
  })
  public ResponseEntity<Void> patchOne(
      @AuthenticationPrincipal final Jwt jwt,
      @PathVariable final UUID id,
      @RequestBody final UserNotificationDtos.NotificationPatchRequest patch) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    if (Boolean.TRUE.equals(patch.isRead())) {
      notificationService.markAsRead(userId, id);
    }
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Delete a notification",
      description = "Permanently removes a single notification belonging to the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Notification not found")
  })
  public ResponseEntity<Void> deleteOne(
      @AuthenticationPrincipal final Jwt jwt,
      @PathVariable final UUID id) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    notificationService.deleteNotification(userId, id);
    return ResponseEntity.noContent().build();
  }
}
