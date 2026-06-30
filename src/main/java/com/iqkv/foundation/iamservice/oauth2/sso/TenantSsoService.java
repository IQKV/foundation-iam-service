package com.iqkv.foundation.iamservice.oauth2.sso;

import com.iqkv.foundation.iamservice.oauth2.TenantOidcProvider;

public interface TenantSsoService {
  void configureTenantSso(String tenantKey, TenantOidcProvider provider);

  TenantOidcProvider getTenantSsoConfiguration(String tenantKey);

  void deleteTenantSsoConfiguration(String tenantKey);
}
