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

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link EmailSender} that delivers email via the Resend HTTP API.
 *
 * <p>Activated when {@code iqkv.notification.mail.provider=resend}.
 * Requires {@code iqkv.notification.mail.resend.api-key} to be set.
 *
 * <p>The sending domain must be verified in the Resend dashboard.
 * Resend expects the sender in {@code "Name <address>"} format, which is
 * assembled here from {@link EmailSendRequest#fromName()} and {@link EmailSendRequest#from()}.
 */
public class ResendEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

  private final Resend resend;

  public ResendEmailSender(final String apiKey) {
    this.resend = new Resend(apiKey);
  }

  @Override
  public void send(final EmailSendRequest request) {
    final String formattedFrom = buildFromAddress(request.fromName(), request.from());

    final CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
        .from(formattedFrom)
        .to(request.to())
        .subject(request.subject())
        .html(request.htmlBody());

    if (request.replyTo() != null && !request.replyTo().isBlank()) {
      builder.replyTo(request.replyTo());
    }

    try {
      final var response = resend.emails().send(builder.build());
      log.debug("Resend delivery succeeded: id={} to={}", response.getId(), request.to());
    } catch (final ResendException e) {
      throw new MessagingException("Resend delivery failed for recipient " + request.to(), e);
    }
  }

  /**
   * Builds the RFC 5322 {@code "Display Name <address>"} sender string expected by Resend.
   * Falls back to the bare address when {@code fromName} is blank.
   */
  private static String buildFromAddress(final String fromName, final String from) {
    if (fromName != null && !fromName.isBlank()) {
      return fromName + " <" + from + ">";
    }
    return from;
  }
}
