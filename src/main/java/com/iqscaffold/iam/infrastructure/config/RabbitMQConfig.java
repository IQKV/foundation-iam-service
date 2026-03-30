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

package com.iqscaffold.iam.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

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
@ConditionalOnProperty(name = "iqscaffold.messaging.rabbitmq.enabled", havingValue = "true")
@Profile("!test")
public class RabbitMQConfig {

  // Exchange names
  public static final String EVENTS_EXCHANGE = "iqscaffold.events";
  public static final String DLX_EXCHANGE = "iqscaffold.dlx";

  // Queue names
  public static final String USER_EVENTS_QUEUE = "iqscaffold.user.events";
  public static final String NOTIFICATIONS_QUEUE = "iqscaffold.notifications";
  public static final String TENANT_PROVISIONING_QUEUE = "iqscaffold.tenant.provisioning";
  public static final String DLQ = "iqscaffold.dlq";

  // Routing keys
  public static final String ROUTING_TENANT_CREATED = "tenant.created";
  public static final String ROUTING_TENANT_UPDATED = "tenant.updated";
  public static final String ROUTING_TENANT_DELETED = "tenant.deleted";
  public static final String ROUTING_USER_CREATED = "user.created";
  public static final String ROUTING_USER_UPDATED = "user.updated";
  public static final String ROUTING_USER_DELETED = "user.deleted";
  public static final String ROUTING_NOTIFICATION_EMAIL = "notification.email";

  private static final long TTL_24H_MS = 86_400_000L;

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange dlxExchange() {
    return new TopicExchange(DLX_EXCHANGE, true, false);
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
  public Queue tenantProvisioningQueue() {
    return QueueBuilder.durable(TENANT_PROVISIONING_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", TTL_24H_MS)
        .build();
  }

  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  public Binding userEventsBinding() {
    return BindingBuilder.bind(userEventsQueue()).to(eventsExchange()).with("user.#");
  }

  @Bean
  public Binding notificationsBinding() {
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange()).with("notification.*");
  }

  @Bean
  public Binding tenantProvisioningBinding() {
    return BindingBuilder.bind(tenantProvisioningQueue()).to(eventsExchange()).with(ROUTING_TENANT_CREATED);
  }

  @Bean
  public Binding dlqBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("#");
  }
}
