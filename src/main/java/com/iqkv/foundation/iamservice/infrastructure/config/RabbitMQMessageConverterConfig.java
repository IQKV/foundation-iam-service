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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ message serialization/deserialization using Jackson 2.x.
 *
 * <p>This replaces the default {@code SimpleMessageConverter} (which only supports
 * String, byte[], and Serializable) with {@code Jackson2JsonMessageConverter} to
 * enable automatic JSON serialization of event POJOs.
 *
 * <p>Note: Spring AMQP requires Jackson 2.x ({@code com.fasterxml.jackson.databind.ObjectMapper}),
 * so we create a separate ObjectMapper bean for RabbitMQ with the same configuration
 * as the Jackson 3.x JsonMapper used elsewhere in the application.
 */
@Configuration
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
class RabbitMQMessageConverterConfig {

  /**
   * Configures Jackson 2.x ObjectMapper for RabbitMQ message conversion.
   *
   * <p>Configuration mirrors {@link JacksonJsonMapperConfig} to ensure consistent
   * JSON handling across REST APIs and messaging.
   *
   * @return an ObjectMapper configured for RabbitMQ message serialization
   */
  @Bean
  ObjectMapper rabbitObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    mapper.findAndRegisterModules(); // Auto-discover Jackson modules (Java 8 time, etc.)
    return mapper;
  }

  /**
   * Configures Jackson-based message converter for RabbitMQ.
   *
   * <p>Spring Boot auto-configures {@code RabbitTemplate} and {@code RabbitListenerContainerFactory}
   * to use this converter when present in the application context.
   *
   * @param rabbitObjectMapper the Jackson 2.x ObjectMapper for message serialization
   * @return a message converter that serializes/deserializes messages as JSON
   */
  @Bean
  MessageConverter rabbitMessageConverter(final ObjectMapper rabbitObjectMapper) {
    return new Jackson2JsonMessageConverter(rabbitObjectMapper);
  }
}
