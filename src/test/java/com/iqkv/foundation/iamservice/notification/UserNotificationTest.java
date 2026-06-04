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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserNotification Tests")
class UserNotificationTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    UserNotification notification = new UserNotification();
    UUID id = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    String locale = "en-US";
    String type = "INFO";
    String severity = "INFO";
    String title = "Welcome";
    String message = "Hello World";
    String payload = "{}";
    boolean isRead = true;
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime readAt = LocalDateTime.now();

    notification.setId(id);
    notification.setTargetUserId(targetUserId);
    notification.setLocale(locale);
    notification.setType(type);
    notification.setSeverity(severity);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setPayload(payload);
    notification.setRead(isRead);
    notification.setCreatedAt(createdAt);
    notification.setReadAt(readAt);

    assertThat(notification.getId()).isEqualTo(id);
    assertThat(notification.getTargetUserId()).isEqualTo(targetUserId);
    assertThat(notification.getLocale()).isEqualTo(locale);
    assertThat(notification.getType()).isEqualTo(type);
    assertThat(notification.getSeverity()).isEqualTo(severity);
    assertThat(notification.getTitle()).isEqualTo(title);
    assertThat(notification.getMessage()).isEqualTo(message);
    assertThat(notification.getPayload()).isEqualTo(payload);
    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
    assertThat(notification.getReadAt()).isEqualTo(readAt);
  }
}
