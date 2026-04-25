package com.iqkv.foundation.iamservice.infrastructure.config;

import java.time.Duration;
import java.util.regex.Pattern;

import com.iqkv.foundation.iamservice.shared.exception.InvalidPlatformModeException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "iqkv.tenancy")
public record TenancyConfigurationProperties(
    @NotBlank String schemaPrefix,
    @NotBlank String defaultSchema,
    @NotNull Duration provisioningTimeout,
    TenancyMode mode,
    String defaultTenantKey,
    String defaultTenantName
) {

  /**
   * Defines the tenancy mode, which must match the platform rollout mode.
   */
  public enum TenancyMode {
    MULTI,
    SINGLE
  }

  private static final Pattern NANOID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");

  /**
   * Validates configuration consistency and format constraints.
   * Called automatically after properties are bound.
   */
  @PostConstruct
  public void validate() {
    // Validate NanoID format if defaultTenantKey is provided
    if (defaultTenantKey != null && !defaultTenantKey.isBlank()) {
      if (!NANOID_PATTERN.matcher(defaultTenantKey).matches()) {
        throw new InvalidPlatformModeException(
            String.format(
                "Invalid defaultTenantKey format: '%s'. Must be exactly 8 characters using alphabet [a-z0-9].",
                defaultTenantKey));
      }
    }
  }

  /**
   * Validates that tenancy mode matches the platform rollout mode.
   *
   * @param rolloutMode the validated platform rollout mode
   * @throws InvalidPlatformModeException if modes are inconsistent
   */
  public void validateModeConsistency(final RolloutMode rolloutMode) {
    if (mode == null) {
      return; // Mode is optional; if not set, no consistency check needed
    }

    final boolean consistent = switch (rolloutMode) {
      case MULTI_TENANT -> mode == TenancyMode.MULTI;
      case SINGLE_TENANT -> mode == TenancyMode.SINGLE;
    };

    if (!consistent) {
      throw new InvalidPlatformModeException(
          String.format(
              "Tenancy mode mismatch: iqkv.tenancy.mode=%s does not match iqkv.platform.rollout-mode=%s. "
                  + "Expected iqkv.tenancy.mode=%s.",
              mode,
              rolloutMode,
              rolloutMode == RolloutMode.MULTI_TENANT ? TenancyMode.MULTI : TenancyMode.SINGLE));
    }
  }
}
