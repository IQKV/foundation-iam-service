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

import java.util.Locale;

import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Orchestrates outbound email delivery.
 *
 * <p>This class owns two concerns:
 * <ol>
 *   <li><b>Rendering</b> — resolves the Thymeleaf template path and i18n subject key from
 *       {@link NotificationEventType}, renders the HTML body via {@link TemplateEngine}, and
 *       resolves the localised subject via {@link MessageSource}.</li>
 *   <li><b>Transport delegation</b> — hands the fully-assembled {@link EmailSendRequest} to the
 *       active {@link EmailSender} (SMTP or Resend), which is injected at startup based
 *       on {@code iqkv.notification.mail.provider}.</li>
 * </ol>
 *
 * <p>Switching providers requires only a configuration change — no code changes here.
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final EmailSender emailSender;
  private final TemplateEngine templateEngine;
  private final NotificationConfigurationProperties notificationProps;
  private final MessageSource messageSource;

  public EmailService(final EmailSender emailSender,
                      final TemplateEngine templateEngine,
                      final NotificationConfigurationProperties notificationProps,
                      final MessageSource messageSource) {
    this.emailSender = emailSender;
    this.templateEngine = templateEngine;
    this.notificationProps = notificationProps;
    this.messageSource = messageSource;
  }

  public void send(final NotificationEvent event) {
    final Locale locale = resolveLocale(event.getLocale());
    final String templateName = resolveTemplate(event.getType());
    final String subjectKey = resolveSubjectKey(event.getType());

    final Context ctx = new Context(locale);
    if (event.getPayload() != null) {
      event.getPayload().forEach(ctx::setVariable);
    }

    final String htmlBody = templateEngine.process(templateName, ctx);
    final String subject = messageSource.getMessage(subjectKey, null, locale);

    final EmailSendRequest request = new EmailSendRequest(
        notificationProps.mail().from(),
        notificationProps.mail().fromName(),
        notificationProps.mail().replyTo(),
        event.getRecipientEmail(),
        subject,
        htmlBody
    );

    emailSender.send(request);
    log.info("Email sent: provider={} type={} to={}",
        notificationProps.mail().provider(), event.getType(), event.getRecipientEmail());
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
      case MAGIC_LINK_SENT -> "email/magic-link/sent";
      case PASSWORD_RESET_CONFIRMED -> "email/password-reset/confirmed";
      case INVITATION -> "email/invitation/invite";
      case TENANT_OWNER_WELCOME -> "email/signup/tenant-owner-welcome";
      case INVITATION_ACCEPTED -> "email/invitation/accepted";
      case PASSWORD_CHANGED -> "email/password/changed";
      case MEMBER_WELCOME -> "email/signup/member-welcome";
      case USER_BANNED -> "email/account/banned";
      case USER_UNBANNED -> "email/account/unbanned"; // Reserved, not implemented
    };
  }

  private String resolveSubjectKey(final NotificationEventType type) {
    return switch (type) {
      case VERIFY_EMAIL -> "email.verify-email.subject";
      case EMAIL_VERIFIED -> "email.email-verified.subject";
      case PASSWORD_RESET_INITIATE -> "email.password-reset.initiate.subject";
      case MAGIC_LINK_SENT -> "email.magic-link.sent.subject";
      case PASSWORD_RESET_CONFIRMED -> "email.password-reset.confirmed.subject";
      case INVITATION -> "email.invitation.subject";
      case TENANT_OWNER_WELCOME -> "email.tenant-owner-welcome.subject";
      case INVITATION_ACCEPTED -> "email.invitation-accepted.subject";
      case PASSWORD_CHANGED -> "email.password-changed.subject";
      case MEMBER_WELCOME -> "email.member-welcome.subject";
      case USER_BANNED -> "email.account-banned.subject";
      case USER_UNBANNED -> "email.account-unbanned.subject"; // Reserved, not implemented
    };
  }
}
