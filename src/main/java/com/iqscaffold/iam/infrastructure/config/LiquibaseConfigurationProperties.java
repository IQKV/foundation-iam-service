package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "iqscaffold.liquibase")
public record LiquibaseConfigurationProperties(
    @NotBlank String systemChangeLog,
    @NotBlank String tenantChangeLog,
    String contexts
) {}
