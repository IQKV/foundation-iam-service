package com.iqkv.iamservice.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "iqkv.auth")
public record AuthConfigurationProperties(
    @Valid @NotNull Jwt jwt,
    @Valid @NotNull Security security,
    @Valid @NotNull PasswordReset passwordReset
) {

    public record Jwt(
        @NotBlank String privateKeyPath,
        @NotBlank String publicKeyPath,
        @NotNull Duration expiry,
        @NotNull Duration refreshExpiry,
        @NotBlank String issuer
    ) {}

    public record Security(
        @Min(4) int passwordEncoderStrength,
        @Min(1) int minLength,
        @Valid @NotNull RateLimiting rateLimiting
    ) {
        public record RateLimiting(
            @Positive int loginAttempts,
            @NotNull Duration lockoutDuration
        ) {}
    }

    public record PasswordReset(
        @NotNull Duration tokenTtl,
        @NotNull Duration rateLimitWindow,
        @Positive int rateLimitMaxRequests
    ) {}
}
