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

package com.iqkv.foundation.iamservice.signup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SignupResult Tests")
class SignupResultTest {

  @Test
  @DisplayName("Should create SignupResult")
  void shouldCreateSignupResult() {
    var user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setStatus(AccountStatus.ACTIVE);

    var tenant = new Tenant();
    tenant.setTenantKey("tenant-key");
    tenant.setName("Test Tenant");
    tenant.setStatus(TenantStatus.ACTIVE);

    var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    membership.setUserId(user.getId());
    membership.setTenantKey(tenant.getTenantKey());
    membership.setStatus(MembershipStatus.ACTIVE);
    membership.setCreatedAt(LocalDateTime.now());

    var authorities = List.of("TENANT_OWNER");

    var result = new SignupResult(user, tenant, membership, authorities);

    assertThat(result.user()).isEqualTo(user);
    assertThat(result.tenant()).isEqualTo(tenant);
    assertThat(result.membership()).isEqualTo(membership);
    assertThat(result.authorities()).containsExactly("TENANT_OWNER");
  }
}
