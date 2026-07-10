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

package com.iqkv.foundation.iamservice.magiclink;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MagicLinkToken Tests")
class MagicLinkTokenTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    final var token = new MagicLinkToken();
    final UUID id = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final String tokenValue = "abc123token";
    final String tenantKey = "tenant01";
    final Instant expiresAt = Instant.now().plusSeconds(900);
    final Instant lastResendAt = Instant.now();
    final Instant createdAt = Instant.now();

    token.setId(id);
    token.setUserId(userId);
    token.setToken(tokenValue);
    token.setTenantKey(tenantKey);
    token.setExpiresAt(expiresAt);
    token.setResendCount(2);
    token.setLastResendAt(lastResendAt);
    token.setCreatedAt(createdAt);

    assertThat(token.getId()).isEqualTo(id);
    assertThat(token.getUserId()).isEqualTo(userId);
    assertThat(token.getToken()).isEqualTo(tokenValue);
    assertThat(token.getTenantKey()).isEqualTo(tenantKey);
    assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(token.getResendCount()).isEqualTo(2);
    assertThat(token.getLastResendAt()).isEqualTo(lastResendAt);
    assertThat(token.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("Should default resendCount to zero on new instance")
  void shouldDefaultResendCountToZero() {
    assertThat(new MagicLinkToken().getResendCount()).isZero();
  }

  @Test
  @DisplayName("Should allow null optional fields on new instance")
  void shouldAllowNullOptionalFields() {
    final var token = new MagicLinkToken();
    assertThat(token.getId()).isNull();
    assertThat(token.getLastResendAt()).isNull();
    assertThat(token.getTenantKey()).isNull();
  }
}
