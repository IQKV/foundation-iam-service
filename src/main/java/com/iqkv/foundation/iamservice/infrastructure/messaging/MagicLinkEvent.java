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

/**
 * Event published for magic link authentication actions.
 */
public class MagicLinkEvent implements AuditableEvent {

  public enum EventType {
    /**
     * User requested a magic link (initiate or resend).
     */
    MAGIC_LINK_INITIATED,
    /**
     * Magic link token was successfully exchanged for authentication tokens.
     */
    MAGIC_LINK_EXCHANGED
  }

  private UUID userId;
  private String email;
  private String tenantKey;
  private EventType eventType;
  private UUID actorId;
  private Instant occurredAt;
  private AuditActor actor;

  public MagicLinkEvent() {
  }

  public MagicLinkEvent(final UUID userId, final String email, final String tenantKey,
                        final EventType eventType, final UUID actorId, final Instant occurredAt) {
    this.userId = userId;
    this.email = email;
    this.tenantKey = tenantKey;
    this.eventType = eventType;
    this.actorId = actorId;
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
