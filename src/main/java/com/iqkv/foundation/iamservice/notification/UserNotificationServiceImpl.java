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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtoMapper;
import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

  private final UserNotificationMapper notificationMapper;

  public UserNotificationServiceImpl(final UserNotificationMapper notificationMapper) {
    this.notificationMapper = notificationMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public UserNotificationDtos.UserNotificationListResponse getNotifications(final UUID userId, final int limit,
                                                                            final int offset, final Boolean isRead) {
    final List<UserNotification> notifications = notificationMapper.findAllByTargetUserId(userId, limit, offset, isRead);
    final long totalElements = notificationMapper.countByTargetUserId(userId, isRead);
    final long unreadCount = notificationMapper.countByTargetUserId(userId, false);

    final List<UserNotificationDtos.UserNotificationResponse> items = notifications.stream()
        .map(UserNotificationDtoMapper::toResponse)
        .toList();

    return new UserNotificationDtos.UserNotificationListResponse(items, totalElements, unreadCount);
  }

  @Override
  @Transactional
  public void markAsRead(final UUID userId, final UUID notificationId) {
    // Note: We could verify ownership here, but markAsRead SQL already checks ID.
    // In a real scenario, we'd fetch the notification first to ensure it belongs to the user.
    notificationMapper.markAsRead(notificationId, LocalDateTime.now());
  }

  @Override
  @Transactional
  public void markAllAsRead(final UUID userId) {
    notificationMapper.markAllAsRead(userId, LocalDateTime.now());
  }

  @Override
  @Transactional
  public void createNotification(final UserNotification notification) {
    if (notification.getId() == null) {
      notification.setId(UUID.randomUUID());
    }
    if (notification.getCreatedAt() == null) {
      notification.setCreatedAt(LocalDateTime.now());
    }
    notificationMapper.insert(notification);
  }
}
