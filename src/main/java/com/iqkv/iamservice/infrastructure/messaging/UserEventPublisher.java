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

import com.iqkv.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.iamservice.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

  private final MessagingService messagingService;

  public UserEventPublisher(final MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  public void publishUserCreated(final User user) {
    final var event = new UserEvent(user.getId(), null, user.getEmail(),
        UserEvent.EventType.USER_CREATED, Instant.now());
    messagingService.publishUserEvent(event, RabbitMQConfig.ROUTING_USER_CREATED);
  }

  public void publishUserDeleted(final User user) {
    final var event = new UserEvent(user.getId(), null, user.getEmail(),
        UserEvent.EventType.USER_DELETED, Instant.now());
    messagingService.publishUserEvent(event, RabbitMQConfig.ROUTING_USER_DELETED);
  }
}
