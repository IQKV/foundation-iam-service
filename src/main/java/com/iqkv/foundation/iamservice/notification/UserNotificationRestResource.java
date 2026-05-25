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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get user notifications", description = "Retrieves a paginated list of notifications for the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
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

  @PutMapping("/{id}/read")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read for the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Notification marked as read"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> markAsRead(
      @AuthenticationPrincipal final Jwt jwt,
      @PathVariable final UUID id) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    notificationService.markAsRead(userId, id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/read-all")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "All notifications marked as read"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal final Jwt jwt) {
    final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    notificationService.markAllAsRead(userId);
    return ResponseEntity.noContent().build();
  }
}
