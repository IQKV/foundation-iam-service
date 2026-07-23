package com.iqkv.foundation.iamservice.oauth2.sso;

import jakarta.validation.Valid;

import com.iqkv.foundation.iamservice.oauth2.TenantOidcProvider;
import com.iqkv.foundation.iamservice.oauth2.sso.dto.TenantSsoDtos;
import com.iqkv.foundation.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/tenants/sso")
@Tag(name = "Tenant SSO Configuration", description = "Configure custom OIDC providers per tenant")
public class TenantSsoRestResource {

  private final TenantSsoService tenantSsoService;

  public TenantSsoRestResource(final TenantSsoService tenantSsoService) {
    this.tenantSsoService = tenantSsoService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get current tenant SSO configuration")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Configuration returned"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  public ResponseEntity<TenantSsoDtos.TenantSsoConfigResponse> getSsoConfig(@AuthenticationPrincipal Jwt jwt) {
    final TenantOidcProvider provider =
        tenantSsoService.getTenantSsoConfiguration(TenantContext.getCurrentTenant());
    if (provider == null) {
      return ResponseEntity.ok(null);
    }
    return ResponseEntity.ok(new TenantSsoDtos.TenantSsoConfigResponse(
        provider.getProviderKey(),
        provider.getDisplayName(),
        provider.getIssuerUri(),
        provider.getClientId(),
        provider.getScopes(),
        provider.isEnabled(),
        provider.getClientSecret() != null && !provider.getClientSecret().isBlank()
    ));
  }

  @PutMapping
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update tenant SSO configuration")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Configuration updated"),
      @ApiResponse(responseCode = "400", description = "Invalid configuration"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  public ResponseEntity<Void> updateSsoConfig(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody TenantSsoDtos.TenantSsoConfigRequest provider
  ) {
    final TenantOidcProvider model = new TenantOidcProvider();
    model.setDisplayName(provider.displayName());
    model.setIssuerUri(provider.issuerUri());
    model.setClientId(provider.clientId());
    model.setClientSecret(provider.clientSecret());
    // Use default scopes if not provided
    model.setScopes(provider.scopes() != null && !provider.scopes().isBlank()
        ? provider.scopes()
        : "openid,profile,email");
    model.setEnabled(provider.enabled());
    tenantSsoService.configureTenantSso(TenantContext.getCurrentTenant(), model);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Delete tenant SSO configuration")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Configuration deleted"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  public ResponseEntity<Void> deleteSsoConfig(@AuthenticationPrincipal Jwt jwt) {
    tenantSsoService.deleteTenantSsoConfiguration(TenantContext.getCurrentTenant());
    return ResponseEntity.noContent().build();
  }
}
