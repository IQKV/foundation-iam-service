package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.liquibase")
public record LiquibaseConfigurationProperties(
    String systemChangeLog,
    String tenantChangeLog,
    String contexts
) {}
