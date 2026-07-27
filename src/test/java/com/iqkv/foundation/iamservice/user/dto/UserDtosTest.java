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

package com.iqkv.foundation.iamservice.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserDtos Tests")
class UserDtosTest {

  @Test
  @DisplayName("Should create RegisterUserRequest")
  void shouldCreateRegisterUserRequest() {
    var request = new UserDtos.RegisterUserRequest(
        "user@example.com",
        "ChangeMePass123!",
        "John",
        "Doe",
        "Test Tenant"
    );

    assertThat(request.email()).isEqualTo("user@example.com");
    assertThat(request.password()).isEqualTo("ChangeMePass123!");
    assertThat(request.firstName()).isEqualTo("John");
    assertThat(request.lastName()).isEqualTo("Doe");
    assertThat(request.tenantName()).isEqualTo("Test Tenant");
  }

  @Test
  @DisplayName("Should create UpdateUserRequest")
  void shouldCreateUpdateUserRequest() {
    var request = new UserDtos.UpdateUserRequest("Jane", "Smith", null);

    assertThat(request.firstName()).isEqualTo("Jane");
    assertThat(request.lastName()).isEqualTo("Smith");
  }

  @Test
  @DisplayName("Should create UserResponse")
  void shouldCreateUserResponse() {
    var userId = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var updatedAt = LocalDateTime.now();
    var response = new UserDtos.UserResponse(
        userId,
        "user@example.com",
        "John",
        "Doe",
        "ACTIVE",
        true,
        "en-US",
        null, // avatarUrl
        null, // firstSignInAt
        false, // onboardingCompleted
        true,  // profileCompleted
        List.of("Acme Corp"),
        List.of("MEMBER"),
        createdAt,
        updatedAt
    );

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.firstName()).isEqualTo("John");
    assertThat(response.lastName()).isEqualTo("Doe");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.emailVerified()).isTrue();
    assertThat(response.organizations()).containsExactly("Acme Corp");
    assertThat(response.membershipAuthorities()).containsExactly("MEMBER");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should create SignupResponse")
  void shouldCreateSignupResponse() {
    var userId = UUID.randomUUID();
    var response = new UserDtos.SignupResponse(
        userId,
        "user@example.com",
        "tenant-key",
        "ACTIVE"
    );

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.tenantKey()).isEqualTo("tenant-key");
    assertThat(response.tenantStatus()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("Should create AdminCreateUserRequest")
  void shouldCreateAdminCreateUserRequest() {
    var request = new UserDtos.AdminCreateUserRequest(
        "admin@example.com",
        "Admin",
        "User",
        null
    );

    assertThat(request.email()).isEqualTo("admin@example.com");
    assertThat(request.firstName()).isEqualTo("Admin");
    assertThat(request.lastName()).isEqualTo("User");
  }

  @Test
  @DisplayName("Should create AdminUpdateUserRequest")
  void shouldCreateAdminUpdateUserRequest() {
    var request = new UserDtos.AdminUpdateUserRequest(
        "Updated",
        "Name",
        "SUSPENDED",
        null
    );

    assertThat(request.firstName()).isEqualTo("Updated");
    assertThat(request.lastName()).isEqualTo("Name");
    assertThat(request.status()).isEqualTo("SUSPENDED");
  }

  @Test
  @DisplayName("Should create PagedUserResponse")
  void shouldCreatePagedUserResponse() {
    var userId = UUID.randomUUID();
    var userResponse = new UserDtos.UserResponse(
        userId,
        "user@example.com",
        "John",
        "Doe",
        "ACTIVE",
        true,
        null,
        null, // avatarUrl
        null, // firstSignInAt
        false, // onboardingCompleted
        true,  // profileCompleted
        List.of(),
        List.of(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );
    var content = List.of(userResponse);
    var response = new UserDtos.PagedUserResponse(content, 0, 10, 1L, 1);

    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(10);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.totalPages()).isEqualTo(1);
  }
}
