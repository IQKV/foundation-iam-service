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

import java.util.UUID;

import com.iqkv.foundation.iamservice.tenant.dto.TenantDtos;

/**
 * Domain service for tenant lifecycle management.
 *
 * <p>Covers two distinct surfaces:
 * <ul>
 *   <li><b>Self-service</b> — operations initiated by a {@code TENANT_OWNER}
 *       (e.g. {@link #getTenantByKey}, {@link #updateTenantStatus}).</li>
 *   <li><b>Platform admin</b> — privileged operations performed by operators with
 *       {@code PLATFORM_ADMIN} authority (e.g. {@link #listTenants},
 *       {@link #adminCreateTenant}, {@link #patchTenant}, {@link #deleteTenant}).</li>
 * </ul>
 */
public interface TenantService {

  // ─── Self-service ──────────────────────────────────────────────────────────

  /**
   * Returns the provisioning status of a tenant by its key.
   * Intended for the post-signup polling flow — no authentication required at the call site.
   *
   * @param tenantKey the tenant's unique key
   * @return the current {@link TenantStatus} as a string
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException if no tenant exists with the given key
   */
  String getProvisioningStatus(String tenantKey);

  /**
   * Creates a new tenant owned by the given user.
   * Publishes a {@code tenant.created} domain event.
   *
   * @param tenantName  human-readable name (must be unique)
   * @param ownerUserId UUID of the user who will own the tenant
   * @return the newly created {@link Tenant}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException if a tenant with the same name already exists
   */
  Tenant createTenant(String tenantName, UUID ownerUserId);

  /**
   * Retrieves a tenant by its 8-character NanoID key.
   *
   * @param tenantKey the tenant's unique key
   * @return the {@link Tenant}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException if no tenant exists with the given key
   */
  Tenant getTenantByKey(String tenantKey);

  /**
   * Transitions a tenant to a new status.
   * Only allowed transitions are accepted (see {@code TenantServiceImpl.ALLOWED_TRANSITIONS}).
   *
   * @param tenantKey the tenant's unique key
   * @param newStatus the target status
   * @return the updated {@link Tenant}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException     if no tenant exists with the given key
   * @throws com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException if the transition is not allowed
   */
  Tenant updateTenantStatus(String tenantKey, TenantStatus newStatus);

  /**
   * Retries provisioning for a tenant in {@code PROVISIONING_FAILED} state.
   *
   * @param tenantKey the tenant's unique key
   * @return the updated {@link Tenant}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException     if no tenant exists with the given key
   * @throws com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException if the tenant is not in {@code PROVISIONING_FAILED} state
   */
  Tenant retryProvisioning(String tenantKey);

  // ─── Platform admin ────────────────────────────────────────────────────────

  /**
   * Returns a paginated, sorted, and optionally filtered list of users
   * who are members of the given tenant.
   *
   * <p>Each row carries {@code tenantAuthorities} scoped to this tenant only,
   * plus {@code organizations} listing all tenants the user belongs to.
   *
   * @param tenantKey the tenant's unique key
   * @param query     encapsulates pagination, sort, and optional filters
   * @return a {@link TenantDtos.PagedTenantMemberResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException if no tenant exists with the given key
   */
  TenantDtos.PagedTenantMemberResponse listMembersByTenantKey(
      String tenantKey, TenantDtos.TenantMemberListQuery query);

  /**
   * Returns the number of members (tenant_memberships) for the given tenant.
   *
   * @param tenantKey the tenant's unique key
   * @return member count response
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException if no tenant exists with the given key
   */
  TenantDtos.TenantMemberCountResponse countMembersByTenantKey(String tenantKey);

  /**
   * Returns the total number of tenants across all statuses.
   *
   * @return total tenant count
   */
  TenantDtos.TenantCountResponse countTenants();

  /**
   * Returns a paginated, sorted, and optionally filtered list of tenants.
   *
   * @param query encapsulates pagination, sort, and optional filters
   * @return a {@link TenantDtos.PagedTenantResponse}
   */
  TenantDtos.PagedTenantResponse listTenants(TenantDtos.TenantListQuery query);

  /**
   * Fully replaces a tenant's name (PUT semantics).
   *
   * @param tenantKey the tenant's unique key
   * @param request   payload containing the new name
   * @return the updated tenant as a {@link TenantDtos.AdminTenantResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException      if no tenant exists with the given key
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException if the new name is already taken by another tenant
   */
  TenantDtos.AdminTenantResponse updateTenant(String tenantKey, TenantDtos.UpdateTenantRequest request);

  /**
   * Partially updates a tenant (PATCH semantics).
   * Only non-null fields in the request are applied.
   *
   * @param tenantKey the tenant's unique key
   * @param request   partial update payload; any field may be {@code null}
   * @return the updated tenant as a {@link TenantDtos.AdminTenantResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException     if no tenant exists with the given key
   * @throws com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException if the requested status transition is not allowed
   */
  TenantDtos.AdminTenantResponse patchTenant(String tenantKey, TenantDtos.AdminUpdateTenantRequest request);

  /**
   * Permanently deletes a tenant and all associated data (cascade).
   * Publishes a {@code tenant.deleted} domain event.
   *
   * @param tenantKey the tenant's unique key
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException if no tenant exists with the given key
   */
  void deleteTenant(String tenantKey);
}
