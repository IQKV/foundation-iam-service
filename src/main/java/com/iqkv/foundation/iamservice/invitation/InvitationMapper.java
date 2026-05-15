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

package com.iqkv.foundation.iamservice.invitation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvitationMapper {

  void insert(TenantInvitation invitation);

  Optional<TenantInvitation> findByToken(String token);

  Optional<TenantInvitation> findById(UUID id);

  /**
   * Returns all non-terminal invitations for a tenant (PENDING only).
   */
  List<TenantInvitation> findPendingByTenantKey(String tenantKey);

  /**
   * Returns all invitations for a tenant regardless of status (for management views).
   */
  List<TenantInvitation> findByTenantKey(String tenantKey);

  boolean existsPendingForTenantAndEmail(
      @Param("tenantKey") String tenantKey,
      @Param("invitedEmail") String invitedEmail);

  void updateStatus(
      @Param("id") UUID id,
      @Param("status") String status,
      @Param("updatedAt") java.time.LocalDateTime updatedAt);

  void markAccepted(
      @Param("id") UUID id,
      @Param("acceptedAt") Instant acceptedAt,
      @Param("updatedAt") java.time.LocalDateTime updatedAt);

  /**
   * Bulk-expire all PENDING invitations whose TTL has passed. Used by the reaper job.
   */
  int expireStale(
      @Param("now") Instant now,
      @Param("updatedAt") java.time.LocalDateTime updatedAt);

  List<TenantInvitation> findAll(
      @Param("limit") int limit,
      @Param("offset") int offset,
      @Param("sortBy") String sortBy,
      @Param("sortDir") String sortDir,
      @Param("search") String search,
      @Param("status") String status,
      @Param("tenantKey") String tenantKey);

  long countAll(
      @Param("search") String search,
      @Param("status") String status,
      @Param("tenantKey") String tenantKey);
}
