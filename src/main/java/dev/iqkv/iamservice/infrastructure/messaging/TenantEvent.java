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

package dev.iqkv.iamservice.infrastructure.messaging;

import java.time.Instant;

public class TenantEvent {

  public enum EventType {
    TENANT_CREATED, TENANT_UPDATED, TENANT_DELETED
  }

  private String tenantKey;
  private String tenantName;
  private EventType eventType;
  private Instant occurredAt;

  public TenantEvent() {}

  public TenantEvent(final String tenantKey, final String tenantName,
                     final EventType eventType, final Instant occurredAt) {
    this.tenantKey = tenantKey;
    this.tenantName = tenantName;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  public String getTenantKey() { return tenantKey; }
  public void setTenantKey(final String tenantKey) { this.tenantKey = tenantKey; }

  public String getTenantName() { return tenantName; }
  public void setTenantName(final String tenantName) { this.tenantName = tenantName; }

  public EventType getEventType() { return eventType; }
  public void setEventType(final EventType eventType) { this.eventType = eventType; }

  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(final Instant occurredAt) { this.occurredAt = occurredAt; }
}
