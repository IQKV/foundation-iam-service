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

package com.iqkv.foundation.iamservice.tenant.dto;

import java.util.Collections;
import java.util.List;

import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.user.UserWithOrganizations;

public final class TenantDtoMapper {

  private TenantDtoMapper() {
  }

  /**
   * Maps a {@link Tenant} to the slim self-service {@link TenantDtos.TenantResponse}.
   * Used by {@code TenantRestResource} (TENANT_OWNER surface).
   */
  public static TenantDtos.TenantResponse toResponse(final Tenant tenant) {
    return new TenantDtos.TenantResponse(
        tenant.getTenantKey(),
        tenant.getName(),
        tenant.getStatus(),
        tenant.getCreatedAt());
  }

  /**
   * Maps a {@link Tenant} to the rich {@link TenantDtos.AdminTenantResponse}.
   * Used by {@code TenantAdminRestResource} (PLATFORM_ADMIN surface).
   */
  public static TenantDtos.AdminTenantResponse toAdminResponse(final Tenant tenant) {
    return new TenantDtos.AdminTenantResponse(
        tenant.getId(),
        tenant.getTenantKey(),
        tenant.getName(),
        tenant.getStatus(),
        tenant.getIsDefault(),
        tenant.getTenantModeOrigin(),
        tenant.getCreatedBy(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt());
  }

  /**
   * Maps a {@link UserWithOrganizations} projection to a {@link TenantDtos.TenantMemberResponse}.
   *
   * <p>Used by the tenant-scoped member list query ({@code findMembersByTenantKeyScoped}).
   * The projection's {@code membershipAuthorities} field carries only the authorities
   * for the queried tenant — it is surfaced here as {@code tenantAuthorities} to make
   * the scope explicit and avoid confusion with the cross-tenant {@code membershipAuthorities}
   * field on {@link com.iqkv.foundation.iamservice.user.dto.UserDtos.UserResponse}.
   */
  public static TenantDtos.TenantMemberResponse toTenantMemberResponse(final UserWithOrganizations projection) {
    final List<String> tenantAuthorities = projection.getMembershipAuthorities() != null
        ? projection.getMembershipAuthorities() : Collections.emptyList();
    final List<String> organizations = projection.getOrganizations() != null
        ? projection.getOrganizations() : Collections.emptyList();
    return new TenantDtos.TenantMemberResponse(
        projection.getId(),
        projection.getEmail(),
        projection.getFirstName(),
        projection.getLastName(),
        projection.getStatus() != null ? projection.getStatus().name() : null,
        projection.isEmailVerified(),
        tenantAuthorities,
        organizations,
        projection.getCreatedAt(),
        projection.getUpdatedAt());
  }
}
