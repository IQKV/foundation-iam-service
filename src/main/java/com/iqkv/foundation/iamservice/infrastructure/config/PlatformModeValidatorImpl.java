package com.iqkv.foundation.iamservice.infrastructure.config;

import com.iqkv.foundation.iamservice.shared.exception.InvalidPlatformModeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates the platform rollout mode configuration at startup.
 * Runs with highest precedence to ensure validation completes before any business logic.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformModeValidatorImpl implements PlatformModeValidator, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformModeValidatorImpl.class);

  private final PlatformConfigurationProperties platformConfig;
  private final TenancyConfigurationProperties tenancyConfig;
  private RolloutMode validatedMode;

  public PlatformModeValidatorImpl(
      final PlatformConfigurationProperties platformConfig,
      final TenancyConfigurationProperties tenancyConfig) {
    this.platformConfig = platformConfig;
    this.tenancyConfig = tenancyConfig;
  }

  @Override
  public void run(final ApplicationArguments args) {
    validate();
  }

  @Override
  public void validate() {
    if (platformConfig == null || platformConfig.rolloutMode() == null) {
      final String message = "Platform rollout mode is not configured. "
          + "Please set 'iqkv.platform.rollout-mode' to either 'MULTI_TENANT' or 'SINGLE_TENANT'.";
      log.error("Platform mode validation failed: {}", message);
      throw new InvalidPlatformModeException(message);
    }

    validatedMode = platformConfig.rolloutMode();
    log.info("Platform rollout mode validated successfully: {}", validatedMode);

    // Validate tenancy mode consistency
    tenancyConfig.validateModeConsistency(validatedMode);
    log.info("Tenancy mode consistency validated successfully");
  }

  @Override
  public RolloutMode getMode() {
    if (validatedMode == null) {
      throw new IllegalStateException(
          "Platform mode has not been validated yet. Ensure validate() is called during startup.");
    }
    return validatedMode;
  }
}
