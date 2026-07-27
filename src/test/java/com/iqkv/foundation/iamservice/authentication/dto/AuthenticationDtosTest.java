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

package com.iqkv.foundation.iamservice.authentication.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthenticationDtos Tests")
class AuthenticationDtosTest {

  @Test
  @DisplayName("Should create SignInRequest")
  void shouldCreateSignInRequest() {
    var request = new AuthenticationDtos.SignInRequest("user@example.com", "password123");

    assertThat(request.email()).isEqualTo("user@example.com");
    assertThat(request.password()).isEqualTo("password123");
  }

  @Test
  @DisplayName("Should create RefreshTokenRequest")
  void shouldCreateRefreshTokenRequest() {
    var request = new AuthenticationDtos.RefreshTokenRequest("refresh-token");

    assertThat(request.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  @DisplayName("Should create TokenResponse")
  void shouldCreateTokenResponse() {
    var response = new AuthenticationDtos.TokenResponse(
        "access-token",
        "refresh-token",
        "tenant-key"
    );

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
  }

  @Test
  @DisplayName("Should create ValidateTokenResponse")
  void shouldCreateValidateTokenResponse() {
    var userId = UUID.randomUUID();
    var authorities = List.of("ADMIN", "MEMBER");
    var response = new AuthenticationDtos.ValidateTokenResponse(
        userId,
        "user@example.com",
        "tenant-key",
        authorities
    );

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.tenantId()).isEqualTo("tenant-key");
    assertThat(response.authorities()).containsExactly("ADMIN", "MEMBER");
  }

  @Test
  @DisplayName("Should create TenantMembershipSummary")
  void shouldCreateTenantMembershipSummary() {
    var authorities = List.of("MEMBER");
    var summary = new AuthenticationDtos.TenantMembershipSummary(
        "tenant-key",
        "Tenant Name",
        "ACTIVE",
        authorities,
        false
    );

    assertThat(summary.tenantKey()).isEqualTo("tenant-key");
    assertThat(summary.tenantName()).isEqualTo("Tenant Name");
    assertThat(summary.membershipStatus()).isEqualTo("ACTIVE");
    assertThat(summary.authorities()).containsExactly("MEMBER");
    assertThat(summary.isPersonal()).isFalse();
  }
}
