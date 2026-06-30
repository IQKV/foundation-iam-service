package com.iqkv.foundation.iamservice.oauth2;

import java.util.List;
import java.util.Optional;

import com.iqkv.foundation.iamservice.oauth2.mapper.TenantOidcProviderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

@Component
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

  private static final Logger log = LoggerFactory.getLogger(DynamicClientRegistrationRepository.class);
  private final List<ClientRegistration> staticRegistrations;
  private final TenantOidcProviderMapper tenantOidcProviderMapper;
  private final AesGcmEncryptionService encryptionService;

  public DynamicClientRegistrationRepository(
      final OAuth2ClientProperties clientProperties,
      final TenantOidcProviderMapper tenantOidcProviderMapper,
      final AesGcmEncryptionService encryptionService
  ) {
    this.staticRegistrations = buildStaticRegistrations(clientProperties);
    this.tenantOidcProviderMapper = tenantOidcProviderMapper;
    this.encryptionService = encryptionService;
  }

  @Override
  public ClientRegistration findByRegistrationId(final String registrationId) {
    // First, look for static registrations
    Optional<ClientRegistration> staticReg = staticRegistrations.stream()
        .filter(reg -> reg.getRegistrationId().equals(registrationId))
        .findFirst();
    if (staticReg.isPresent()) {
      return staticReg.get();
    }

    // If not found static, look in tenant_oidc_providers
    Optional<TenantOidcProvider> tenantProvider = tenantOidcProviderMapper.findByProviderKey(registrationId);
    if (tenantProvider.isPresent()) {
      final TenantOidcProvider tp = tenantProvider.get();
      try {
        return ClientRegistrations.fromIssuerLocation(tp.getIssuerUri())
            .registrationId(registrationId)
            .clientId(tp.getClientId())
            .clientSecret(encryptionService.decrypt(tp.getClientSecret()))
            .scope(tp.getScopes().split(","))
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .redirectUri("{baseUrl}/api/v1/iam/auth/oauth2/callback/{registrationId}")
            .build();
      } catch (final Exception e) {
        log.error("Failed to build dynamic client registration for providerKey: {}", registrationId, e);
        return null;
      }
    }

    return null;
  }

  private List<ClientRegistration> buildStaticRegistrations(final OAuth2ClientProperties clientProperties) {
    if (clientProperties == null) {
      return List.of();
    }
    return new OAuth2ClientPropertiesMapper(clientProperties)
        .asClientRegistrations()
        .values()
        .stream()
        .filter(registration -> !isBlank(registration.getClientId()))
        .toList();
  }

  private boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
