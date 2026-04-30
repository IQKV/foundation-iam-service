package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.regex.Pattern;

import com.iqkv.foundation.iamservice.shared.exception.InvalidPlatformModeException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.tenancy")
public record TenancyConfigurationProperties(
    @NotBlank String schemaPrefix,
    @NotBlank String defaultSchema,
    @NotNull Duration provisioningTimeout,
    String defaultTenantKey,
    // Display name shown in UI — blank would result in unnamed tenants
    @NotBlank String defaultTenantName
) {

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
    // Validate provisioningTimeout is positive
    if (provisioningTimeout != null && (provisioningTimeout.isZero() || provisioningTimeout.isNegative())) {
      throw new InvalidPlatformModeException("provisioningTimeout must be a positive duration, got: " + provisioningTimeout);
    }
  }

}
