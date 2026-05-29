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

package com.iqkv.foundation.iamservice.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only projection used by the admin user list and single-user detail queries.
 *
 * <p>Extends the core user fields with:
 * <ul>
 *   <li>{@code organizations} — aggregated tenant names the user belongs to,
 *       populated via {@code array_agg} in a single SQL query — no N+1 lookups.</li>
 *   <li>{@code membershipAuthorities} — distinct authority strings the user holds
 *       across all tenant memberships (e.g. {@code TENANT_OWNER}, {@code ADMIN},
 *       {@code MEMBER}), also aggregated in the same query.</li>
 * </ul>
 */
public class UserWithOrganizations {

  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private AccountStatus status;
  private boolean emailVerified;
  private String locale;
  private String avatarUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<String> organizations;
  private List<String> membershipAuthorities;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public void setStatus(final AccountStatus status) {
    this.status = status;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(final boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(final String locale) {
    this.locale = locale;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(final String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<String> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(final List<String> organizations) {
    this.organizations = organizations;
  }

  public List<String> getMembershipAuthorities() {
    return membershipAuthorities;
  }

  public void setMembershipAuthorities(final List<String> membershipAuthorities) {
    this.membershipAuthorities = membershipAuthorities;
  }
}
