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

package com.iqkv.foundation.iamservice.platformnote;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A lightweight operator note scoped to the platform level.
 *
 * <p>Platform Health Notes are private to PLATFORM_ADMIN operators — they are never
 * exposed to tenants or end-users. Their purpose is to record ongoing operational
 * observations (incidents, maintenance windows, degraded services) so the ops team
 * can track platform health from within the admin console.
 */
public class PlatformNote {

  private UUID id;
  private String title;
  private String body;
  private PlatformNoteSeverity severity;
  private PlatformNoteStatus status;
  private UUID createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(final String body) {
    this.body = body;
  }

  public PlatformNoteSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(final PlatformNoteSeverity severity) {
    this.severity = severity;
  }

  public PlatformNoteStatus getStatus() {
    return status;
  }

  public void setStatus(final PlatformNoteStatus status) {
    this.status = status;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(final UUID createdBy) {
    this.createdBy = createdBy;
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
