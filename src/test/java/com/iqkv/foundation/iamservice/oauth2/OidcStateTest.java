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

package com.iqkv.foundation.iamservice.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OidcState Tests")
class OidcStateTest {

  @Test
  @DisplayName("Should create OidcState with all fields")
  void shouldCreateWithAllFields() {
    final UUID jti = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant expiresAt = Instant.now().plusSeconds(600);

    final var state = new OidcState(
        jti,
        "google",
        "nonce-abc",
        "tenant01",
        "https://app.example.com/callback",
        "login",
        userId,
        expiresAt
    );

    assertThat(state.jti()).isEqualTo(jti);
    assertThat(state.provider()).isEqualTo("google");
    assertThat(state.nonce()).isEqualTo("nonce-abc");
    assertThat(state.tenantKey()).isEqualTo("tenant01");
    assertThat(state.redirectUri()).isEqualTo("https://app.example.com/callback");
    assertThat(state.flowType()).isEqualTo("login");
    assertThat(state.userId()).isEqualTo(userId);
    assertThat(state.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("Should create OidcState with null optional fields")
  void shouldCreateWithNullOptionalFields() {
    final UUID jti = UUID.randomUUID();
    final Instant expiresAt = Instant.now().plusSeconds(600);

    final var state = new OidcState(jti, "github", "nonce-xyz", null, null, "link", null, expiresAt);

    assertThat(state.tenantKey()).isNull();
    assertThat(state.userId()).isNull();
    assertThat(state.redirectUri()).isNull();
  }

  @Test
  @DisplayName("Should be equal when all fields match")
  void shouldBeEqualWhenFieldsMatch() {
    final UUID jti = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant expiresAt = Instant.parse("2026-12-31T00:00:00Z");

    final var a = new OidcState(jti, "google", "n1", "t1", "https://cb", "login", userId, expiresAt);
    final var b = new OidcState(jti, "google", "n1", "t1", "https://cb", "login", userId, expiresAt);

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when jti differs")
  void shouldNotBeEqualWhenJtiDiffers() {
    final Instant expiresAt = Instant.parse("2026-12-31T00:00:00Z");

    final var a = new OidcState(UUID.randomUUID(), "google", "n1", "t1", null, "login", null, expiresAt);
    final var b = new OidcState(UUID.randomUUID(), "google", "n1", "t1", null, "login", null, expiresAt);

    assertThat(a).isNotEqualTo(b);
  }
}
