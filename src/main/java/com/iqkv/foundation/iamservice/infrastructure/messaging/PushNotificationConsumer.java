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

import java.util.Set;
import java.util.UUID;

import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.notification.UserNotification;
import com.iqkv.foundation.iamservice.notification.UserNotificationService;
import com.iqkv.foundation.iamservice.notification.dto.UserNotificationDtoMapper;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes {@link UserPushNotificationEvent} from {@code notification.iam.push} and delivers
 * them as in-app notifications without sending any email. Any platform service can publish to
 * this routing key to show a UI toast / bell notification to a specific user in real time.
 *
 * <p>Processing steps:
 * <ol>
 *   <li>Resolve the target user by {@code targetUserId}.
 *   <li>Persist a {@code user_notifications} row (title + message already resolved by the publisher).
 *   <li>Push the row over STOMP to {@code /user/queue/notifications} so the UI bell reacts instantly.
 * </ol>
 *
 * <p>Failures are caught and logged — messages are never requeued from here; the DLQ captures
 * anything that the listener container cannot deliver after its bounded retry.
 */
@Component
public class PushNotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(PushNotificationConsumer.class);

  /** Allowed severity values — anything outside this set is normalised to INFO. */
  private static final Set<String> VALID_SEVERITIES = Set.of("INFO", "SUCCESS", "WARNING", "ERROR");

  private final UserNotificationService userNotificationService;
  private final UserMapper userMapper;
  private final SimpMessagingTemplate messagingTemplate;
  private final JsonMapper jsonMapper;

  public PushNotificationConsumer(final UserNotificationService userNotificationService,
                                  final UserMapper userMapper,
                                  final SimpMessagingTemplate messagingTemplate,
                                  final JsonMapper jsonMapper) {
    this.userNotificationService = userNotificationService;
    this.userMapper = userMapper;
    this.messagingTemplate = messagingTemplate;
    this.jsonMapper = jsonMapper;
  }

  @RabbitListener(queues = RabbitMQConfig.PUSH_NOTIFICATIONS_QUEUE)
  public void handlePushNotification(final UserPushNotificationEvent event) {
    if (event.targetUserId() == null) {
      log.warn("Dropping push notification with null targetUserId: type={}", event.type());
      return;
    }
    try {
      userMapper.findById(event.targetUserId()).ifPresentOrElse(
          user -> persist(event, user.getId()),
          () -> log.warn("Target user not found for push notification: id={} type={}",
              event.targetUserId(), event.type())
      );
    } catch (final Exception e) {
      log.error("Failed to process push notification: type={} user={}",
          event.type(), event.targetUserId(), e);
    }
  }

  private void persist(final UserPushNotificationEvent event, final UUID userId) {
    final UserNotification notification = new UserNotification();
    notification.setId(UUID.randomUUID());
    notification.setTargetUserId(userId);
    notification.setType(event.type() != null ? event.type() : "PUSH");
    notification.setSeverity(VALID_SEVERITIES.contains(event.severity()) ? event.severity() : "INFO");
    notification.setTitle(event.title());
    notification.setMessage(event.message());
    notification.setRead(false);

    if (event.payload() != null && !event.payload().isEmpty()) {
      try {
        notification.setPayload(jsonMapper.writeValueAsString(event.payload()));
      } catch (final Exception e) {
        log.warn("Failed to serialize push notification payload for user={}", userId, e);
      }
    }

    userNotificationService.createNotification(notification);
    log.debug("Persisted push notification: id={} type={} user={}",
        notification.getId(), notification.getType(), userId);

    try {
      messagingTemplate.convertAndSendToUser(
          userId.toString(),
          "/queue/notifications",
          UserNotificationDtoMapper.toResponse(notification)
      );
    } catch (final Exception e) {
      log.warn("Failed to push WebSocket notification for user={}", userId, e);
    }
  }
}
