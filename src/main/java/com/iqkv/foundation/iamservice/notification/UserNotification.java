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

package com.iqkv.foundation.iamservice.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserNotification {

  private UUID id;
  private UUID targetUserId;
  private String locale;
  private String type;
  private String severity;
  private String title;
  private String message;
  private String payload; // JSONB
  private boolean isRead;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UUID getTargetUserId() {
    return targetUserId;
  }

  public void setTargetUserId(final UUID targetUserId) {
    this.targetUserId = targetUserId;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(final String locale) {
    this.locale = locale;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(final String severity) {
    this.severity = severity;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(final String payload) {
    this.payload = payload;
  }

  public boolean isRead() {
    return isRead;
  }

  public void setRead(final boolean read) {
    isRead = read;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(final LocalDateTime readAt) {
    this.readAt = readAt;
  }
}
