package com.iqkv.foundation.iamservice.oauth2.dto;

import jakarta.validation.constraints.NotBlank;

public final class OidcDtos {

  private OidcDtos() {
  }

  public record EnabledProvidersResponse(
      java.util.List<String> providers
  ) {
  }

  public record OidcExchangeRequest(
      @NotBlank String provider,
      @NotBlank String code,
      @NotBlank String codeVerifier,
      @NotBlank String redirectUri,
      String tenantKey
  ) {
  }

  public record LinkedIdentityResponse(
      String provider,
      String displayName,
      String email,
      String avatarUrl,
      String linkedAt
  ) {
  }

  public record AdminLinkedIdentityResponse(
      java.util.UUID id,
      java.util.UUID userId,
      String provider,
      String providerSub,
      String email,
      String displayName,
      String avatarUrl,
      String linkedAt,
      String lastUsedAt
  ) {
  }
}
