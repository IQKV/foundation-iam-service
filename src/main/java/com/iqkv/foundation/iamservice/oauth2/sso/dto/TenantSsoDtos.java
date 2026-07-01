package com.iqkv.foundation.iamservice.oauth2.sso.dto;

public final class TenantSsoDtos {

  private TenantSsoDtos() {
  }

  public record TenantSsoConfigResponse(
      String providerKey,
      String displayName,
      String issuerUri,
      String clientId,
      String scopes,
      boolean enabled,
      boolean hasClientSecret
  ) {
  }

  public record TenantSsoConfigRequest(
      String displayName,
      String issuerUri,
      String clientId,
      String clientSecret,
      String scopes,
      boolean enabled
  ) {
  }
}

