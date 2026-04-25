package com.iqkv.foundation.iamservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for platform-wide settings.
 * These settings must be consistent across all core services (IAM, Billing, Gateway).
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.platform")
public record PlatformConfigurationProperties(
    @NotNull RolloutMode rolloutMode
) {

  /**
   * Returns the rollout mode as a string value.
   * Useful for logging and external API responses.
   */
  public String getRolloutModeValue() {
    return rolloutMode.name();
  }
}
