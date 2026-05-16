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

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configures RabbitMQ message serialization/deserialization using Jackson.
 *
 * <p>This replaces the default {@code SimpleMessageConverter} (which only supports
 * String, byte[], and Serializable) with {@code Jackson2JsonMessageConverter} to
 * enable automatic JSON serialization of event POJOs.
 *
 * <p>The converter uses the same {@link JsonMapper} bean configured in
 * {@link JacksonJsonMapperConfig}, ensuring consistent JSON handling across
 * REST APIs, messaging, and internal serialization.
 */
@Configuration
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
class RabbitMQMessageConverterConfig {

  /**
   * Configures Jackson-based message converter for RabbitMQ.
   *
   * <p>Spring Boot auto-configures {@code RabbitTemplate} and {@code RabbitListenerContainerFactory}
   * to use this converter when present in the application context.
   *
   * @param jsonMapper the Jackson JSON mapper configured with application-wide settings
   * @return a message converter that serializes/deserializes messages as JSON
   */
  @Bean
  MessageConverter rabbitMessageConverter(final JsonMapper jsonMapper) {
    return new Jackson2JsonMessageConverter(jsonMapper);
  }
}
