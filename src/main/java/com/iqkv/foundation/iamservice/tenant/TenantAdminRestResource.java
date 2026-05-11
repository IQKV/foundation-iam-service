/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.iamservice.tenant;

import jakarta.validation.Valid;

import com.iqkv.foundation.iamservice.tenant.dto.TenantDtoMapper;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/admin/tenants")
@Tag(name = "Tenant Admin", description = "Platform operator CRUD operations for tenants — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class TenantAdminRestResource {

  private final TenantService tenantService;

  public TenantAdminRestResource(final TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping
  @Operation(summary = "List tenants",
             description = "Returns a paginated, sorted, and optionally filtered list of all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of tenants returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<TenantDtos.PagedTenantResponse> listTenants(
      @ModelAttribute @Valid TenantDtos.TenantListQuery query) {
    return ResponseEntity.ok(tenantService.listTenants(query));
  }

  @GetMapping("/{tenantKey}")
  @Operation(summary = "Get tenant by key")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content)
  })
  public ResponseEntity<TenantDtos.AdminTenantResponse> getTenant(
      @Parameter(description = "8-char tenant key") @PathVariable String tenantKey) {
    final Tenant tenant = tenantService.getTenantByKey(tenantKey);
    return ResponseEntity.ok(TenantDtoMapper.toAdminResponse(tenant));
  }

  @PutMapping("/{tenantKey}")
  @Operation(summary = "Rename tenant",
             description = "Full replacement of the tenant name. All other fields are unchanged.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content),
      @ApiResponse(responseCode = "409", description = "Tenant name already in use", content = @Content)
  })
  public ResponseEntity<TenantDtos.AdminTenantResponse> updateTenant(
      @Parameter(description = "8-char tenant key") @PathVariable String tenantKey,
      @Valid @RequestBody TenantDtos.UpdateTenantRequest request) {
    return ResponseEntity.ok(tenantService.updateTenant(tenantKey, request));
  }

  @PatchMapping("/{tenantKey}")
  @Operation(summary = "Partially update tenant",
             description = "Updates only the provided fields. Omitted fields are left unchanged. "
                           + "Status transitions follow the same allowed-transitions rules as the self-service endpoint.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant patched"),
      @ApiResponse(responseCode = "400", description = "Validation error or invalid status transition", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content),
      @ApiResponse(responseCode = "409", description = "Tenant name already in use", content = @Content)
  })
  public ResponseEntity<TenantDtos.AdminTenantResponse> patchTenant(
      @Parameter(description = "8-char tenant key") @PathVariable String tenantKey,
      @RequestBody TenantDtos.AdminUpdateTenantRequest request) {
    return ResponseEntity.ok(tenantService.patchTenant(tenantKey, request));
  }

  @DeleteMapping("/{tenantKey}")
  @Operation(summary = "Delete tenant",
             description = "Permanently deletes the tenant and all associated data (cascade). "
                           + "Publishes a tenant.deleted domain event.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Tenant deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content)
  })
  public ResponseEntity<Void> deleteTenant(
      @Parameter(description = "8-char tenant key") @PathVariable String tenantKey) {
    tenantService.deleteTenant(tenantKey);
    return ResponseEntity.noContent().build();
  }
}
