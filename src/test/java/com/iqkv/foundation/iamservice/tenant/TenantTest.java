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

package com.iqkv.foundation.iamservice.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tenant Tests")
class TenantTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    Tenant tenant = new Tenant();
    UUID id = UUID.randomUUID();
    String tenantKey = "test-tenant";
    String name = "Test Tenant";
    TenantStatus status = TenantStatus.ACTIVE;
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    String createdBy = "system";
    String updatedBy = "admin";
    Boolean isDefault = true;
    String tenantModeOrigin = "single";

    tenant.setId(id);
    tenant.setTenantKey(tenantKey);
    tenant.setName(name);
    tenant.setStatus(status);
    tenant.setCreatedAt(createdAt);
    tenant.setUpdatedAt(updatedAt);
    tenant.setCreatedBy(createdBy);
    tenant.setUpdatedBy(updatedBy);
    tenant.setIsDefault(isDefault);
    tenant.setTenantModeOrigin(tenantModeOrigin);

    assertThat(tenant.getId()).isEqualTo(id);
    assertThat(tenant.getTenantKey()).isEqualTo(tenantKey);
    assertThat(tenant.getName()).isEqualTo(name);
    assertThat(tenant.getStatus()).isEqualTo(status);
    assertThat(tenant.getCreatedAt()).isEqualTo(createdAt);
    assertThat(tenant.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(tenant.getCreatedBy()).isEqualTo(createdBy);
    assertThat(tenant.getUpdatedBy()).isEqualTo(updatedBy);
    assertThat(tenant.getIsDefault()).isEqualTo(isDefault);
    assertThat(tenant.getTenantModeOrigin()).isEqualTo(tenantModeOrigin);
  }
}
