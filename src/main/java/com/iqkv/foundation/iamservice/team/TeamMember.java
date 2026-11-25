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

import java.time.LocalDateTime;
import java.util.UUID;

public class TeamMember {

  private UUID id;
  private UUID teamId;
  private UUID userId;
  private TeamRole role;
  private TeamMemberStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

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

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public TeamRole getRole() {
    return role;
  }

  public void setRole(final TeamRole role) {
    this.role = role;
  }

  public TeamMemberStatus getStatus() {
    return status;
  }

  public void setStatus(final TeamMemberStatus status) {
    this.status = status;
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
}
