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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.notification.UserNotification;
import com.iqkv.foundation.iamservice.notification.UserNotificationService;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Unit Tests")
class NotificationConsumerTest {

  @Mock
  private EmailService emailService;

  @Mock
  private UserNotificationService userNotificationService;

  @Mock
  private UserMapper userMapper;

  @Mock
  private MessageSource messageSource;

  @Mock
  private JsonMapper jsonMapper;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private NotificationConsumer notificationConsumer;

  @Test
  @DisplayName("Should process notification event and persist in-app notification")
  void shouldProcessAndPersistNotification() {
    // Arrange
    final String email = "test@example.com";
    final NotificationEvent event = new NotificationEvent();
    event.setRecipientEmail(email);
    event.setType(NotificationEventType.PASSWORD_CHANGED);
    event.setPayload(Map.of("key", "value"));

    final User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(email);
    user.setLocale("it-IT");

    when(userMapper.findByEmail(email)).thenReturn(Optional.of(user));
    when(messageSource.getMessage(eq("notification.PASSWORD_CHANGED.title"), any(), any(), any()))
        .thenReturn("Localized Title");
    when(messageSource.getMessage(eq("notification.PASSWORD_CHANGED.message"), any(), any(), any()))
        .thenReturn("Localized Message");

    // Act
    notificationConsumer.handleNotification(event);

    // Assert
    verify(emailService).send(event);
    verify(userNotificationService).createNotification(any(UserNotification.class));
  }
}
