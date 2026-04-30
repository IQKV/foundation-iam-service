package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.auth")
public record AuthConfigurationProperties(
    @Valid @NotNull Jwt jwt,
    @Valid @NotNull Security security,
    @Valid @NotNull PasswordReset passwordReset
) {

  @PostConstruct
  public void validate() {
    if (jwt != null) {
      if (jwt.expiry() != null && (jwt.expiry().isZero() || jwt.expiry().isNegative())) {
        throw new IllegalStateException("iqkv.auth.jwt.expiry must be a positive duration, got: " + jwt.expiry());
      }
      if (jwt.refreshExpiry() != null && (jwt.refreshExpiry().isZero() || jwt.refreshExpiry().isNegative())) {
        throw new IllegalStateException("iqkv.auth.jwt.refresh-expiry must be a positive duration, got: " + jwt.refreshExpiry());
      }
      if (jwt.expiry() != null && jwt.refreshExpiry() != null && !jwt.refreshExpiry().isNegative()
          && jwt.refreshExpiry().compareTo(jwt.expiry()) <= 0) {
        throw new IllegalStateException(
            "iqkv.auth.jwt.refresh-expiry (" + jwt.refreshExpiry() + ") must be greater than expiry (" + jwt.expiry() + ")");
      }
    }
    if (security != null && security.rateLimiting() != null) {
      final Duration lockout = security.rateLimiting().lockoutDuration();
      if (lockout != null && (lockout.isZero() || lockout.isNegative())) {
        throw new IllegalStateException("iqkv.auth.security.rate-limiting.lockout-duration must be a positive duration, got: " + lockout);
      }
    }
    if (passwordReset != null) {
      if (passwordReset.tokenTtl() != null && (passwordReset.tokenTtl().isZero() || passwordReset.tokenTtl().isNegative())) {
        throw new IllegalStateException("iqkv.auth.password-reset.token-ttl must be a positive duration, got: " + passwordReset.tokenTtl());
      }
      if (passwordReset.rateLimitWindow() != null && (passwordReset.rateLimitWindow().isZero() || passwordReset.rateLimitWindow().isNegative())) {
        throw new IllegalStateException(
            "iqkv.auth.password-reset.rate-limit-window must be a positive duration, got: " + passwordReset.rateLimitWindow());
      }
    }
  }

  public record Jwt(
      @NotBlank String privateKeyPath,
      @NotBlank String publicKeyPath,
      // Must be positive — PT0S or negative would silently break token issuance
      @NotNull Duration expiry,
      // Must be positive and longer than expiry — enforced by @PostConstruct validate()
      @NotNull Duration refreshExpiry,
      @NotBlank String issuer
  ) {
  }

  public record Security(
      // BCrypt cost factor: 4 is the absolute minimum (test only), 10+ recommended for production
      @Min(4) int passwordEncoderStrength,
      // Minimum password length: 8 is the NIST SP 800-63B baseline
      @Min(8) int minLength,
      @Valid @NotNull RateLimiting rateLimiting
  ) {
    public record RateLimiting(
        @Positive int loginAttempts,
        @NotNull Duration lockoutDuration
    ) {
    }
  }

  public record PasswordReset(
      @NotNull Duration tokenTtl,
      @NotNull Duration rateLimitWindow,
      @Positive int rateLimitMaxRequests
  ) {
  }
}
