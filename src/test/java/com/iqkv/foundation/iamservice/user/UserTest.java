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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User Tests")
class UserTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    User user = new User();
    UUID id = UUID.randomUUID();
    String email = "test@example.com";
    String passwordHash = "hashed-password";
    String firstName = "John";
    String lastName = "Doe";
    AccountStatus status = AccountStatus.ACTIVE;
    boolean emailVerified = true;
    String locale = "en-US";
    String avatarUrl = "http://example.com/avatar.jpg";
    Instant lastGlobalSignoutAt = Instant.now();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    String createdBy = "system";
    String updatedBy = "admin";

    user.setId(id);
    user.setEmail(email);
    user.setPasswordHash(passwordHash);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setStatus(status);
    user.setEmailVerified(emailVerified);
    user.setLocale(locale);
    user.setAvatarUrl(avatarUrl);
    user.setLastGlobalSignoutAt(lastGlobalSignoutAt);
    user.setCreatedAt(createdAt);
    user.setUpdatedAt(updatedAt);
    user.setCreatedBy(createdBy);
    user.setUpdatedBy(updatedBy);

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
    assertThat(user.getFirstName()).isEqualTo(firstName);
    assertThat(user.getLastName()).isEqualTo(lastName);
    assertThat(user.getStatus()).isEqualTo(status);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getLocale()).isEqualTo(locale);
    assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
    assertThat(user.getLastGlobalSignoutAt()).isEqualTo(lastGlobalSignoutAt);
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(user.getCreatedBy()).isEqualTo(createdBy);
    assertThat(user.getUpdatedBy()).isEqualTo(updatedBy);
  }
}
