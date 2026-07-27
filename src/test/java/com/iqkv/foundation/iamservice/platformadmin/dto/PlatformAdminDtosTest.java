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

package com.iqkv.foundation.iamservice.platformadmin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformAdminDtos Tests")
class PlatformAdminDtosTest {

  @Test
  @DisplayName("Should create AdminAccountResponse")
  void shouldCreateAdminAccountResponse() {
    var userId = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var response = new PlatformAdminDtos.AdminAccountResponse(
        userId,
        "admin@example.com",
        "Admin",
        "User",
        "ACTIVE",
        true,
        List.of("PLATFORM_ADMIN"),
        createdAt,
        updatedAt
    );

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("admin@example.com");
    assertThat(response.firstName()).isEqualTo("Admin");
    assertThat(response.lastName()).isEqualTo("User");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.emailVerified()).isTrue();
    assertThat(response.platformAuthorities()).containsExactly("PLATFORM_ADMIN");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should create AdminUpdateAccountRequest")
  void shouldCreateAdminUpdateAccountRequest() {
    var request = new PlatformAdminDtos.AdminUpdateAccountRequest(
        "Updated",
        "Name"
    );

    assertThat(request.firstName()).isEqualTo("Updated");
    assertThat(request.lastName()).isEqualTo("Name");
  }

  @Test
  @DisplayName("Should create AdminChangePasswordRequest")
  void shouldCreateAdminChangePasswordRequest() {
    var request = new PlatformAdminDtos.AdminChangePasswordRequest(
        "OldPass123!",
        "NewPass456!"
    );

    assertThat(request.currentPassword()).isEqualTo("OldPass123!");
    assertThat(request.newPassword()).isEqualTo("NewPass456!");
  }
}
