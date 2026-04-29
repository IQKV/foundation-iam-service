package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.notification")
public record NotificationConfigurationProperties(
    @Valid @NotNull Mail mail,
    @NotBlank String defaultLocale,
    @NotBlank String baseUrl
) {

  public record Mail(@NotBlank String from, @NotBlank String fromName, String replyTo) {
  }
}
