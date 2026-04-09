package com.iqkv.iamservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "iqkv.notification")
public record NotificationConfigurationProperties(
    @Valid @NotNull Mail mail,
    @NotBlank String defaultLocale,
    @NotBlank String baseUrl
) {

    public record Mail(@NotBlank String from, @NotBlank String fromName, String replyTo) {}
}
