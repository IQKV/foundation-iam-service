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

package com.iqkv.foundation.iamservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationConfigurationProperties Tests")
class NotificationConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create NotificationConfigurationProperties")
  void shouldCreateNotificationConfiguration() {
    var mail = new NotificationConfigurationProperties.Mail(
        "noreply@example.com",
        "IAM Service",
        "support@example.com",
        "smtp",
        null
    );
    var config = new NotificationConfigurationProperties(
        mail,
        "en",
        "https://example.com"
    );

    assertThat(config.mail()).isNotNull();
    assertThat(config.mail().from()).isEqualTo("noreply@example.com");
    assertThat(config.mail().fromName()).isEqualTo("IAM Service");
    assertThat(config.mail().replyTo()).isEqualTo("support@example.com");
    assertThat(config.mail().provider()).isEqualTo("smtp");
    assertThat(config.mail().resend()).isNull();
    assertThat(config.defaultLocale()).isEqualTo("en");
    assertThat(config.baseUrl()).isEqualTo("https://example.com");
  }
}
