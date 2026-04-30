package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.invitation")
public record InvitationConfigurationProperties(
    // How long an invitation token remains valid. Default: 72 hours.
    // Must be positive — PT0S would expire all invitations immediately upon creation.
    @NotNull Duration tokenTtl,
    // How often the reaper job runs to expire stale invitations. Default: 1 hour.
    // Must be positive — PT0S would cause continuous reaper execution.
    @NotNull Duration reaperInterval
) {

  @PostConstruct
  public void validate() {
    if (tokenTtl != null && (tokenTtl.isZero() || tokenTtl.isNegative())) {
      throw new IllegalStateException("iqkv.invitation.token-ttl must be a positive duration, got: " + tokenTtl);
    }
    if (reaperInterval != null && (reaperInterval.isZero() || reaperInterval.isNegative())) {
      throw new IllegalStateException("iqkv.invitation.reaper-interval must be a positive duration, got: " + reaperInterval);
    }
  }
}
