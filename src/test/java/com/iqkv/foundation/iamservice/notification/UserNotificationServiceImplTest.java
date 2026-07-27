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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotificationService Unit Tests")
class UserNotificationServiceImplTest {

  @Mock
  private UserNotificationMapper notificationMapper;

  @InjectMocks
  private UserNotificationServiceImpl notificationService;

  @Test
  @DisplayName("Should return notifications for user")
  void shouldReturnNotificationsForUser() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final UserNotification notification = new UserNotification();
    notification.setId(UUID.randomUUID());
    notification.setTargetUserId(userId);
    notification.setType("TEST_TYPE");
    notification.setTitle("Test Title");

    when(notificationMapper.findAllByTargetUserId(eq(userId), eq(10), eq(0), eq(null)))
        .thenReturn(List.of(notification));
    when(notificationMapper.countByTargetUserId(eq(userId), eq(null))).thenReturn(1L);
    when(notificationMapper.countByTargetUserId(eq(userId), eq(false))).thenReturn(1L);

    // Act
    final UserNotificationDtos.UserNotificationListResponse result = notificationService.getNotifications(userId, 10, 0, null);

    // Assert
    assertThat(result.items()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.unreadCount()).isEqualTo(1);
    assertThat(result.items().get(0).title()).isEqualTo("Test Title");
  }

  @Test
  @DisplayName("Should mark notification as read")
  void shouldMarkAsRead() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final UUID notificationId = UUID.randomUUID();

    // Act
    notificationService.markAsRead(userId, notificationId);

    // Assert
    verify(notificationMapper).markAsRead(eq(notificationId), any());
  }

  @Test
  @DisplayName("Should create notification")
  void shouldCreateNotification() {
    // Arrange
    final UserNotification notification = new UserNotification();
    notification.setTargetUserId(UUID.randomUUID());
    notification.setType("TEST");

    // Act
    notificationService.createNotification(notification);

    // Assert
    assertThat(notification.getId()).isNotNull();
    assertThat(notification.getCreatedAt()).isNotNull();
    verify(notificationMapper).insert(notification);
  }
}
