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

package com.iqkv.iamservice.membership;

import java.util.UUID;

public class TenantMemberAuthority {

  private UUID id;
  private UUID membershipId;
  private String authority;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UUID getMembershipId() {
    return membershipId;
  }

  public void setMembershipId(final UUID membershipId) {
    this.membershipId = membershipId;
  }

  public String getAuthority() {
    return authority;
  }

  public void setAuthority(final String authority) {
    this.authority = authority;
  }
}
