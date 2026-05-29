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
 * Event published for every signin attempt (successful or failed).
 * This enables comprehensive audit logging of authentication activities.
 */
public class SigninAttemptEvent implements AuditableEvent {

  public enum AttemptResult {
    SUCCESS,
    FAILURE
  }

  public enum FailureReason {
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_NOT_ACTIVE,
    TENANT_SUSPENDED,
    TENANT_NOT_AVAILABLE,
    EMAIL_NOT_VERIFIED,
    UNKNOWN
  }

  private String email;
  private UUID userId;
  private String tenantKey;
  private AttemptResult result;
  private FailureReason failureReason;
  private String ipAddress;
  private String userAgent;
  private Instant occurredAt;
  private AuditActor actor;

  public SigninAttemptEvent() {
  }

  public SigninAttemptEvent(final String email, final UUID userId, final String tenantKey,
                           final AttemptResult result, final FailureReason failureReason,
                           final String ipAddress, final String userAgent, final Instant occurredAt) {
    this.email = email;
    this.userId = userId;
    this.tenantKey = tenantKey;
    this.result = result;
    this.failureReason = failureReason;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public String getTenantKey() {
    return tenantKey;
  }

  public void setTenantKey(final String tenantKey) {
    this.tenantKey = tenantKey;
  }

  public AttemptResult getResult() {
    return result;
  }

  public void setResult(final AttemptResult result) {
    this.result = result;
  }

  public FailureReason getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(final FailureReason failureReason) {
    this.failureReason = failureReason;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(final String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(final String userAgent) {
    this.userAgent = userAgent;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  @Override
  public String toString() {
    return "SigninAttemptEvent{" +
        "email='" + email + '\'' +
        ", userId=" + userId +
        ", tenantKey='" + tenantKey + '\'' +
        ", result=" + result +
        ", failureReason=" + failureReason +
        ", ipAddress='" + ipAddress + '\'' +
        ", userAgent='" + userAgent + '\'' +
        ", occurredAt=" + occurredAt +
        '}';
  }
}