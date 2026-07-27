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

package com.iqkv.foundation.iamservice.user;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {

  private UUID id;
  private String email;
  private String passwordHash;
  private String firstName;
  private String lastName;
  private AccountStatus status;
  private boolean emailVerified = false;
  private String locale;
  private String avatarUrl;
  private Instant lastGlobalSignoutAt;
  private Instant firstSignInAt;
  private boolean onboardingCompleted = false;
  private boolean profileCompleted = false;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;

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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(final String passwordHash) {
    this.passwordHash = passwordHash;
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

  public Instant getLastGlobalSignoutAt() {
    return lastGlobalSignoutAt;
  }

  public void setLastGlobalSignoutAt(final Instant lastGlobalSignoutAt) {
    this.lastGlobalSignoutAt = lastGlobalSignoutAt;
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

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getFirstSignInAt() {
    return firstSignInAt;
  }

  public void setFirstSignInAt(final Instant firstSignInAt) {
    this.firstSignInAt = firstSignInAt;
  }

  public boolean isOnboardingCompleted() {
    return onboardingCompleted;
  }

  public void setOnboardingCompleted(final boolean onboardingCompleted) {
    this.onboardingCompleted = onboardingCompleted;
  }

  public boolean isProfileCompleted() {
    return profileCompleted;
  }

  public void setProfileCompleted(final boolean profileCompleted) {
    this.profileCompleted = profileCompleted;
  }
}
