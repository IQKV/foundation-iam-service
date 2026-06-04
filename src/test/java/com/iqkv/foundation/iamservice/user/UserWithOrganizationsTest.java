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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserWithOrganizations Tests")
class UserWithOrganizationsTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    UserWithOrganizations user = new UserWithOrganizations();
    UUID id = UUID.randomUUID();
    String email = "test@example.com";
    String firstName = "John";
    String lastName = "Doe";
    AccountStatus status = AccountStatus.ACTIVE;
    boolean emailVerified = true;
    String locale = "en-US";
    String avatarUrl = "http://example.com/avatar.jpg";
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    List<String> organizations = List.of("Test Tenant");
    List<String> membershipAuthorities = List.of("MEMBER");

    user.setId(id);
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setStatus(status);
    user.setEmailVerified(emailVerified);
    user.setLocale(locale);
    user.setAvatarUrl(avatarUrl);
    user.setCreatedAt(createdAt);
    user.setUpdatedAt(updatedAt);
    user.setOrganizations(organizations);
    user.setMembershipAuthorities(membershipAuthorities);

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getFirstName()).isEqualTo(firstName);
    assertThat(user.getLastName()).isEqualTo(lastName);
    assertThat(user.getStatus()).isEqualTo(status);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getLocale()).isEqualTo(locale);
    assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(user.getOrganizations()).isEqualTo(organizations);
    assertThat(user.getMembershipAuthorities()).isEqualTo(membershipAuthorities);
  }
}
