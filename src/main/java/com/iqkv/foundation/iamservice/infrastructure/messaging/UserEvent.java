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
import java.util.UUID;

import com.iqkv.foundation.audit.model.event.AuditActor;
import com.iqkv.foundation.audit.model.event.AuditableEvent;

public class UserEvent implements AuditableEvent {

  public enum EventType {
    USER_CREATED, USER_UPDATED, USER_DELETED,
    USER_REMOVED,       // membership removal
    USER_INVITED,       // invitation sent
    USER_STATUS_CHANGED, // account status changed (ACTIVE / SUSPENDED / etc.)
    USER_UNLOCKED,      // lockout cleared by admin
    USER_BANNED,        // user banned (platform or tenant scope)
    USER_UNBANNED       // ban lifted
  }

  // Populated for BAN / UNBAN / STATUS_CHANGED events — null otherwise
  private String banScope;    // "PLATFORM" | "TENANT" | null
  private String banReason;   // nullable
  private String newStatus;   // e.g. "ACTIVE", "SUSPENDED" — for USER_STATUS_CHANGED

  private UUID userId;
  private String tenantId;
  private String email;
  private EventType eventType;
  private Instant occurredAt;
  private AuditActor actor;
  private String banScope;
  private String banReason;
  private String newStatus;

  public UserEvent() {
  }

  public UserEvent(final UUID userId, final String tenantId, final String email,
                   final EventType eventType, final Instant occurredAt) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.email = email;
    this.eventType = eventType;
    this.occurredAt = occurredAt;
  }

  // -------------------------------------------------------------------------
  // AuditableEvent
  // -------------------------------------------------------------------------

  @Override
  public AuditActor getActor() {
    return actor;
  }

  @Override
  public void setActor(final AuditActor actor) {
    this.actor = actor;
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(final String tenantId) {
    this.tenantId = tenantId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
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

  public String getBanScope() {
    return banScope;
  }

  public void setBanScope(final String banScope) {
    this.banScope = banScope;
  }

  public String getBanReason() {
    return banReason;
  }

  public void setBanReason(final String banReason) {
    this.banReason = banReason;
  }

  public String getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(final String newStatus) {
    this.newStatus = newStatus;
  }
}
