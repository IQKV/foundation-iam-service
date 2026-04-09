package com.iqkv.iamservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.messaging.rabbitmq")
public record MessagingConfigurationProperties(boolean enabled) {}
