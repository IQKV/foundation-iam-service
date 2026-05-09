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

package com.iqkv.foundation.iamservice.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountStatus Tests")
class AccountStatusTest {

  @Test
  @DisplayName("Should have all expected status values")
  void shouldHaveActiveStatus() {
    var statuses = AccountStatus.values();

    assertThat(statuses).containsExactlyInAnyOrder(
        AccountStatus.ACTIVE,
        AccountStatus.LOCKED,
        AccountStatus.SUSPENDED,
        AccountStatus.DELETED
    );
  }

  @Test
  @DisplayName("Should convert from string")
  void shouldConvertFromString() {
    assertThat(AccountStatus.valueOf("ACTIVE")).isEqualTo(AccountStatus.ACTIVE);
    assertThat(AccountStatus.valueOf("LOCKED")).isEqualTo(AccountStatus.LOCKED);
    assertThat(AccountStatus.valueOf("SUSPENDED")).isEqualTo(AccountStatus.SUSPENDED);
    assertThat(AccountStatus.valueOf("DELETED")).isEqualTo(AccountStatus.DELETED);
  }
}
