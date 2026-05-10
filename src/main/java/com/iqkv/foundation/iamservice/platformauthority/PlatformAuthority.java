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

package com.iqkv.foundation.iamservice.platformauthority;

import java.time.Instant;
import java.util.UUID;

/**
 * A platform-level authority assigned directly to a user, independent of any tenant membership.
 * Used for authortities such as {@code PLATFORM_ADMIN} that span the entire platform.
 */
public class PlatformAuthority {

  private UUID id;
  private UUID userId;
  private String authority;
  private Instant grantedAt;
  private String grantedBy;

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

  public String getRole() {
    return authority;
  }

  public void setRole(final String authority) {
    this.authority = authority;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(final Instant grantedAt) {
    this.grantedAt = grantedAt;
  }

  public String getGrantedBy() {
    return grantedBy;
  }

  public void setGrantedBy(final String grantedBy) {
    this.grantedBy = grantedBy;
  }
}
