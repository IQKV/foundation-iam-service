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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailVerificationToken Tests")
class EmailVerificationTokenTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    EmailVerificationToken token = new EmailVerificationToken();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tokenValue = "test-token";
    Instant expiresAt = Instant.now().plusSeconds(86400);
    int resendCount = 2;
    Instant lastResendAt = Instant.now();
    Instant createdAt = Instant.now();

    token.setId(id);
    token.setUserId(userId);
    token.setToken(tokenValue);
    token.setExpiresAt(expiresAt);
    token.setResendCount(resendCount);
    token.setLastResendAt(lastResendAt);
    token.setCreatedAt(createdAt);

    assertThat(token.getId()).isEqualTo(id);
    assertThat(token.getUserId()).isEqualTo(userId);
    assertThat(token.getToken()).isEqualTo(tokenValue);
    assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(token.getResendCount()).isEqualTo(resendCount);
    assertThat(token.getLastResendAt()).isEqualTo(lastResendAt);
    assertThat(token.getCreatedAt()).isEqualTo(createdAt);
  }
}
