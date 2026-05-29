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

import java.time.Instant;

import com.iqkv.foundation.audit.spi.context.AuditEventEnricher;
import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MessagingService {

  private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

  private final RabbitTemplate rabbitTemplate;
  private final JsonMapper jsonMapper;
  private final IamServiceMetrics metrics;

  public MessagingService(final RabbitTemplate rabbitTemplate, final JsonMapper jsonMapper, final IamServiceMetrics metrics) {
    this.rabbitTemplate = rabbitTemplate;
    this.jsonMapper = jsonMapper;
    this.metrics = metrics;
  }

  public void publishTenantCreated(final String tenantKey, final String tenantName,
                                   final String ownerEmail, final String ownerFirstName) {
    final var event = new TenantEvent(tenantKey, tenantName, ownerEmail, ownerFirstName,
        TenantEvent.EventType.TENANT_CREATED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_CREATED, event);
  }

  public void publishTenantProvisioned(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null, null, null,
        TenantEvent.EventType.TENANT_PROVISIONED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_PROVISIONED, event);
  }

  public void publishTenantProvisioningFailed(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null, null, null,
        TenantEvent.EventType.TENANT_PROVISIONING_FAILED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_PROVISIONING_FAILED, event);
  }

  public void publishTenantSuspended(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null, null, null,
        TenantEvent.EventType.TENANT_SUSPENDED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_SUSPENDED, event);
  }

  public void publishTenantDeleted(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null, null, null,
        TenantEvent.EventType.TENANT_DELETED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_DELETED, event);
  }

  public void publishTenantUpdated(final String tenantKey) {
    final var event = new TenantEvent(tenantKey, null, null, null,
        TenantEvent.EventType.TENANT_UPDATED, Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_TENANT_UPDATED, event);
  }

  public void publishUserEvent(final UserEvent event, final String routingKey) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, routingKey, event);
  }

  public void publishUserInvited(
      final com.iqkv.foundation.iamservice.invitation.TenantInvitation invitation,
      final String tenantName) {
    final var event = new UserEvent(
        null,                          // invitee has no userId yet (may not have an account)
        invitation.getTenantKey(),
        invitation.getInvitedEmail(),
        UserEvent.EventType.USER_INVITED,
        java.time.Instant.now());
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_USER_INVITED, event);
  }

  public void publishNotification(final NotificationEvent event) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_NOTIFICATION_IAM_EMAIL, event);
  }

  public void publishAnnouncementPublish(final AnnouncementPublishEvent event) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_ANNOUNCEMENT_PUBLISH, event);
  }

  public void publishSigninAttempt(final SigninAttemptEvent event) {
    publish(RabbitMQConfig.EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_SIGNIN_ATTEMPT, event);
  }

  private void publish(final String exchange, final String routingKey, final Object event) {
    try {
      AuditEventEnricher.enrich(event);
      rabbitTemplate.convertAndSend(exchange, routingKey, event);
      log.debug("Published event to exchange={} routingKey={}", exchange, routingKey);
      metrics.recordMessagingOutcome(routingKey, "success");
    } catch (final AmqpException e) {
      metrics.recordMessagingOutcome(routingKey, "failure");
      throw new MessagingException(
          "Failed to publish message to exchange=" + exchange + " routingKey=" + routingKey, e);
    }
  }
}
