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

package com.iqkv.foundation.iamservice.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

@Mapper
public interface UserMapper {

  void upsertByEmail(User user);

  Optional<User> findById(UUID id);

  Cursor<User> findAllStreaming();

  Optional<UserWithOrganizations> findByIdWithAuthorities(UUID id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<UserWithOrganizations> findAll(@Param("limit") int limit, @Param("offset") int offset,
                                      @Param("sortBy") String sortBy, @Param("sortDir") String sortDir,
                                      @Param("search") String search, @Param("status") String status,
                                      @Param("excludePlatformAdmins") Boolean excludePlatformAdmins);

  long countAll(@Param("search") String search, @Param("status") String status,
                @Param("excludePlatformAdmins") Boolean excludePlatformAdmins);

  List<UserWithOrganizations> findMembersByTenantKey(@Param("tenantKey") String tenantKey,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset,
                                                     @Param("sortBy") String sortBy,
                                                     @Param("sortDir") String sortDir,
                                                     @Param("search") String search,
                                                     @Param("status") String status);

  /**
   * Tenant-scoped member list query.
   *
   * <p>Returns members of the given tenant with {@code membershipAuthorities} populated
   * from <em>only that tenant's</em> {@code tenant_member_authorities} rows.
   * {@code organizations} still aggregates all tenant names the user belongs to
   * (cross-tenant context, useful for the admin UI).
   *
   * <p>Distinct from {@link #findMembersByTenantKey} which is retained for
   * backward-compatibility but carries the same authority semantics.
   * This method is the canonical source for the tenant member list UI.
   */
  List<UserWithOrganizations> findMembersByTenantKeyScoped(@Param("tenantKey") String tenantKey,
                                                           @Param("limit") int limit,
                                                           @Param("offset") int offset,
                                                           @Param("sortBy") String sortBy,
                                                           @Param("sortDir") String sortDir,
                                                           @Param("search") String search,
                                                           @Param("status") String status);

  long countMembersByTenantKey(@Param("tenantKey") String tenantKey,
                               @Param("search") String search,
                               @Param("status") String status);

  void update(User user);

  void deleteById(@Param("id") UUID id);

  void updateLastGlobalSignoutAt(@Param("userId") UUID userId,
                                 @Param("lastGlobalSignoutAt") Instant lastGlobalSignoutAt);

  Optional<Instant> findLastGlobalSignoutAt(@Param("userId") UUID userId);

  void setEmailVerified(@Param("userId") UUID userId);

  void updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash,
                      @Param("updatedAt") Instant updatedAt);

  void updateAvatarUrl(@Param("userId") UUID userId, @Param("avatarUrl") String avatarUrl);
}
