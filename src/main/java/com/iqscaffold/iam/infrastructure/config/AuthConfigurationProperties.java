package com.iqscaffold.iam.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.auth")
public record AuthConfigurationProperties(
    Jwt jwt,
    Security security,
    PasswordReset passwordReset
) {

    public record Jwt(
        String privateKeyPath,
        String publicKeyPath,
        Duration expiry,
        Duration refreshExpiry,
        String issuer
    ) {}

    public record Security(
        int passwordEncoderStrength,
        int minLength,
        RateLimiting rateLimiting
    ) {
        public record RateLimiting(
            int loginAttempts,
            Duration lockoutDuration
        ) {}
    }

    public record PasswordReset(
        Duration tokenTtl,
        Duration rateLimitWindow,
        int rateLimitMaxRequests
    ) {}
}
