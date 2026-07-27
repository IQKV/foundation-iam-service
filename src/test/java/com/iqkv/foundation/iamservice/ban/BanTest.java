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

package com.iqkv.foundation.iamservice.ban;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ban Tests")
class BanTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    Ban ban = new Ban();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID initiatorId = UUID.randomUUID();
    BanType type = BanType.TENANT;
    String tenantKey = "test-tenant";
    String reason = "Violation of terms";
    LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    String createdBy = "system";
    String updatedBy = "admin";

    ban.setId(id);
    ban.setUserId(userId);
    ban.setInitiatorId(initiatorId);
    ban.setType(type);
    ban.setTenantKey(tenantKey);
    ban.setReason(reason);
    ban.setExpiresAt(expiresAt);
    ban.setCreatedAt(createdAt);
    ban.setUpdatedAt(updatedAt);
    ban.setCreatedBy(createdBy);
    ban.setUpdatedBy(updatedBy);

    assertThat(ban.getId()).isEqualTo(id);
    assertThat(ban.getUserId()).isEqualTo(userId);
    assertThat(ban.getInitiatorId()).isEqualTo(initiatorId);
    assertThat(ban.getType()).isEqualTo(type);
    assertThat(ban.getTenantKey()).isEqualTo(tenantKey);
    assertThat(ban.getReason()).isEqualTo(reason);
    assertThat(ban.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(ban.getCreatedAt()).isEqualTo(createdAt);
    assertThat(ban.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(ban.getCreatedBy()).isEqualTo(createdBy);
    assertThat(ban.getUpdatedBy()).isEqualTo(updatedBy);
  }
}
