package dev.iqkv.iamservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "iqkv.liquibase")
public record LiquibaseConfigurationProperties(
    @NotBlank String systemChangeLog,
    @NotBlank String tenantChangeLog,
    String contexts
) {}
