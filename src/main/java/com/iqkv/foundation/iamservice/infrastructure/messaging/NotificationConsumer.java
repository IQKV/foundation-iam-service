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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.notification.UserNotification;
import com.iqkv.foundation.iamservice.notification.UserNotificationService;
import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtoMapper;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

  private final EmailService emailService;
  private final UserNotificationService userNotificationService;
  private final UserMapper userMapper;
  private final MessageSource messageSource;
  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  public NotificationConsumer(final EmailService emailService,
                              final UserNotificationService userNotificationService,
                              final UserMapper userMapper,
                              final MessageSource messageSource,
                              final ObjectMapper objectMapper,
                              final SimpMessagingTemplate messagingTemplate) {
    this.emailService = emailService;
    this.userNotificationService = userNotificationService;
    this.userMapper = userMapper;
    this.messageSource = messageSource;
    this.objectMapper = objectMapper;
    this.messagingTemplate = messagingTemplate;
  }

  @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
  public void handleNotification(final NotificationEvent event) {
    try {
      // 1. Send Email
      emailService.send(event);

      // 2. Persist In-App Notification
      persistInAppNotification(event);
    } catch (final Exception e) {
      // Do NOT rethrow — failed messages route to DLQ via x-dead-letter-exchange
      log.error("Failed to process notification event: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }

  private void persistInAppNotification(final NotificationEvent event) {
    if (event.getTargetUserId() != null) {
      userMapper.findById(event.getTargetUserId()).ifPresentOrElse(
          user -> doPersist(event, user),
          () -> log.warn("Target user not found for in-app notification: id={}", event.getTargetUserId())
      );
    } else {
      userMapper.findByEmail(event.getRecipientEmail()).ifPresentOrElse(
          user -> doPersist(event, user),
          () -> log.warn("Target user not found for in-app notification: email={}", event.getRecipientEmail())
      );
    }
  }

  private void doPersist(final NotificationEvent event, final com.iqkv.foundation.iamservice.user.User user) {
    final String localeTag = event.getLocale() != null ? event.getLocale() : user.getLocale();
    final Locale locale = Locale.forLanguageTag(localeTag != null ? localeTag : "en-US");

    final String title = messageSource.getMessage(
        "notification." + event.getType() + ".title",
        null,
        event.getType().name(),
        locale
    );

    final String message = messageSource.getMessage(
        "notification." + event.getType() + ".message",
        null,
        null,
        locale
    );

    final UserNotification notification = new UserNotification();
    notification.setId(UUID.randomUUID());
    notification.setTargetUserId(user.getId());
    notification.setLocale(locale.toLanguageTag());
    notification.setType(event.getType().name());
    notification.setSeverity("INFO");
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setRead(false);

    if (event.getPayload() != null && !event.getPayload().isEmpty()) {
      try {
        notification.setPayload(objectMapper.writeValueAsString(event.getPayload()));
      } catch (final Exception e) {
        log.warn("Failed to serialize notification payload for user={}", user.getId(), e);
      }
    }

    userNotificationService.createNotification(notification);
    log.debug("Persisted in-app notification: id={} type={} user={}",
        notification.getId(), notification.getType(), user.getId());

    // 3. Push to WebSocket
    try {
      messagingTemplate.convertAndSendToUser(
          user.getId().toString(),
          "/queue/notifications",
          UserNotificationDtoMapper.toResponse(notification)
      );
    } catch (final Exception e) {
      log.warn("Failed to push WebSocket notification for user={}", user.getId(), e);
    }
  }
}
