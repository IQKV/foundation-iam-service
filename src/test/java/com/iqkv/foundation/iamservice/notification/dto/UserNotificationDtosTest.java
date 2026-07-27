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

package com.iqkv.foundation.iamservice.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserNotificationDtos Tests")
class UserNotificationDtosTest {

  @Test
  @DisplayName("Should create UserNotificationResponse")
  void shouldCreateUserNotificationResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var readAt = LocalDateTime.now();
    var response = new UserNotificationDtos.UserNotificationResponse(
        id,
        "INFO",
        "INFO",
        "Notification Title",
        "Notification Message",
        "{}",
        true,
        createdAt,
        readAt
    );

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.type()).isEqualTo("INFO");
    assertThat(response.severity()).isEqualTo("INFO");
    assertThat(response.title()).isEqualTo("Notification Title");
    assertThat(response.message()).isEqualTo("Notification Message");
    assertThat(response.payload()).isEqualTo("{}");
    assertThat(response.isRead()).isTrue();
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.readAt()).isEqualTo(readAt);
  }

  @Test
  @DisplayName("Should create UserNotificationListResponse")
  void shouldCreateUserNotificationListResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var readAt = LocalDateTime.now();
    var item = new UserNotificationDtos.UserNotificationResponse(
        id,
        "INFO",
        "INFO",
        "Notification Title",
        "Notification Message",
        "{}",
        true,
        createdAt,
        readAt
    );
    var response = new UserNotificationDtos.UserNotificationListResponse(
        List.of(item),
        1L,
        0L
    );

    assertThat(response.items()).hasSize(1);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.unreadCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("Should create UnreadCountResponse")
  void shouldCreateUnreadCountResponse() {
    var response = new UserNotificationDtos.UnreadCountResponse(5L);

    assertThat(response.unreadCount()).isEqualTo(5L);
  }

  @Test
  @DisplayName("Should create NotificationPatchRequest")
  void shouldCreateNotificationPatchRequest() {
    var request = new UserNotificationDtos.NotificationPatchRequest(true);

    assertThat(request.isRead()).isTrue();
  }

  @Test
  @DisplayName("Should create AnnouncementBroadcastResponse")
  void shouldCreateAnnouncementBroadcastResponse() {
    var announcementId = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var response = new UserNotificationDtos.AnnouncementBroadcastResponse(
        announcementId,
        "INFO",
        "INFO",
        "Announcement Title",
        "Announcement Message",
        createdAt
    );

    assertThat(response.announcementId()).isEqualTo(announcementId);
    assertThat(response.type()).isEqualTo("INFO");
    assertThat(response.severity()).isEqualTo("INFO");
    assertThat(response.title()).isEqualTo("Announcement Title");
    assertThat(response.message()).isEqualTo("Announcement Message");
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }
}
