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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain class representing a pending invitation to join a tenant.
 *
 * <p>Stored in the system schema ({@code public}) so the token can be resolved
 * before tenant context is established — the invitee may not have an account yet.
 *
 * <p>One active (PENDING) invitation per {@code (tenantKey, invitedEmail)} is enforced
 * by a partial unique index on the database.
 */
public class TenantInvitation {

  private UUID id;
  private String tenantKey;
  private String invitedEmail;
  private UUID invitedBy;
  private String authority;      // authority granted on accept — defaults to MEMBER; TENANT_OWNER is not grantable via invite
  private String token;          // 32-byte SecureRandom hex (64 chars)
  private InvitationStatus status;
  private Instant expiresAt;
  private Instant acceptedAt;    // nullable
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getInvitedEmail() {
    return invitedEmail;
  }

  public void setInvitedEmail(final String invitedEmail) {
    this.invitedEmail = invitedEmail;
  }

  public UUID getInvitedBy() {
    return invitedBy;
  }

  public void setInvitedBy(final UUID invitedBy) {
    this.invitedBy = invitedBy;
  }

  public String getAuthority() {
    return authority;
  }

  public void setAuthority(final String authority) {
    this.authority = authority;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public InvitationStatus getStatus() {
    return status;
  }

  public void setStatus(final InvitationStatus status) {
    this.status = status;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(final Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(final Instant acceptedAt) {
    this.acceptedAt = acceptedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
  }

  /** Returns {@code true} if the invitation is still usable. */
  public boolean isPending() {
    return status == InvitationStatus.PENDING && Instant.now().isBefore(expiresAt);
  }
}
