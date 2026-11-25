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

package com.iqkv.foundation.iamservice.team;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class TeamInvitation {

  private UUID id;
  private UUID teamId;
  private String invitedEmail;
  private UUID invitedBy;
  private TeamRole role;
  private String token;
  private TeamInvitationStatus status;
  private Instant expiresAt;
  private Instant acceptedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;

  public boolean isPending() {
    return status == TeamInvitationStatus.PENDING && Instant.now().isBefore(expiresAt);
  }

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(final UUID teamId) {
    this.teamId = teamId;
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

  public TeamRole getRole() {
    return role;
  }

  public void setRole(final TeamRole role) {
    this.role = role;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public TeamInvitationStatus getStatus() {
    return status;
  }

  public void setStatus(final TeamInvitationStatus status) {
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
}
