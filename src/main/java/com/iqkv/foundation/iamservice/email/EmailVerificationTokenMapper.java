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

package com.iqkv.foundation.iamservice.email;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailVerificationTokenMapper {

  void insert(EmailVerificationToken token);

  Optional<EmailVerificationToken> findByToken(String token);

  void deleteByUserId(UUID userId);

  void deleteByExpiresAtBefore(Instant cutoff);

  void incrementResendCount(@Param("userId") UUID userId, @Param("now") Instant now);

  int countResendsWithinWindow(@Param("userId") UUID userId, @Param("since") Instant since);
}
