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

import com.iqkv.foundation.iamservice.infrastructure.messaging.EmailSender;
import com.iqkv.foundation.iamservice.infrastructure.messaging.ResendEmailSender;
import com.iqkv.foundation.iamservice.infrastructure.messaging.SmtpEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Registers exactly one {@link EmailSender} bean depending on the value of
 * {@code iqkv.notification.mail.provider}.
 *
 * <ul>
 *   <li>{@code smtp} (default) — {@link SmtpEmailSender} via Spring's {@link JavaMailSender}.</li>
 *   <li>{@code resend} — {@link ResendEmailSender} via the Resend HTTP API; requires
 *       {@code iqkv.notification.mail.resend.api-key} to be set.</li>
 * </ul>
 *
 * <p>Only one bean is active at a time. {@link EmailService} receives whichever
 * implementation is registered — no conditional logic inside the service itself.
 */
@Configuration
public class EmailSenderConfig {

  private static final Logger log = LoggerFactory.getLogger(EmailSenderConfig.class);

  /**
   * SMTP sender — active when {@code iqkv.notification.mail.provider=smtp}
   * or when the property is absent ({@code matchIfMissing=true} keeps backwards compatibility).
   */
  @Bean
  @ConditionalOnProperty(
      name = "iqkv.notification.mail.provider",
      havingValue = "smtp",
      matchIfMissing = true
  )
  public EmailSender smtpEmailSender(final JavaMailSender javaMailSender) {
    log.info("Email provider: SMTP");
    return new SmtpEmailSender(javaMailSender);
  }

  /**
   * Resend sender — active when {@code iqkv.notification.mail.provider=resend}.
   * The API key is read directly from the properties record to keep it out of logs.
   */
  @Bean
  @ConditionalOnProperty(
      name = "iqkv.notification.mail.provider",
      havingValue = "resend"
  )
  public EmailSender resendEmailSender(final NotificationConfigurationProperties notificationProps) {
    final var resendProps = notificationProps.mail().resend();
    if (resendProps == null || resendProps.apiKey() == null || resendProps.apiKey().isBlank()) {
      throw new IllegalStateException(
          "iqkv.notification.mail.resend.api-key must be set when provider=resend");
    }
    log.info("Email provider: Resend");
    return new ResendEmailSender(resendProps.apiKey());
  }
}
