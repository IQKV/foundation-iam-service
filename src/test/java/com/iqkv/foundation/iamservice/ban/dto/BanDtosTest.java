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

package com.iqkv.foundation.iamservice.ban.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BanDtos Tests")
class BanDtosTest {

  @Test
  @DisplayName("Should create CreateBanRequest")
  void shouldCreateCreateBanRequest() {
    var expiresAt = LocalDateTime.now().plusDays(7);
    var request = new BanDtos.CreateBanRequest(
        "Violation of terms",
        expiresAt
    );

    assertThat(request.reason()).isEqualTo("Violation of terms");
    assertThat(request.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("Should create BanResponse")
  void shouldCreateBanResponse() {
    var id = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var initiatorId = UUID.randomUUID();
    var expiresAt = LocalDateTime.now().plusDays(7);
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var response = new BanDtos.BanResponse(
        id,
        userId,
        initiatorId,
        "TENANT",
        "tenant-key",
        "Violation of terms",
        expiresAt,
        createdAt,
        updatedAt
    );

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.initiatorId()).isEqualTo(initiatorId);
    assertThat(response.type()).isEqualTo("TENANT");
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.reason()).isEqualTo("Violation of terms");
    assertThat(response.expiresAt()).isEqualTo(expiresAt);
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }
}
