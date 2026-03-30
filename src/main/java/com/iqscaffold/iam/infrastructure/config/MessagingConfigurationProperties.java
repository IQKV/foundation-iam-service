package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqscaffold.messaging.rabbitmq")
public record MessagingConfigurationProperties(boolean enabled) {}
