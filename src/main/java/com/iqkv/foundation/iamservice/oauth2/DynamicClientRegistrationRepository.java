package com.iqkv.foundation.iamservice.oauth2;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.iqkv.foundation.iamservice.infrastructure.config.OAuth2ConfigurationProperties;
import com.iqkv.foundation.iamservice.oauth2.mapper.TenantOidcProviderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
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
      final OAuth2ConfigurationProperties oauth2Properties,
      final TenantOidcProviderMapper tenantOidcProviderMapper,
      final AesGcmEncryptionService encryptionService
  ) {
    this.staticRegistrations = buildStaticRegistrations(oauth2Properties);
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

      // Validate that client_secret is present (required for confidential OAuth2 clients)
      if (tp.getClientSecret() == null || tp.getClientSecret().isBlank()) {
        log.warn("Tenant OIDC provider {} has no client_secret configured, skipping registration", registrationId);
        return null;
      }

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

  private List<ClientRegistration> buildStaticRegistrations(final OAuth2ConfigurationProperties oauth2Properties) {
    if (oauth2Properties == null || oauth2Properties.providers() == null) {
      return List.of();
    }
    return Stream.of(
            googleRegistration(oauth2Properties.providers().google()),
            githubRegistration(oauth2Properties.providers().github()),
            microsoftRegistration(oauth2Properties.providers().microsoft())
        )
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<ClientRegistration> googleRegistration(final OAuth2ConfigurationProperties.Provider provider) {
    if (provider == null || isBlank(provider.clientId())) {
      return Optional.empty();
    }
    return Optional.of(CommonOAuth2Provider.GOOGLE.getBuilder("google")
        .clientId(provider.clientId())
        .clientSecret(provider.clientSecret())
        .scope(splitScopes(provider.scopes()))
        .redirectUri(provider.redirectUri())
        .build());
  }

  private Optional<ClientRegistration> githubRegistration(final OAuth2ConfigurationProperties.Provider provider) {
    if (provider == null || isBlank(provider.clientId())) {
      return Optional.empty();
    }
    return Optional.of(CommonOAuth2Provider.GITHUB.getBuilder("github")
        .clientId(provider.clientId())
        .clientSecret(provider.clientSecret())
        .scope(splitScopes(provider.scopes()))
        .redirectUri(provider.redirectUri())
        .build());
  }

  private Optional<ClientRegistration> microsoftRegistration(
      final OAuth2ConfigurationProperties.MicrosoftProvider provider) {
    if (provider == null || isBlank(provider.clientId()) || isBlank(provider.issuerUri())) {
      return Optional.empty();
    }
    return Optional.of(ClientRegistrations.fromIssuerLocation(provider.issuerUri())
        .registrationId("microsoft")
        .clientId(provider.clientId())
        .clientSecret(provider.clientSecret())
        .scope(splitScopes(provider.scopes()))
        .redirectUri(provider.redirectUri())
        .build());
  }

  private String[] splitScopes(final String scopes) {
    if (isBlank(scopes)) {
      return new String[0];
    }
    return Stream.of(scopes.split(","))
        .map(String::trim)
        .filter(scope -> !scope.isEmpty())
        .toArray(String[]::new);
  }

  private boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
