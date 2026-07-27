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

package com.iqkv.foundation.iamservice.lockout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FailedLoginAttempt Tests")
class FailedLoginAttemptTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    FailedLoginAttempt attempt = new FailedLoginAttempt();
    UUID id = UUID.randomUUID();
    String email = "test@example.com";
    Instant attemptedAt = Instant.now();

    attempt.setId(id);
    attempt.setEmail(email);
    attempt.setAttemptedAt(attemptedAt);

    assertThat(attempt.getId()).isEqualTo(id);
    assertThat(attempt.getEmail()).isEqualTo(email);
    assertThat(attempt.getAttemptedAt()).isEqualTo(attemptedAt);
  }
}
