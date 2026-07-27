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
import java.util.Map;
import java.util.UUID;

public class NotificationEvent {

  private UUID targetUserId;
  private String recipientEmail;
  private String locale;
  private NotificationEventType type;
  private Map<String, Object> payload;
  private Instant occurredAt;

  public NotificationEvent() {
  }

  public NotificationEvent(final String recipientEmail, final String locale,
                           final NotificationEventType type, final Map<String, Object> payload,
                           final Instant occurredAt) {
    this(null, recipientEmail, locale, type, payload, occurredAt);
  }

  public NotificationEvent(final UUID targetUserId, final String recipientEmail, final String locale,
                           final NotificationEventType type, final Map<String, Object> payload,
                           final Instant occurredAt) {
    this.targetUserId = targetUserId;
    this.recipientEmail = recipientEmail;
    this.locale = locale;
    this.type = type;
    this.payload = payload;
    this.occurredAt = occurredAt;
  }

  public UUID getTargetUserId() {
    return targetUserId;
  }

  public void setTargetUserId(final UUID targetUserId) {
    this.targetUserId = targetUserId;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public void setRecipientEmail(final String recipientEmail) {
    this.recipientEmail = recipientEmail;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(final String locale) {
    this.locale = locale;
  }

  public NotificationEventType getType() {
    return type;
  }

  public void setType(final NotificationEventType type) {
    this.type = type;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(final Map<String, Object> payload) {
    this.payload = payload;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
