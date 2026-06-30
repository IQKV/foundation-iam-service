package com.iqkv.foundation.iamservice.oauth2.sso;

import jakarta.validation.Valid;

import com.iqkv.foundation.iamservice.oauth2.TenantOidcProvider;
import com.iqkv.foundation.iamservice.tenancy.TenantContext;
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
  public ResponseEntity<TenantOidcProvider> getSsoConfig(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(tenantSsoService.getTenantSsoConfiguration(TenantContext.getCurrentTenant()));
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
      @Valid @RequestBody TenantOidcProvider provider
  ) {
    tenantSsoService.configureTenantSso(TenantContext.getCurrentTenant(), provider);
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
