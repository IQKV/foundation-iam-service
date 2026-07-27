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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iqkv.foundation.iamservice.announcement.FanOutService;
import com.iqkv.foundation.iamservice.announcement.SiteAnnouncement;
import com.iqkv.foundation.iamservice.announcement.SiteAnnouncementMapper;
import com.iqkv.foundation.iamservice.announcement.SiteAnnouncementStatus;
import com.iqkv.foundation.iamservice.announcement.SiteAnnouncementTranslation;
import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.notification.UserNotification;
import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtos;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementConsumer {

  private static final Logger log = LoggerFactory.getLogger(AnnouncementConsumer.class);
  private static final int BATCH_SIZE = 1000;

  private final SiteAnnouncementMapper announcementMapper;
  private final UserMapper userMapper;
  private final FanOutService fanOutService;
  private final SimpMessagingTemplate messagingTemplate;

  public AnnouncementConsumer(final SiteAnnouncementMapper announcementMapper,
                              final UserMapper userMapper,
                              final FanOutService fanOutService,
                              final SimpMessagingTemplate messagingTemplate) {
    this.announcementMapper = announcementMapper;
    this.userMapper = userMapper;
    this.fanOutService = fanOutService;
    this.messagingTemplate = messagingTemplate;
  }

  @RabbitListener(queues = RabbitMQConfig.ANNOUNCEMENTS_QUEUE)
  public void handleAnnouncementPublish(final AnnouncementPublishEvent event) {
    log.info("Starting fan-out for announcement: {}", event.announcementId());

    final SiteAnnouncement announcement = announcementMapper.findById(event.announcementId())
        .orElseThrow(() -> new RuntimeException("Announcement not found: " + event.announcementId()));

    if (announcement.getStatus() != SiteAnnouncementStatus.PENDING) {
      log.warn("Announcement {} is in status {}, skipping fan-out", event.announcementId(), announcement.getStatus());
      return;
    }

    // Transition to PUBLISHING
    fanOutService.updateStatus(event.announcementId(), SiteAnnouncementStatus.PUBLISHING);

    final Map<String, SiteAnnouncementTranslation> translationsByLocale = announcement.getTranslations()
        .stream()
        .collect(Collectors.toMap(SiteAnnouncementTranslation::getLocale, t -> t));

    final SiteAnnouncementTranslation defaultTranslation = translationsByLocale.getOrDefault("en-US",
        announcement.getTranslations().isEmpty() ? null : announcement.getTranslations().get(0));

    if (defaultTranslation == null) {
      log.error("No translations found for announcement: {}", announcement.getId());
      fanOutService.updateStatus(event.announcementId(), SiteAnnouncementStatus.FAILED);
      throw new RuntimeException("No translations found for announcement: " + announcement.getId());
    }

    try (Cursor<User> userCursor = userMapper.findAllStreaming()) {
      List<UserNotification> batch = new ArrayList<>(BATCH_SIZE);
      int totalProcessed = 0;

      for (final User user : userCursor) {
        final SiteAnnouncementTranslation translation = translationsByLocale.getOrDefault(user.getLocale(), defaultTranslation);

        final UserNotification notification = new UserNotification();
        notification.setId(UUID.randomUUID());
        notification.setTargetUserId(user.getId());
        notification.setLocale(user.getLocale());
        notification.setType("ANNOUNCEMENT");
        notification.setSeverity("INFO");
        notification.setTitle(translation.getTitle());
        notification.setMessage(translation.getMessage());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        batch.add(notification);

        if (batch.size() >= BATCH_SIZE) {
          fanOutService.saveBatch(batch);
          totalProcessed += batch.size();
          batch.clear();
          log.debug("Processed batch of {} notifications for announcement {}", totalProcessed, announcement.getId());
        }
      }

      if (!batch.isEmpty()) {
        fanOutService.saveBatch(batch);
        totalProcessed += batch.size();
      }

      // Final Step: Real-time Broadcast via WebSockets
      try {
        final var broadcast = new UserNotificationDtos.AnnouncementBroadcastResponse(
            announcement.getId(),
            "ANNOUNCEMENT",
            "INFO",
            defaultTranslation.getTitle(),
            defaultTranslation.getMessage(),
            LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/announcements", broadcast);
      } catch (final Exception e) {
        log.warn("Failed to push global announcement WebSocket broadcast for announcement {}", announcement.getId(), e);
      }

      // Transition to PUBLISHED
      fanOutService.updateStatus(event.announcementId(), SiteAnnouncementStatus.PUBLISHED);
      log.info("Finished fan-out for announcement {}: {} notifications created", announcement.getId(), totalProcessed);

    } catch (final Exception e) {
      log.error("Failed to process fan-out for announcement: {}", announcement.getId(), e);
      fanOutService.updateStatus(event.announcementId(), SiteAnnouncementStatus.FAILED);
      throw new RuntimeException("Fan-out failed", e);
    }
  }
}
