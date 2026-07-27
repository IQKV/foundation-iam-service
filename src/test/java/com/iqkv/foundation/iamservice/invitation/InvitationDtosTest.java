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

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvitationDtos Tests")
class InvitationDtosTest {

  @Test
  @DisplayName("Should create SendInvitationRequest")
  void shouldCreateSendInvitationRequest() {
    var request = new InvitationDtos.SendInvitationRequest(
        "invitee@example.com",
        "ADMIN"
    );

    assertThat(request.email()).isEqualTo("invitee@example.com");
    assertThat(request.authority()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("Should create AcceptInvitationRequest")
  void shouldCreateAcceptInvitationRequest() {
    var request = new InvitationDtos.AcceptInvitationRequest(
        "John",
        "Doe",
        "password123"
    );

    assertThat(request.firstName()).isEqualTo("John");
    assertThat(request.lastName()).isEqualTo("Doe");
    assertThat(request.password()).isEqualTo("password123");
  }

  @Test
  @DisplayName("Should create InvitationResponse")
  void shouldCreateInvitationResponse() {
    var invitationId = UUID.randomUUID();
    var expiresAt = Instant.now().plusSeconds(86400);
    var createdAt = Instant.now();
    var response = new InvitationDtos.InvitationResponse(
        invitationId,
        "tenant-key",
        "invitee@example.com",
        "MEMBER",
        "PENDING",
        expiresAt,
        createdAt
    );

    assertThat(response.invitationId()).isEqualTo(invitationId);
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.email()).isEqualTo("invitee@example.com");
    assertThat(response.authority()).isEqualTo("MEMBER");
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.expiresAt()).isEqualTo(expiresAt);
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("Should create InvitationPreviewResponse")
  void shouldCreateInvitationPreviewResponse() {
    var invitationId = UUID.randomUUID();
    var expiresAt = Instant.now().plusSeconds(86400);
    var response = new InvitationDtos.InvitationPreviewResponse(
        invitationId,
        "Test Tenant",
        "invitee@example.com",
        "ADMIN",
        expiresAt,
        true
    );

    assertThat(response.invitationId()).isEqualTo(invitationId);
    assertThat(response.tenantName()).isEqualTo("Test Tenant");
    assertThat(response.email()).isEqualTo("invitee@example.com");
    assertThat(response.authority()).isEqualTo("ADMIN");
    assertThat(response.expiresAt()).isEqualTo(expiresAt);
    assertThat(response.requiresSignup()).isTrue();
  }

  @Test
  @DisplayName("Should create AcceptInvitationResponse")
  void shouldCreateAcceptInvitationResponse() {
    var response = new InvitationDtos.AcceptInvitationResponse(
        "access-token",
        "refresh-token",
        "tenant-key"
    );

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
  }
}
