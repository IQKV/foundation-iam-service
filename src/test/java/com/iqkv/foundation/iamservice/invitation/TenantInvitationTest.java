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

package com.iqkv.foundation.iamservice.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TenantInvitation Tests")
class TenantInvitationTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    TenantInvitation invitation = new TenantInvitation();
    UUID id = UUID.randomUUID();
    String tenantKey = "test-tenant";
    String invitedEmail = "test@example.com";
    UUID invitedBy = UUID.randomUUID();
    String authority = "MEMBER";
    String token = "test-token";
    InvitationStatus status = InvitationStatus.PENDING;
    Instant expiresAt = Instant.now().plusSeconds(86400);
    Instant acceptedAt = Instant.now();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    String createdBy = "admin";
    String updatedBy = "admin";

    invitation.setId(id);
    invitation.setTenantKey(tenantKey);
    invitation.setInvitedEmail(invitedEmail);
    invitation.setInvitedBy(invitedBy);
    invitation.setAuthority(authority);
    invitation.setToken(token);
    invitation.setStatus(status);
    invitation.setExpiresAt(expiresAt);
    invitation.setAcceptedAt(acceptedAt);
    invitation.setCreatedAt(createdAt);
    invitation.setUpdatedAt(updatedAt);
    invitation.setCreatedBy(createdBy);
    invitation.setUpdatedBy(updatedBy);

    assertThat(invitation.getId()).isEqualTo(id);
    assertThat(invitation.getTenantKey()).isEqualTo(tenantKey);
    assertThat(invitation.getInvitedEmail()).isEqualTo(invitedEmail);
    assertThat(invitation.getInvitedBy()).isEqualTo(invitedBy);
    assertThat(invitation.getAuthority()).isEqualTo(authority);
    assertThat(invitation.getToken()).isEqualTo(token);
    assertThat(invitation.getStatus()).isEqualTo(status);
    assertThat(invitation.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(invitation.getAcceptedAt()).isEqualTo(acceptedAt);
    assertThat(invitation.getCreatedAt()).isEqualTo(createdAt);
    assertThat(invitation.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(invitation.getCreatedBy()).isEqualTo(createdBy);
    assertThat(invitation.getUpdatedBy()).isEqualTo(updatedBy);
  }

  @Test
  @DisplayName("Should return true for isPending when status is PENDING and not expired")
  void shouldReturnTrueForIsPendingWhenPendingAndNotExpired() {
    TenantInvitation invitation = new TenantInvitation();
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setExpiresAt(Instant.now().plusSeconds(86400));
    assertThat(invitation.isPending()).isTrue();
  }

  @Test
  @DisplayName("Should return false for isPending when status is not PENDING")
  void shouldReturnFalseForIsPendingWhenNotPending() {
    TenantInvitation invitation = new TenantInvitation();
    invitation.setStatus(InvitationStatus.ACCEPTED);
    invitation.setExpiresAt(Instant.now().plusSeconds(86400));
    assertThat(invitation.isPending()).isFalse();
  }

  @Test
  @DisplayName("Should return false for isPending when expired")
  void shouldReturnFalseForIsPendingWhenExpired() {
    TenantInvitation invitation = new TenantInvitation();
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setExpiresAt(Instant.now().minusSeconds(86400));
    assertThat(invitation.isPending()).isFalse();
  }
}
