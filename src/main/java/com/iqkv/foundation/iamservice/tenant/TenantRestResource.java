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
import java.util.Objects;
import java.util.UUID;

import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.shared.exception.TenantContextMismatchException;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtoMapper;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtos;
import com.iqkv.foundation.iamservice.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/tenants")
@Tag(name = "Tenant Management", description = "Tenant lifecycle and status management")
@SecurityRequirement(name = "bearerAuth")
public class TenantRestResource {

  private final TenantService tenantService;
  private final UserService userService;
  private final MembershipService membershipService;

  public TenantRestResource(final TenantService tenantService,
                            final UserService userService,
                            final MembershipService membershipService) {
    this.tenantService = tenantService;
    this.userService = userService;
    this.membershipService = membershipService;
  }

  @GetMapping("/{tenantKey}")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(summary = "Get tenant by key", description = "Retrieves tenant details by tenantKey")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant found"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.TenantResponse> getTenant(@PathVariable final String tenantKey,
                                                             @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    final Tenant tenant = tenantService.getTenantByKey(tenantKey);
    return ResponseEntity.ok(TenantDtoMapper.toResponse(tenant));
  }

  @PatchMapping("/{tenantKey}")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Rename tenant",
             description = "Updates the tenant name. Requires TENANT_OWNER authority.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.TenantResponse> updateTenant(
      @PathVariable final String tenantKey,
      @Valid @RequestBody final TenantDtos.UpdateTenantRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    final var updated = tenantService.updateTenant(tenantKey, request);
    return ResponseEntity.ok(new TenantDtos.TenantResponse(
        updated.tenantKey(),
        updated.name(),
        updated.status(),
        updated.createdAt()
    ));
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

  @GetMapping("/{tenantKey}/members")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(summary = "List tenant members",
             description = "Returns a paginated, sorted, and optionally filtered list of users belonging to the given tenant.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of members returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.PagedTenantMemberResponse> listTenantMembers(
      @PathVariable final String tenantKey,
      @ModelAttribute @Valid final TenantDtos.TenantMemberListQuery query,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    return ResponseEntity.ok(tenantService.listMembersByTenantKey(tenantKey, query));
  }

  @GetMapping("/{tenantKey}/members/count")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN', 'MEMBER')")
  @Operation(summary = "Count tenant members", description = "Returns the number of active members in the tenant.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Member count returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantDtos.TenantMemberCountResponse> countTenantMembers(
      @PathVariable final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    return ResponseEntity.ok(tenantService.countMembersByTenantKey(tenantKey));
  }

  @PutMapping("/{tenantKey}/members/{userId}/authorities")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Update tenant member authorities", description = "Full replacement of authorities for the tenant member.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Authorities updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Tenant or member not found")
  })
  public ResponseEntity<TenantDtos.MemberAuthoritiesResponse> updateMemberAuthorities(
      @PathVariable final String tenantKey,
      @PathVariable final UUID userId,
      @Valid @RequestBody final TenantDtos.AdminUpdateMemberAuthoritiesRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    membershipService.updateMemberAuthorities(userId, tenantKey, request.authorities());
    var membership = membershipService.resolveMembership(userId, tenantKey);
    var authorities = membershipService.getAuthorities(membership.getId());
    return ResponseEntity.ok(new TenantDtos.MemberAuthoritiesResponse(userId, tenantKey, authorities));
  }

  @DeleteMapping("/{tenantKey}/members/{userId}")
  @PreAuthorize("hasAuthority('TENANT_OWNER')")
  @Operation(summary = "Remove member from tenant", description = "Deletes the membership of the user in this tenant.")
  @Parameter(name = "X-Tenant-ID", in = ParameterIn.HEADER, required = true,
             description = "8-char alphanumeric tenantKey (e.g. xk7f2b9a)")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Member removed"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Membership not found")
  })
  public ResponseEntity<Void> removeMember(
      @PathVariable final String tenantKey,
      @PathVariable final UUID userId,
      @AuthenticationPrincipal final Jwt jwt) {
    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    if (!Objects.equals(tenantKey, tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }
    userService.deleteUser(userId, tenantKey);
    return ResponseEntity.noContent().build();
  }
}
