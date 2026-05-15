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

package com.iqkv.foundation.iamservice.platformadmin;

import java.util.List;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;
import com.iqkv.foundation.iamservice.user.User;

public final class PlatformAdminDtoMapper {

  private PlatformAdminDtoMapper() {
  }

  public static PlatformAdminDtos.AdminAccountResponse toAccountResponse(
      final User user,
      final List<String> platformAuthorities) {
    if (user == null) {
      throw new IllegalArgumentException("user must not be null");
    }
    return new PlatformAdminDtos.AdminAccountResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getStatus() != null ? user.getStatus().name() : null,
        user.isEmailVerified(),
        platformAuthorities,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
