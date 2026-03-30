package com.iqscaffold.iam.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.tenancy")
public record TenancyConfigurationProperties(
    String schemaPrefix,
    String defaultSchema,
    Duration provisioningTimeout
) {}
