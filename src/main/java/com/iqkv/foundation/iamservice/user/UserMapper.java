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

@Mapper
public interface UserMapper {

  void upsertByEmail(User user);

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<UserWithOrganizations> findAll(@Param("limit") int limit, @Param("offset") int offset,
                                      @Param("sortBy") String sortBy, @Param("sortDir") String sortDir,
                                      @Param("search") String search, @Param("status") String status);

  long countAll(@Param("search") String search, @Param("status") String status);

  List<UserWithOrganizations> findMembersByTenantKey(@Param("tenantKey") String tenantKey,
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
}
