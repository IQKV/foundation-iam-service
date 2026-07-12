package com.iqkv.foundation.iamservice.oauth2.sso;

import java.time.LocalDateTime;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.AesGcmEncryptionService;
import com.iqkv.foundation.iamservice.oauth2.TenantOidcProvider;
import com.iqkv.foundation.iamservice.oauth2.mapper.TenantOidcProviderMapper;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSsoServiceImpl implements TenantSsoService {

  private final TenantOidcProviderMapper tenantOidcProviderMapper;
  private final TenantMapper tenantMapper;
  private final AesGcmEncryptionService encryptionService;

  public TenantSsoServiceImpl(
      final TenantOidcProviderMapper tenantOidcProviderMapper,
      final TenantMapper tenantMapper,
      final AesGcmEncryptionService encryptionService
  ) {
    this.tenantOidcProviderMapper = tenantOidcProviderMapper;
    this.tenantMapper = tenantMapper;
    this.encryptionService = encryptionService;
  }

  @Override
  @Transactional
  public void configureTenantSso(String tenantKey, TenantOidcProvider provider) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

    // Check if existing configuration
    final var existing = tenantOidcProviderMapper.findByTenantId(tenant.getId());

    if (existing.isPresent()) {
      final var existingProvider = existing.get();
      existingProvider.setDisplayName(provider.getDisplayName());
      existingProvider.setIssuerUri(provider.getIssuerUri());
      existingProvider.setClientId(provider.getClientId());
      if (provider.getClientSecret() != null && !provider.getClientSecret().isBlank()) {
        existingProvider.setClientSecret(encryptionService.encrypt(provider.getClientSecret()));
      }
      existingProvider.setScopes(provider.getScopes());
      existingProvider.setEnabled(provider.isEnabled());
      tenantOidcProviderMapper.update(existingProvider);
    } else {
      // For new provider, client_secret is mandatory
      if (provider.getClientSecret() == null || provider.getClientSecret().isBlank()) {
        throw new IllegalArgumentException("client_secret is required when creating a new SSO provider");
      }
      
      final var newProvider = new TenantOidcProvider();
      newProvider.setId(UUID.randomUUID());
      newProvider.setTenantId(tenant.getId());
      newProvider.setProviderKey("oidc:" + tenantKey);
      newProvider.setDisplayName(provider.getDisplayName());
      newProvider.setIssuerUri(provider.getIssuerUri());
      newProvider.setClientId(provider.getClientId());
      newProvider.setClientSecret(encryptionService.encrypt(provider.getClientSecret()));
      newProvider.setScopes(provider.getScopes());
      newProvider.setEnabled(provider.isEnabled());
      newProvider.setCreatedAt(LocalDateTime.now());
      newProvider.setUpdatedAt(LocalDateTime.now());
      tenantOidcProviderMapper.insert(newProvider);
    }
  }

  @Override
  public TenantOidcProvider getTenantSsoConfiguration(String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    return tenantOidcProviderMapper.findByTenantId(tenant.getId()).orElse(null);
  }

  @Override
  @Transactional
  public void deleteTenantSsoConfiguration(String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    tenantOidcProviderMapper.deleteByTenantId(tenant.getId());
  }
}
