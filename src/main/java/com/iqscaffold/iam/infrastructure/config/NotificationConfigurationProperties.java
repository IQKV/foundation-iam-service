package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.notification")
public record NotificationConfigurationProperties(
    Mail mail,
    String defaultLocale,
    String baseUrl
) {

    public record Mail(String from, String replyTo) {}
}
