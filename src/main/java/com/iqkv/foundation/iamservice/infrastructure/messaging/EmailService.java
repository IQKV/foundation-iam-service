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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender javaMailSender;
  private final TemplateEngine templateEngine;
  private final NotificationConfigurationProperties notificationProps;
  private final MessageSource messageSource;

  public EmailService(final JavaMailSender javaMailSender,
                      final TemplateEngine templateEngine,
                      final NotificationConfigurationProperties notificationProps,
                      final MessageSource messageSource) {
    this.javaMailSender = javaMailSender;
    this.templateEngine = templateEngine;
    this.notificationProps = notificationProps;
    this.messageSource = messageSource;
  }

  public void send(final NotificationEvent event) {
    final Locale locale = resolveLocale(event.getLocale());
    final String templateName = resolveTemplate(event.getType());
    final String subjectKey = resolveSubjectKey(event.getType());

    try {
      final Context ctx = new Context(locale);
      if (event.getPayload() != null) {
        event.getPayload().forEach(ctx::setVariable);
      }

      final String htmlBody = templateEngine.process(templateName, ctx);
      final String subject = messageSource.getMessage(subjectKey, null, locale);

      final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
      final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(notificationProps.mail().from(), notificationProps.mail().fromName());
      helper.setTo(event.getRecipientEmail());
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      final String replyTo = notificationProps.mail().replyTo();
      if (replyTo != null && !replyTo.isBlank()) {
        helper.setReplyTo(replyTo);
      }

      javaMailSender.send(mimeMessage);
      log.info("Email sent: type={} to={}", event.getType(), event.getRecipientEmail());
    } catch (final MessagingException | java.io.UnsupportedEncodingException e) {
      log.error("Failed to send email: type={} to={}", event.getType(), event.getRecipientEmail(), e);
      throw new com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingException(
          "Failed to send email to " + event.getRecipientEmail(), e);
    }
  }

  private Locale resolveLocale(final String localeTag) {
    if (localeTag != null && !localeTag.isBlank()) {
      return Locale.forLanguageTag(localeTag);
    }
    final String defaultLocale = notificationProps.defaultLocale();
    return defaultLocale != null ? Locale.forLanguageTag(defaultLocale) : Locale.ENGLISH;
  }

  private String resolveTemplate(final NotificationEventType type) {
    return switch (type) {
      case VERIFY_EMAIL -> "email/signup/verify-email";
      case EMAIL_VERIFIED -> "email/signup/email-verified";
      case PASSWORD_RESET_INITIATE -> "email/password-reset/initiate";
      case PASSWORD_RESET_CONFIRMED -> "email/password-reset/confirmed";
      case INVITATION -> "email/invitation/invite";
      case TENANT_OWNER_WELCOME -> "email/signup/tenant-owner-welcome";
      case INVITATION_ACCEPTED -> "email/invitation/accepted";
      case PASSWORD_CHANGED -> "email/password/changed";
      case MEMBER_WELCOME -> "email/signup/member-welcome";
    };
  }

  private String resolveSubjectKey(final NotificationEventType type) {
    return switch (type) {
      case VERIFY_EMAIL -> "email.verify-email.subject";
      case EMAIL_VERIFIED -> "email.email-verified.subject";
      case PASSWORD_RESET_INITIATE -> "email.password-reset.initiate.subject";
      case PASSWORD_RESET_CONFIRMED -> "email.password-reset.confirmed.subject";
      case INVITATION -> "email.invitation.subject";
      case TENANT_OWNER_WELCOME -> "email.tenant-owner-welcome.subject";
      case INVITATION_ACCEPTED -> "email.invitation-accepted.subject";
      case PASSWORD_CHANGED -> "email.password-changed.subject";
      case MEMBER_WELCOME -> "email.member-welcome.subject";
    };
  }
}
