package com.iqkv.foundation.iamservice.infrastructure.config;

/**
 * Validates the platform rollout mode configuration at startup.
 * Implementations must fail fast if the configuration is invalid or missing.
 */
public interface PlatformModeValidator {

  /**
   * Validates the configured rollout mode and all mode-dependent configuration.
   * This method is called during application startup before any business logic executes.
   *
   * @throws InvalidPlatformModeException if validation fails
   */
  void validate();

  /**
   * Returns the validated rollout mode.
   *
   * @return the active rollout mode
   */
  RolloutMode getMode();
}
