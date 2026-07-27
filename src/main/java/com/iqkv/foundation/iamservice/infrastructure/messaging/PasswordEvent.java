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
import java.util.UUID;

import com.iqkv.foundation.audit.model.event.AuditActor;
import com.iqkv.foundation.audit.model.event.AuditableEvent;

/**
 * Event published for every password mutation (reset request, reset completion, self-service change,
 * or admin-forced change).
 *
 * <p>{@code actorId} is the ID of the user who triggered the change:
 * <ul>
 *   <li>Self-service change and reset — same as {@code userId}</li>
 *   <li>Admin-forced change — the admin's user ID (different from {@code userId})</li>
 * </ul>
 *
 * <p>IP address and user-agent are populated automatically by {@link com.iqkv.foundation.audit.spi.context.AuditEventEnricher}
 * before the event is published.
 */
public class PasswordEvent implements AuditableEvent {

  public enum EventType {
    /**
     * User or admin requested a password-reset link.
     */
    PASSWORD_RESET_INITIATED,
    /**
     * Password-reset token was consumed and new password was set.
     */
    PASSWORD_RESET_COMPLETED,
    /**
     * User changed their own password while authenticated.
     */
    PASSWORD_CHANGED_SELF,
    /**
     * Platform admin forcibly set a new password for another user.
     */
    PASSWORD_CHANGED_BY_ADMIN
  }

  private UUID userId;
  private String email;
  private String tenantKey;
  private EventType eventType;
  /**
   * Null for INITIATED (actor unknown at that point) or self-service; admin ID for CHANGED_BY_ADMIN.
   */
  private UUID actorId;
  private Instant occurredAt;
  private AuditActor actor;

  public PasswordEvent() {
  }

  public PasswordEvent(final UUID userId, final String email, final String tenantKey,
                       final EventType eventType, final UUID actorId, final Instant occurredAt) {
    this.userId = userId;
    this.email = email;
    this.tenantKey = tenantKey;
    this.eventType = eventType;
    this.actorId = actorId;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(final EventType eventType) {
    this.eventType = eventType;
  }

  public UUID getActorId() {
    return actorId;
  }

  public void setActorId(final UUID actorId) {
    this.actorId = actorId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
