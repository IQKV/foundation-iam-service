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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import java.time.Instant;

import com.iqkv.foundation.audit.model.event.AuditActor;
import com.iqkv.foundation.audit.model.event.AuditableEvent;

public class TenantEvent implements AuditableEvent {

  public enum EventType {
    TENANT_CREATED,
    TENANT_PROVISIONED,
    TENANT_PROVISIONING_FAILED,
    TENANT_UPDATED,
    TENANT_DELETED,
    TENANT_SUSPENDED
  }

  private String tenantKey;
  private String tenantName;
  private String ownerEmail;
  private String ownerFirstName;
  private EventType eventType;
  private Instant occurredAt;
  private AuditActor actor;

  public TenantEvent() {
  }

  public TenantEvent(final String tenantKey, final String tenantName,
                     final String ownerEmail, final String ownerFirstName,
                     final EventType eventType, final Instant occurredAt) {
    this.tenantKey = tenantKey;
    this.tenantName = tenantName;
    this.ownerEmail = ownerEmail;
    this.ownerFirstName = ownerFirstName;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  @Override
  public AuditActor getActor() {
    return actor;
  }

  @Override
  public void setActor(final AuditActor actor) {
    this.actor = actor;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(final String tenantName) {
    this.tenantName = tenantName;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(final String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public String getOwnerFirstName() {
    return ownerFirstName;
  }

  public void setOwnerFirstName(final String ownerFirstName) {
    this.ownerFirstName = ownerFirstName;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(final EventType eventType) {
    this.eventType = eventType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
