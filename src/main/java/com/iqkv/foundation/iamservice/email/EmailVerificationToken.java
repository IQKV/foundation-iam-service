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

package com.iqkv.foundation.iamservice.email;

import java.time.Instant;
import java.util.UUID;

public class EmailVerificationToken {

  private UUID id;
  private UUID userId;
  private String token;
  private Instant expiresAt;
  private int resendCount = 0;
  private Instant lastResendAt;
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(final Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public int getResendCount() {
    return resendCount;
  }

  public void setResendCount(final int resendCount) {
    this.resendCount = resendCount;
  }

  public Instant getLastResendAt() {
    return lastResendAt;
  }

  public void setLastResendAt(final Instant lastResendAt) {
    this.lastResendAt = lastResendAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Instant createdAt) {
    this.createdAt = createdAt;
  }
}
