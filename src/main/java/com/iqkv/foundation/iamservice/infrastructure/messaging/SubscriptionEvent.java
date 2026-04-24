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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import java.time.Instant;

public class SubscriptionEvent {

  public enum EventType {
    SUBSCRIPTION_CANCELLED
  }

  private String tenantKey;
  private String externalSubscriptionId;
  private EventType eventType;
  private Instant occurredAt;

  public SubscriptionEvent() {}

  public SubscriptionEvent(final String tenantKey, final String externalSubscriptionId,
                            final EventType eventType, final Instant occurredAt) {
    this.tenantKey = tenantKey;
    this.externalSubscriptionId = externalSubscriptionId;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  public String getTenantKey() { return tenantKey; }
  public void setTenantKey(final String tenantKey) { this.tenantKey = tenantKey; }

  public String getExternalSubscriptionId() { return externalSubscriptionId; }
  public void setExternalSubscriptionId(final String externalSubscriptionId) {
    this.externalSubscriptionId = externalSubscriptionId;
  }

  public EventType getEventType() { return eventType; }
  public void setEventType(final EventType eventType) { this.eventType = eventType; }

  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(final Instant occurredAt) { this.occurredAt = occurredAt; }
}
