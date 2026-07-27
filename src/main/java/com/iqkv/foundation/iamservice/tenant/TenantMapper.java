/*
 * Copyright 2026 iQKV Foundation Team.
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantMapper {

  void insertIfAbsent(Tenant tenant);

  Optional<Tenant> findByTenantKey(String tenantKey);

  Optional<Tenant> findById(@Param("id") UUID id);

  List<Tenant> findByStatus(String status);

  boolean existsByName(String name);

  void updateStatus(@Param("tenantKey") String tenantKey, @Param("status") String status,
                    @Param("updatedAt") LocalDateTime updatedAt);

  void update(Tenant tenant);

  /**
   * Sets {@code active_plan_code} for the given tenant.
   * Called when a {@code subscription.created} or {@code subscription.updated} event is received.
   *
   * @param tenantKey the tenant's unique key
   * @param planCode  the human-readable plan code (e.g. {@code "pro-monthly"}); may be null to clear
   */
  void updateActivePlanCode(@Param("tenantKey") String tenantKey, @Param("planCode") String planCode, @Param("seatCount") Long seatCount);

  void deleteByTenantKey(@Param("tenantKey") String tenantKey);

  List<Tenant> findAll(@Param("limit") int limit, @Param("offset") int offset,
                       @Param("sortBy") String sortBy, @Param("sortDir") String sortDir,
                       @Param("search") String search, @Param("status") String status);

  long countAll(@Param("search") String search, @Param("status") String status);

  List<Tenant> findStuckProvisioning(@Param("olderThan") Instant olderThan);

  /**
   * Returns all tenant keys whose schemas should be migrated on startup.
   * Excludes tenants in terminal states ({@code DELETED}) that no longer have an active schema.
   * Used exclusively by {@link com.iqkv.foundation.iamservice.tenant.AllTenantsKeyProvider}.
   */
  List<String> findAllTenantKeysForUpgrade();

  Optional<OwnerInfo> findOwnerByTenantKey(String tenantKey);

  /**
   * Finds the default tenant (is_default = true).
   * Returns empty if no default tenant exists.
   */
  Optional<Tenant> findDefaultTenant();

  /**
   * Marks the specified tenant as the default tenant.
   * Sets is_default = true for the given key.
   */
  void markDefaultTenant(@Param("tenantKey") String tenantKey);

  /**
   * Atomically inserts a default tenant if none exists.
   * Uses INSERT ... ON CONFLICT DO NOTHING pattern.
   */
  void insertIfAbsentDefault(Tenant tenant);
}
