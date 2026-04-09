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

package com.iqkv.iamservice.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public class UserEvent {

  public enum EventType {
    USER_CREATED, USER_UPDATED, USER_DELETED
  }

  private UUID userId;
  private String tenantId;
  private String email;
  private EventType eventType;
  private Instant occurredAt;

  public UserEvent() {}

  public UserEvent(final UUID userId, final String tenantId, final String email,
                   final EventType eventType, final Instant occurredAt) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.email = email;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  public UUID getUserId() { return userId; }
  public void setUserId(final UUID userId) { this.userId = userId; }

  public String getTenantId() { return tenantId; }
  public void setTenantId(final String tenantId) { this.tenantId = tenantId; }

  public String getEmail() { return email; }
  public void setEmail(final String email) { this.email = email; }

  public EventType getEventType() { return eventType; }
  public void setEventType(final EventType eventType) { this.eventType = eventType; }

  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(final Instant occurredAt) { this.occurredAt = occurredAt; }
}
