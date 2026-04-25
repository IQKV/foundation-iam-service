package com.iqkv.foundation.iamservice.infrastructure.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Exposes the platform rollout mode in the Spring Boot Actuator health endpoint.
 * This allows other services (Gateway, Billing) to verify mode consistency.
 */
@Component
public class PlatformModeHealthIndicator implements HealthIndicator {

  private final PlatformModeValidator platformModeValidator;

  public PlatformModeHealthIndicator(final PlatformModeValidator platformModeValidator) {
    this.platformModeValidator = platformModeValidator;
  }

  @Override
  public Health health() {
    try {
      final RolloutMode mode = platformModeValidator.getMode();
      return Health.up()
          .withDetail("rolloutMode", mode.name())
          .withDetail("description", getDescription(mode))
          .build();
    } catch (final Exception e) {
      return Health.down()
          .withDetail("error", "Platform mode validation failed")
          .withDetail("message", e.getMessage())
          .build();
    }
  }

  private String getDescription(final RolloutMode mode) {
    return switch (mode) {
      case MULTI_TENANT -> "Multi-tenant mode: each user signup creates a new tenant";
      case SINGLE_TENANT -> "Single-tenant mode: all users join a pre-provisioned default tenant";
    };
  }
}
