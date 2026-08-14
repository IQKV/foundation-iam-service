package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.notification")
public record NotificationConfigurationProperties(
    @Valid @NotNull Mail mail,
    // BCP 47 locale tag: e.g. "en", "en-US", "fr"
    @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "defaultLocale must be a valid BCP 47 language tag (e.g. 'en' or 'en-US')") String defaultLocale,
    // Must be a valid http/https URL — used as base for all email links
    @NotBlank @Pattern(regexp = "^https?://.+", message = "baseUrl must be a valid http or https URL") String baseUrl
) {

  /**
   * SMTP sender identity and optional reply-to.
   */
  public record Mail(
      @NotBlank String from,
      @NotBlank String fromName,
      String replyTo,
      // Active email transport: "smtp" (default) or "resend"
      @NotBlank @Pattern(regexp = "^(smtp|resend)$", message = "provider must be 'smtp' or 'resend'") String provider,
      @Valid Resend resend
  ) {

    /**
     * Resend-specific configuration, required when {@code provider=resend}.
     */
    public record Resend(String apiKey) {
    }
  }
}
