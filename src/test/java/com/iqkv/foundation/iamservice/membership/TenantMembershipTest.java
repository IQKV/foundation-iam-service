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

package com.iqkv.foundation.iamservice.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TenantMembership Tests")
class TenantMembershipTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    TenantMembership membership = new TenantMembership();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantKey = "test-tenant";
    MembershipStatus status = MembershipStatus.ACTIVE;
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    String createdBy = "admin";
    String updatedBy = "admin";

    membership.setId(id);
    membership.setUserId(userId);
    membership.setTenantKey(tenantKey);
    membership.setStatus(status);
    membership.setCreatedAt(createdAt);
    membership.setUpdatedAt(updatedAt);
    membership.setCreatedBy(createdBy);
    membership.setUpdatedBy(updatedBy);

    assertThat(membership.getId()).isEqualTo(id);
    assertThat(membership.getUserId()).isEqualTo(userId);
    assertThat(membership.getTenantKey()).isEqualTo(tenantKey);
    assertThat(membership.getStatus()).isEqualTo(status);
    assertThat(membership.getCreatedAt()).isEqualTo(createdAt);
    assertThat(membership.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(membership.getCreatedBy()).isEqualTo(createdBy);
    assertThat(membership.getUpdatedBy()).isEqualTo(updatedBy);
  }
}
