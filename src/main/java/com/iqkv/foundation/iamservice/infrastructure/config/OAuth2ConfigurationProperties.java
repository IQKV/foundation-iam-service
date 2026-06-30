package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.auth.oauth2")
public record OAuth2ConfigurationProperties(
    @NotNull List<String> enabledProviders,
    @NotBlank String baseUrl,
    @NotBlank String postLoginRedirectUri,
    boolean autoProvisionUsers,
    String encryptionKey,
    @NotNull Duration stateTtl,
    @NotNull Duration stateTtlMax,
    @NotNull AutoLink autoLink
) {

  public record AutoLink(
      boolean enabled
  ) {
  }

  @PostConstruct
  public void validate() {
    if (stateTtl.isZero() || stateTtl.isNegative()) {
      throw new IllegalStateException("iqkv.auth.oauth2.state-ttl must be a positive duration, got: " + stateTtl);
    }
    if (stateTtlMax.isZero() || stateTtlMax.isNegative()) {
      throw new IllegalStateException("iqkv.auth.oauth2.state-ttl-max must be a positive duration, got: " + stateTtlMax);
    }
    if (stateTtl.compareTo(stateTtlMax) > 0) {
      throw new IllegalStateException(
          "iqkv.auth.oauth2.state-ttl (" + stateTtl + ") must not be greater than state-ttl-max (" + stateTtlMax + ")");
    }
  }
}
