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

package com.iqkv.foundation.iamservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
@Profile("!test")
public class RabbitMQConfig {

  // -------------------------------------------------------------------------
  // Shared exchange (all services publish here)
  // -------------------------------------------------------------------------
  public static final String EVENTS_EXCHANGE = "iqkv.events";
  public static final String DLX_EXCHANGE = "iqkv.dlx";
  public static final String DLQ = "iqkv.dlq";

  // -------------------------------------------------------------------------
  // IAM queue names
  // -------------------------------------------------------------------------
  public static final String USER_EVENTS_QUEUE = "iqkv.iam.user.events";
  public static final String NOTIFICATIONS_QUEUE = "iqkv.iam.notifications";
  public static final String ANNOUNCEMENTS_QUEUE = "iqkv.iam.announcements";
  public static final String TENANT_PROVISIONING_QUEUE = "iqkv.iam.tenant.provisioning";
  public static final String SUBSCRIPTION_EVENTS_QUEUE = "iqkv.iam.subscription.events";
  public static final String AUTH_EVENTS_QUEUE = "iqkv.iam.auth.events";

  // -------------------------------------------------------------------------
  // Routing keys — domain events (shared across services)
  // -------------------------------------------------------------------------
  public static final String ROUTING_TENANT_CREATED = "tenant.created";
  public static final String ROUTING_TENANT_PROVISIONED = "tenant.provisioned";
  public static final String ROUTING_TENANT_PROVISIONING_FAILED = "tenant.provisioning_failed";
  public static final String ROUTING_TENANT_UPDATED = "tenant.updated";
  public static final String ROUTING_TENANT_DELETED = "tenant.deleted";
  public static final String ROUTING_TENANT_SUSPENDED = "tenant.suspended";
  public static final String ROUTING_USER_CREATED = "user.created";
  public static final String ROUTING_USER_UPDATED = "user.updated";
  public static final String ROUTING_USER_DELETED = "user.deleted";
  public static final String ROUTING_USER_REMOVED = "user.removed";
  public static final String ROUTING_USER_INVITED = "user.invited";
  public static final String ROUTING_SUBSCRIPTION_CANCELLED = "subscription.cancelled";
  public static final String ROUTING_SUBSCRIPTION_CREATED = "subscription.created";
  public static final String ROUTING_SUBSCRIPTION_UPDATED = "subscription.updated";
  public static final String ROUTING_SIGNIN_ATTEMPT = "auth.signin.attempt";

  // -------------------------------------------------------------------------
  // Routing keys — password mutations
  // -------------------------------------------------------------------------
  public static final String ROUTING_PASSWORD_RESET_INITIATED = "auth.password.reset.initiated";
  public static final String ROUTING_PASSWORD_RESET_COMPLETED = "auth.password.reset.completed";
  public static final String ROUTING_PASSWORD_CHANGED = "auth.password.changed";

  // -------------------------------------------------------------------------
  // Routing keys — magic link events
  // -------------------------------------------------------------------------
  public static final String ROUTING_MAGIC_LINK_INITIATED = "auth.magic_link.initiated";
  public static final String ROUTING_MAGIC_LINK_EXCHANGED = "auth.magic_link.exchanged";

  // -------------------------------------------------------------------------
  // Routing keys — user admin mutations (ban, unban, unlock, status change)
  // user.# wildcard in USER_EVENTS_QUEUE already captures these
  // -------------------------------------------------------------------------
  public static final String ROUTING_USER_BANNED = "user.banned";
  public static final String ROUTING_USER_UNBANNED = "user.unbanned";
  public static final String ROUTING_USER_UNLOCKED = "user.unlocked";
  public static final String ROUTING_USER_STATUS_CHANGED = "user.status_changed";

  // -------------------------------------------------------------------------
  // Routing keys — IAM notification emails (scoped to avoid conflicts)
  // -------------------------------------------------------------------------
  public static final String ROUTING_NOTIFICATION_IAM_EMAIL = "notification.iam.email";
  public static final String ROUTING_ANNOUNCEMENT_PUBLISH = "announcement.publish";

  private static final long TTL_24H_MS = 86_400_000L;

  // -------------------------------------------------------------------------
  // Beans
  // -------------------------------------------------------------------------

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange dlxExchange() {
    return new TopicExchange(DLX_EXCHANGE, true, false);
  }

  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  public Queue userEventsQueue() {
    return QueueBuilder.durable(USER_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue notificationsQueue() {
    return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue announcementsQueue() {
    return QueueBuilder.durable(ANNOUNCEMENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Binding announcementsBinding(final Queue announcementsQueue, final TopicExchange eventsExchange) {
    return BindingBuilder.bind(announcementsQueue).to(eventsExchange).with(ROUTING_ANNOUNCEMENT_PUBLISH);
  }

  @Bean
  public Queue tenantProvisioningQueue() {
    return QueueBuilder.durable(TENANT_PROVISIONING_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue subscriptionEventsQueue() {
    return QueueBuilder.durable(SUBSCRIPTION_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue authEventsQueue() {
    return QueueBuilder.durable(AUTH_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Binding userEventsBinding() {
    return BindingBuilder.bind(userEventsQueue()).to(eventsExchange()).with("user.#");
  }

  @Bean
  public Binding authEventsBinding() {
    // Captures signin attempts + all password mutation events
    return BindingBuilder.bind(authEventsQueue()).to(eventsExchange()).with("auth.#");
  }

  @Bean
  public Binding notificationsBinding() {
    // Exact match — only IAM notification emails land here
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange())
        .with(ROUTING_NOTIFICATION_IAM_EMAIL);
  }

  @Bean
  public Binding tenantProvisioningBinding() {
    return BindingBuilder.bind(tenantProvisioningQueue()).to(eventsExchange())
        .with(ROUTING_TENANT_CREATED);
  }

  @Bean
  public Binding subscriptionEventsBinding() {
    // Wildcard — subscription.created, subscription.updated, subscription.cancelled all route here
    return BindingBuilder.bind(subscriptionEventsQueue()).to(eventsExchange())
        .with("subscription.#");
  }

  @Bean
  public Binding dlqBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("#");
  }
}
