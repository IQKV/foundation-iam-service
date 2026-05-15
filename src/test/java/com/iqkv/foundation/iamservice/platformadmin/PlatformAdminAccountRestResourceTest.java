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

package com.iqkv.foundation.iamservice.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformAdminAccountRestResource Unit Tests")
class PlatformAdminAccountRestResourceTest {

  @Mock
  private PlatformAdminAccountService platformAdminAccountService;

  @Mock
  private Jwt jwt;

  private PlatformAdminAccountRestResource resource;

  private final UUID userId = UUID.randomUUID();
  private PlatformAdminDtos.AdminAccountResponse accountResponse;

  @BeforeEach
  void setUp() {
    resource = new PlatformAdminAccountRestResource(platformAdminAccountService);

    accountResponse = new PlatformAdminDtos.AdminAccountResponse(
        userId,
        "admin@example.com",
        "Platform",
        "Admin",
        "ACTIVE",
        true,
        List.of("PLATFORM_ADMIN"),
        LocalDateTime.now(),
        LocalDateTime.now());

    when(jwt.getClaimAsString(JwtClaimNames.USER_ID)).thenReturn(userId.toString());
  }

  // ─── GET /api/v1/iam/auth/admin/me ────────────────────────────────────────

  @Test
  @DisplayName("GET /me — should return 200 with account profile")
  void shouldGetAccountSuccessfully() {
    when(platformAdminAccountService.getAccount(userId)).thenReturn(accountResponse);

    final var response = resource.getAccount(jwt);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().userId()).isEqualTo(userId);
    assertThat(response.getBody().email()).isEqualTo("admin@example.com");
    assertThat(response.getBody().firstName()).isEqualTo("Platform");
    assertThat(response.getBody().lastName()).isEqualTo("Admin");
    assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    assertThat(response.getBody().emailVerified()).isTrue();
    assertThat(response.getBody().platformAuthorities()).containsExactly("PLATFORM_ADMIN");
    verify(platformAdminAccountService).getAccount(userId);
  }

  @Test
  @DisplayName("GET /me — should extract userId from JWT claim")
  void shouldExtractUserIdFromJwt() {
    when(platformAdminAccountService.getAccount(userId)).thenReturn(accountResponse);

    resource.getAccount(jwt);

    verify(jwt).getClaimAsString(JwtClaimNames.USER_ID);
    verify(platformAdminAccountService).getAccount(userId);
  }

  // ─── PATCH /api/v1/iam/auth/admin/me ──────────────────────────────────────

  @Test
  @DisplayName("PATCH /me — should return 200 with updated account")
  void shouldUpdateAccountSuccessfully() {
    final var request = new PlatformAdminDtos.AdminUpdateAccountRequest("New", "Name");
    final var updatedResponse = new PlatformAdminDtos.AdminAccountResponse(
        userId,
        "admin@example.com",
        "New",
        "Name",
        "ACTIVE",
        true,
        List.of("PLATFORM_ADMIN"),
        LocalDateTime.now(),
        LocalDateTime.now());

    when(platformAdminAccountService.updateAccount(eq(userId), any(PlatformAdminDtos.AdminUpdateAccountRequest.class)))
        .thenReturn(updatedResponse);

    final var response = resource.updateAccount(jwt, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().firstName()).isEqualTo("New");
    assertThat(response.getBody().lastName()).isEqualTo("Name");
    verify(platformAdminAccountService).updateAccount(userId, request);
  }

  @Test
  @DisplayName("PATCH /me — should pass request body to service unchanged")
  void shouldPassRequestBodyToService() {
    final var request = new PlatformAdminDtos.AdminUpdateAccountRequest("Jane", "Doe");
    when(platformAdminAccountService.updateAccount(eq(userId), eq(request)))
        .thenReturn(accountResponse);

    resource.updateAccount(jwt, request);

    verify(platformAdminAccountService).updateAccount(userId, request);
  }

  @Test
  @DisplayName("PATCH /me — response must not include organizations or tenant fields")
  void responseShouldNotIncludeTenantFields() {
    final var request = new PlatformAdminDtos.AdminUpdateAccountRequest("Platform", "Admin");
    when(platformAdminAccountService.updateAccount(eq(userId), any())).thenReturn(accountResponse);

    final var response = resource.updateAccount(jwt, request);

    assertThat(response.getBody()).isNotNull();
    // AdminAccountResponse has no organizations / membershipAuthorities fields —
    // only platformAuthorities. Verify the response type is correct.
    assertThat(response.getBody()).isInstanceOf(PlatformAdminDtos.AdminAccountResponse.class);
    assertThat(response.getBody().platformAuthorities()).isNotNull();
  }
}
