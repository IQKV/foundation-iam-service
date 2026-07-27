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

package com.iqkv.foundation.iamservice.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvitationStatus Tests")
class InvitationStatusTest {

  @Test
  @DisplayName("Should have all expected statuses")
  void shouldHaveAllExpectedStatuses() {
    var statuses = InvitationStatus.values();

    assertThat(statuses).contains(
        InvitationStatus.PENDING,
        InvitationStatus.ACCEPTED,
        InvitationStatus.REVOKED,
        InvitationStatus.EXPIRED
    );
  }

  @Test
  @DisplayName("Should convert from string")
  void shouldConvertFromString() {
    var pending = InvitationStatus.valueOf("PENDING");
    var accepted = InvitationStatus.valueOf("ACCEPTED");

    assertThat(pending).isEqualTo(InvitationStatus.PENDING);
    assertThat(accepted).isEqualTo(InvitationStatus.ACCEPTED);
  }
}
