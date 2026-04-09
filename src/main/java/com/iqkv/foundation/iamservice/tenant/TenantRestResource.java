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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqkv.foundation.iamservice.tenant.dto.TenantDtoMapper;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtos;

@RestController
@RequestMapping("/api/v1/iam/tenants")
@Tag(name = "Tenant Management", description = "Tenant lifecycle and status management")
@SecurityRequirement(name = "bearerAuth")
public class TenantRestResource {

  private final TenantService tenantService;

  public TenantRestResource(final TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping("/{tenantKey}")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Get tenant by key", description = "Retrieves tenant details by tenantKey")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant found"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.TenantResponse> getTenant(@PathVariable final String tenantKey) {
    final Tenant tenant = tenantService.getTenantByKey(tenantKey);
    return ResponseEntity.ok(TenantDtoMapper.toResponse(tenant));
  }

  @PatchMapping("/{tenantKey}/status")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Update tenant status", description = "Transitions tenant to a new status")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Status updated"),
      @ApiResponse(responseCode = "400", description = "Invalid status transition"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.TenantResponse> updateTenantStatus(
      @PathVariable final String tenantKey,
      @Valid @RequestBody final TenantDtos.UpdateTenantStatusRequest request) {
    final Tenant tenant = tenantService.updateTenantStatus(tenantKey, request.status());
    return ResponseEntity.ok(TenantDtoMapper.toResponse(tenant));
  }

  @PostMapping("/{tenantKey}/retry-provisioning")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Retry tenant provisioning",
      description = "Retries provisioning for a tenant in PROVISIONING_FAILED state")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
      description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "202", description = "Provisioning retry initiated"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found"),
      @ApiResponse(responseCode = "409", description = "Tenant is not in PROVISIONING_FAILED state")
  })
  public ResponseEntity<TenantDtos.TenantResponse> retryProvisioning(
      @PathVariable final String tenantKey) {
    final Tenant tenant = tenantService.retryProvisioning(tenantKey);
    return ResponseEntity.accepted().body(TenantDtoMapper.toResponse(tenant));
  }
}
