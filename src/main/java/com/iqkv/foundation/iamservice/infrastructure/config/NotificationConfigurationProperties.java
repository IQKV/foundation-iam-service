package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Service-level notification settings.
 *
 * <p>Mail transport configuration ({@code iqkv.notification.mail.*}) is no longer bound here —
 * it is owned by {@link com.iqkv.foundation.mail.MailProperties} in the shared
 * {@code foundation-mail} package, which selects the active {@code EmailSender} strategy.
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.notification")
public record NotificationConfigurationProperties(
    // BCP 47 locale tag: e.g. "en", "en-US", "fr"
    @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "defaultLocale must be a valid BCP 47 language tag (e.g. 'en' or 'en-US')") String defaultLocale,
    // Must be a valid http/https URL — used as base for all email links
    @NotBlank @Pattern(regexp = "^https?://.+", message = "baseUrl must be a valid http or https URL") String baseUrl
) {
}
