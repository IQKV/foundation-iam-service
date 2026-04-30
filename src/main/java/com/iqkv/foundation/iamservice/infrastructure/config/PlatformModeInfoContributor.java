package com.iqkv.foundation.iamservice.infrastructure.config;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Exposes the platform rollout mode in the Spring Boot Actuator info endpoint.
 * This provides a canonical source of truth for the platform's operational mode.
 *
 * <p>The {@code /actuator/info} response includes a {@code platform} block:
 * <pre>{@code
 * {
 *   "platform": {
 *     "rollout-mode": "MULTI_TENANT"
 *   }
 * }
 * }</pre>
 *
 * <p>Consumers (e.g. Gateway, Billing) should navigate this structure using
 * {@link #INFO_PLATFORM_KEY} and {@link #INFO_ROLLOUT_MODE_KEY}:
 * <pre>{@code
 * // JSON path within /actuator/info that carries the rollout mode:
 * // platform.rollout-mode
 * String mode = response.get(INFO_PLATFORM_KEY).get(INFO_ROLLOUT_MODE_KEY);
 * }</pre>
 */
@Component
public class PlatformModeInfoContributor implements InfoContributor {

  /**
   * Top-level key in the {@code /actuator/info} response that groups platform metadata.
   * JSON path: {@code platform}
   */
  public static final String INFO_PLATFORM_KEY = "platform";

  /**
   * JSON path within the IAM {@code /actuator/info} response that carries the rollout mode.
   * Expected structure: {@code { "platform": { "rollout-mode": "MULTI_TENANT" } }}
   * <p>Full JSON path: {@code platform.rollout-mode}
   */
  public static final String INFO_ROLLOUT_MODE_KEY = "rollout-mode";

  private final PlatformModeValidator platformModeValidator;

  public PlatformModeInfoContributor(final PlatformModeValidator platformModeValidator) {
    this.platformModeValidator = platformModeValidator;
  }

  @Override
  public void contribute(final Info.Builder builder) {
    try {
      final RolloutMode mode = platformModeValidator.getMode();
      builder.withDetail(INFO_PLATFORM_KEY, Map.of(
          INFO_ROLLOUT_MODE_KEY, mode.name(),
          "description", getDescription(mode)
      ));
    } catch (final Exception e) {
      builder.withDetail(INFO_PLATFORM_KEY, Map.of(
          INFO_ROLLOUT_MODE_KEY, "UNKNOWN",
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
