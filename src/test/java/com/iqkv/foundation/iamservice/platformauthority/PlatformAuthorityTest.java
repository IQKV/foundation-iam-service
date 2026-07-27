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

package com.iqkv.foundation.iamservice.platformauthority;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformAuthority Tests")
class PlatformAuthorityTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    PlatformAuthority platformAuthority = new PlatformAuthority();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String authority = "PLATFORM_ADMIN";
    Instant grantedAt = Instant.now();
    String grantedBy = "system";

    platformAuthority.setId(id);
    platformAuthority.setUserId(userId);
    platformAuthority.setRole(authority);
    platformAuthority.setGrantedAt(grantedAt);
    platformAuthority.setGrantedBy(grantedBy);

    assertThat(platformAuthority.getId()).isEqualTo(id);
    assertThat(platformAuthority.getUserId()).isEqualTo(userId);
    assertThat(platformAuthority.getRole()).isEqualTo(authority);
    assertThat(platformAuthority.getGrantedAt()).isEqualTo(grantedAt);
    assertThat(platformAuthority.getGrantedBy()).isEqualTo(grantedBy);
  }
}
