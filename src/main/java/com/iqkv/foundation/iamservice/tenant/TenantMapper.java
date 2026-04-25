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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantMapper {

  void insertIfAbsent(Tenant tenant);

  Optional<Tenant> findByTenantKey(String tenantKey);

  List<Tenant> findByStatus(String status);

  boolean existsByName(String name);

  void updateStatus(@Param("tenantKey") String tenantKey, @Param("status") String status,
                    @Param("updatedAt") java.time.LocalDateTime updatedAt);

  List<Tenant> findStuckProvisioning(@Param("olderThan") Instant olderThan);

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
