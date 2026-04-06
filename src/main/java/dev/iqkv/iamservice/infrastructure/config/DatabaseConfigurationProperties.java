package dev.iqkv.iamservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "iqkv.database")
public record DatabaseConfigurationProperties(
    @NotBlank String url,
    @NotBlank String username,
    @NotBlank String password,
    @Positive int poolSize
) {}
