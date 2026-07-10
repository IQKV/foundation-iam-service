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

package com.iqkv.foundation.iamservice.magiclink.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MagicLinkDtos Tests")
class MagicLinkDtosTest {

  @Test
  @DisplayName("Should create InitiateMagicLinkRequest with email and tenantKey")
  void shouldCreateInitiateMagicLinkRequest() {
    final var request = new MagicLinkDtos.InitiateMagicLinkRequest("user@example.com", "tenant01");

    assertThat(request.email()).isEqualTo("user@example.com");
    assertThat(request.tenantKey()).isEqualTo("tenant01");
  }

  @Test
  @DisplayName("Should create InitiateMagicLinkRequest with null tenantKey")
  void shouldCreateInitiateMagicLinkRequestWithNullTenantKey() {
    final var request = new MagicLinkDtos.InitiateMagicLinkRequest("user@example.com", null);

    assertThat(request.email()).isEqualTo("user@example.com");
    assertThat(request.tenantKey()).isNull();
  }

  @Test
  @DisplayName("Should create ExchangeMagicLinkRequest")
  void shouldCreateExchangeMagicLinkRequest() {
    final var request = new MagicLinkDtos.ExchangeMagicLinkRequest("some-secure-token-value");

    assertThat(request.token()).isEqualTo("some-secure-token-value");
  }

  @Test
  @DisplayName("InitiateMagicLinkRequest equality should be based on field values")
  void shouldBeEqualWhenFieldsMatch() {
    final var a = new MagicLinkDtos.InitiateMagicLinkRequest("user@example.com", "tenant01");
    final var b = new MagicLinkDtos.InitiateMagicLinkRequest("user@example.com", "tenant01");

    assertThat(a).isEqualTo(b);
  }

  @Test
  @DisplayName("ExchangeMagicLinkRequest equality should be based on field values")
  void shouldBeEqualExchangeWhenFieldsMatch() {
    final var a = new MagicLinkDtos.ExchangeMagicLinkRequest("tok");
    final var b = new MagicLinkDtos.ExchangeMagicLinkRequest("tok");

    assertThat(a).isEqualTo(b);
  }
}
