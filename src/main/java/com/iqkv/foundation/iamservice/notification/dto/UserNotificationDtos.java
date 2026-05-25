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

package com.iqkv.foundation.iamservice.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

public final class UserNotificationDtos {

  private UserNotificationDtos() {
  }

  public record UserNotificationResponse(
      UUID id,
      String type,
      String severity,
      String title,
      String message,
      @JsonRawValue String payload,
      boolean isRead,
      LocalDateTime createdAt,
      LocalDateTime readAt) {
  }

  public record UserNotificationListResponse(
      List<UserNotificationResponse> items,
      long totalElements,
      long unreadCount) {
  }
}
