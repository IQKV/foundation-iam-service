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

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqkv.iamservice.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {

  private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public MessagingService(final RabbitTemplate rabbitTemplate, final ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  public void publishTenantCreated(final String tenantKey, final String tenantName) {
    final var event = new TenantEvent(tenantKey, tenantName,
        TenantEvent.EventType.TENANT_CREATED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_CREATED, event);
  }

  public void publishTenantUpdated(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null,
        TenantEvent.EventType.TENANT_UPDATED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_UPDATED, event);
  }

  public void publishUserEvent(final UserEvent event, final String routingKey) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, routingKey, event);
  }

  public void publishNotification(final NotificationEvent event) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_NOTIFICATION_EMAIL, event);
  }

  private void publish(final String exchange, final String routingKey, final Object payload) {
    try {
      rabbitTemplate.convertAndSend(exchange, routingKey, payload);
      log.debug("Published event to exchange={} routingKey={}", exchange, routingKey);
    } catch (final AmqpException e) {
      throw new MessagingException(
          "Failed to publish message to exchange=" + exchange + " routingKey=" + routingKey, e);
    }
  }
}
