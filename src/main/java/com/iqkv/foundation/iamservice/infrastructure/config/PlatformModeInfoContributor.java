package com.iqkv.foundation.iamservice.infrastructure.config;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Exposes the platform rollout mode in the Spring Boot Actuator info endpoint.
 * This provides a canonical source of truth for the platform's operational mode.
 */
@Component
public class PlatformModeInfoContributor implements InfoContributor {

  private final PlatformModeValidator platformModeValidator;

  public PlatformModeInfoContributor(final PlatformModeValidator platformModeValidator) {
    this.platformModeValidator = platformModeValidator;
  }

  @Override
  public void contribute(final Info.Builder builder) {
    try {
      final RolloutMode mode = platformModeValidator.getMode();
      builder.withDetail("platform", Map.of(
          "rolloutMode", mode.name(),
          "description", getDescription(mode)
      ));
    } catch (final Exception e) {
      builder.withDetail("platform", Map.of(
          "rolloutMode", "UNKNOWN",
          "error", e.getMessage()
      ));
    }
  }

  private String getDescription(final RolloutMode mode) {
    return switch (mode) {
      case MULTI_TENANT -> "Multi-tenant mode: each user signup creates a new tenant";
      case SINGLE_TENANT -> "Single-tenant mode: all users join a pre-provisioned default tenant";
    };
  }
}
