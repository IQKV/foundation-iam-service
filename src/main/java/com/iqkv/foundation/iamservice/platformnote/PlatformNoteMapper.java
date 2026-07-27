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

package com.iqkv.foundation.iamservice.platformnote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlatformNoteMapper {

  void insert(PlatformNote note);

  void update(PlatformNote note);

  void delete(@Param("id") UUID id);

  Optional<PlatformNote> findById(@Param("id") UUID id);

  List<PlatformNote> findAll(@Param("search") String search,
                             @Param("severity") String severity,
                             @Param("status") String status,
                             @Param("sortBy") String sortBy,
                             @Param("sortDir") String sortDir,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

  long countAll(@Param("search") String search,
                @Param("severity") String severity,
                @Param("status") String status);
}
