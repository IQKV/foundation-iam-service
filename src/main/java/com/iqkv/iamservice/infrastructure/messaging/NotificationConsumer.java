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

package com.iqkv.iamservice.infrastructure.messaging;

import com.iqkv.iamservice.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

  private final EmailService emailService;

  public NotificationConsumer(final EmailService emailService) {
    this.emailService = emailService;
  }

  @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
  public void handleNotification(final NotificationEvent event) {
    try {
      emailService.send(event);
    } catch (final Exception e) {
      // Do NOT rethrow — failed messages route to DLQ via x-dead-letter-exchange
      log.error("Failed to process notification event: type={} recipient={}",
          event.getType(), event.getRecipientEmail(), e);
    }
  }
}
