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

package com.iqkv.foundation.iamservice.tenant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TenantDtos Tests")
class TenantDtosTest {

  @Test
  @DisplayName("Should create CreateTenantRequest")
  void shouldCreateCreateTenantRequest() {
    var request = new TenantDtos.CreateTenantRequest("Test Tenant");

    assertThat(request.name()).isEqualTo("Test Tenant");
  }

  @Test
  @DisplayName("Should create UpdateTenantStatusRequest")
  void shouldCreateUpdateTenantStatusRequest() {
    var request = new TenantDtos.UpdateTenantStatusRequest(TenantStatus.ACTIVE);

    assertThat(request.status()).isEqualTo(TenantStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should create TenantResponse")
  void shouldCreateTenantResponse() {
    var createdAt = LocalDateTime.now();
    var response = new TenantDtos.TenantResponse(
        "tenant-key",
        "Test Tenant",
        TenantStatus.ACTIVE,
        createdAt
    );

    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.name()).isEqualTo("Test Tenant");
    assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("Should create UpdateTenantRequest")
  void shouldCreateUpdateTenantRequest() {
    var request = new TenantDtos.UpdateTenantRequest("Updated Tenant Name");

    assertThat(request.name()).isEqualTo("Updated Tenant Name");
  }

  @Test
  @DisplayName("Should create AdminUpdateTenantRequest")
  void shouldCreateAdminUpdateTenantRequest() {
    var request = new TenantDtos.AdminUpdateTenantRequest(
        "Updated Name",
        TenantStatus.SUSPENDED
    );

    assertThat(request.name()).isEqualTo("Updated Name");
    assertThat(request.status()).isEqualTo(TenantStatus.SUSPENDED);
  }

  @Test
  @DisplayName("Should create AdminUpdateMemberAuthoritiesRequest")
  void shouldCreateAdminUpdateMemberAuthoritiesRequest() {
    var request = new TenantDtos.AdminUpdateMemberAuthoritiesRequest(
        List.of("ADMIN", "MEMBER")
    );

    assertThat(request.authorities()).containsExactly("ADMIN", "MEMBER");
  }

  @Test
  @DisplayName("Should create MemberAuthoritiesResponse")
  void shouldCreateMemberAuthoritiesResponse() {
    var userId = UUID.randomUUID();
    var response = new TenantDtos.MemberAuthoritiesResponse(
        userId,
        "tenant-key",
        List.of("MEMBER")
    );

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.authorities()).containsExactly("MEMBER");
  }

  @Test
  @DisplayName("Should create TenantMemberResponse")
  void shouldCreateTenantMemberResponse() {
    var userId = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var response = new TenantDtos.TenantMemberResponse(
        userId,
        "user@example.com",
        "John",
        "Doe",
        "ACTIVE",
        true,
        List.of("MEMBER"),
        List.of("Test Tenant"),
        createdAt,
        updatedAt
    );

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.firstName()).isEqualTo("John");
    assertThat(response.lastName()).isEqualTo("Doe");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.emailVerified()).isTrue();
    assertThat(response.tenantAuthorities()).containsExactly("MEMBER");
    assertThat(response.organizations()).containsExactly("Test Tenant");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should create PagedTenantMemberResponse")
  void shouldCreatePagedTenantMemberResponse() {
    var userId = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var member = new TenantDtos.TenantMemberResponse(
        userId,
        "user@example.com",
        "John",
        "Doe",
        "ACTIVE",
        true,
        List.of("MEMBER"),
        List.of("Test Tenant"),
        createdAt,
        updatedAt
    );
    var response = new TenantDtos.PagedTenantMemberResponse(
        List.of(member),
        0,
        10,
        1L,
        1
    );

    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(10);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create AdminTenantResponse")
  void shouldCreateAdminTenantResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var response = new TenantDtos.AdminTenantResponse(
        id,
        "tenant-key",
        "Test Tenant",
        TenantStatus.ACTIVE,
        true,
        "SINGLE",
        "system",
        createdAt,
        updatedAt
    );

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.name()).isEqualTo("Test Tenant");
    assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
    assertThat(response.isDefault()).isTrue();
    assertThat(response.tenantModeOrigin()).isEqualTo("SINGLE");
    assertThat(response.createdBy()).isEqualTo("system");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should create TenantCountResponse")
  void shouldCreateTenantCountResponse() {
    var response = new TenantDtos.TenantCountResponse(10L);

    assertThat(response.total()).isEqualTo(10L);
  }

  @Test
  @DisplayName("Should create TenantMemberCountResponse")
  void shouldCreateTenantMemberCountResponse() {
    var response = new TenantDtos.TenantMemberCountResponse("tenant-key", 5L);

    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.total()).isEqualTo(5L);
  }

  @Test
  @DisplayName("Should create TenantMemberListQuery")
  void shouldCreateTenantMemberListQuery() {
    var query = new TenantDtos.TenantMemberListQuery(
        1,
        20,
        "email",
        "asc",
        "john",
        "ACTIVE"
    );

    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortBy()).isEqualTo("email");
    assertThat(query.sortDir()).isEqualTo("asc");
    assertThat(query.search()).isEqualTo("john");
    assertThat(query.status()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("Should create PagedTenantResponse")
  void shouldCreatePagedTenantResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var tenant = new TenantDtos.AdminTenantResponse(
        id,
        "tenant-key",
        "Test Tenant",
        TenantStatus.ACTIVE,
        true,
        "SINGLE",
        "system",
        createdAt,
        updatedAt
    );
    var response = new TenantDtos.PagedTenantResponse(
        List.of(tenant),
        0,
        10,
        1L,
        1
    );

    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(10);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create TenantListQuery")
  void shouldCreateTenantListQuery() {
    var query = new TenantDtos.TenantListQuery(
        1,
        20,
        "name",
        "asc",
        "test",
        "ACTIVE"
    );

    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortBy()).isEqualTo("name");
    assertThat(query.sortDir()).isEqualTo("asc");
    assertThat(query.search()).isEqualTo("test");
    assertThat(query.status()).isEqualTo("ACTIVE");
  }
}
