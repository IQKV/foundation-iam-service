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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * {@link EmailSender} that delivers email via SMTP using Spring's {@link JavaMailSender}.
 *
 * <p>Activated when {@code iqkv.notification.mail.provider=smtp} (the default).
 * Requires {@code spring.mail.*} properties to be configured.
 */
public class SmtpEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

  private final JavaMailSender javaMailSender;

  public SmtpEmailSender(final JavaMailSender javaMailSender) {
    this.javaMailSender = javaMailSender;
  }

  @Override
  public void send(final EmailSendRequest request) {
    try {
      final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
      final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(request.from(), request.fromName());
      helper.setTo(request.to());
      helper.setSubject(request.subject());
      helper.setText(request.htmlBody(), true);

      if (request.replyTo() != null && !request.replyTo().isBlank()) {
        helper.setReplyTo(request.replyTo());
      }

      javaMailSender.send(mimeMessage);
      log.debug("SMTP delivery succeeded: to={}", request.to());
    } catch (final jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
      throw new MessagingException("SMTP delivery failed for recipient " + request.to(), e);
    }
  }
}
