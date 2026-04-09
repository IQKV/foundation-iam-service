package com.iqkv.iamservice.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "iqkv.tenancy")
public record TenancyConfigurationProperties(
    @NotBlank String schemaPrefix,
    @NotBlank String defaultSchema,
    @NotNull Duration provisioningTimeout
) {}
