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

package com.iqkv.foundation.iamservice.ban;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BanMapper {

  void insert(Ban ban);

  void update(Ban ban);

  Optional<Ban> findById(UUID id);

  Optional<Ban> findActiveBan(@Param("userId") UUID userId,
                              @Param("tenantKey") String tenantKey);

  Optional<Ban> findActivePlatformBan(@Param("userId") UUID userId);

  Optional<Ban> findActiveTenantBan(@Param("userId") UUID userId,
                                    @Param("tenantKey") String tenantKey);

  List<Ban> findAllByUserId(UUID userId);

  List<Ban> findAllByTenantKey(String tenantKey);

  void deleteById(UUID id);

  void deleteByUserIdAndTypeAndTenantKey(@Param("userId") UUID userId,
                                         @Param("type") BanType type,
                                         @Param("tenantKey") String tenantKey);
}
