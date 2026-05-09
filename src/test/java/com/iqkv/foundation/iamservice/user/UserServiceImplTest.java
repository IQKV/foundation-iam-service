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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.UserEventPublisher;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.signup.SignupResult;
import com.iqkv.foundation.iamservice.signup.SignupStrategy;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

  @Mock
  private UserMapper userMapper;
  @Mock
  private TenantMembershipMapper membershipMapper;
  @Mock
  private EmailVerificationTokenMapper emailVerificationTokenMapper;
  @Mock
  private MessagingService messagingService;
  @Mock
  private UserEventPublisher userEventPublisher;
  @Mock
  private NotificationConfigurationProperties notificationProps;
  @Mock
  private SignupStrategy signupStrategy;
  @Mock
  private PasswordEncoder passwordEncoder;

  private UserServiceImpl userService;

  private User testUser;
  private Tenant testTenant;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(
        userMapper,
        membershipMapper,
        emailVerificationTokenMapper,
        messagingService,
        userEventPublisher,
        notificationProps,
        signupStrategy,
        passwordEncoder
    );

    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("user@example.com");
    testUser.setPasswordHash("$2a$10$hashedPassword");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setStatus(AccountStatus.ACTIVE);
    testUser.setEmailVerified(false);
    testUser.setCreatedAt(LocalDateTime.now());

    testTenant = new Tenant();
    testTenant.setTenantKey("test-tenant");
    testTenant.setName("Test Tenant");
    testTenant.setStatus(TenantStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should register user successfully")
  void shouldRegisterUserSuccessfully() {
    // Arrange
    var request = new UserDtos.RegisterUserRequest(
        "user@example.com",
        "password123",
        "John",
        "Doe",
        "Test Tenant"
    );
    var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    var signupResult = new SignupResult(testUser, testTenant, membership, List.of("MEMBER"));

    when(signupStrategy.execute(request)).thenReturn(signupResult);
    when(notificationProps.baseUrl()).thenReturn("https://example.com");
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    var result = userService.registerUser(request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo(testUser.getId());
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");
    verify(userEventPublisher).publishUserCreated(testUser);
    verify(emailVerificationTokenMapper).insert(any());
    verify(messagingService).publishNotification(any());
  }

  @Test
  @DisplayName("Should list users with pagination successfully")
  void shouldListUsersSuccessfully() {
    // Arrange
    var user1 = createUser("user1@example.com", "User", "One");
    var user2 = createUser("user2@example.com", "User", "Two");

    when(userMapper.findAll(10, 0, "createdAt", "desc")).thenReturn(List.of(user1, user2));
    when(userMapper.countAll()).thenReturn(2L);

    // Act
    var result = userService.listUsers(0, 10, "createdAt", "desc");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(2);
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalElements()).isEqualTo(2L);
    assertThat(result.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create user by admin successfully")
  void shouldCreateUserByAdminSuccessfully() {
    // Arrange
    var request = new UserDtos.AdminCreateUserRequest(
        "newuser@example.com",
        "New",
        "User"
    );

    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
    when(userMapper.findByEmail("newuser@example.com")).thenReturn(Optional.of(testUser));

    // Act
    var result = userService.createUser(request);

    // Assert
    assertThat(result).isNotNull();
    verify(userMapper).upsertByEmail(any(User.class));
  }

  @Test
  @DisplayName("Should get user by ID successfully")
  void shouldGetUserByIdSuccessfully() {
    // Arrange
    var userId = testUser.getId();

    when(userMapper.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    var result = userService.getUserById(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.firstName()).isEqualTo("John");
    assertThat(result.lastName()).isEqualTo("Doe");
  }

  @Test
  @DisplayName("Should update user successfully")
  void shouldUpdateUserSuccessfully() {
    // Arrange
    var userId = testUser.getId();
    var firstName = "Jane";
    var lastName = "Smith";
    var updatedBy = "admin";

    when(userMapper.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    var result = userService.updateUser(userId, firstName, lastName, updatedBy);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.firstName()).isEqualTo("Jane");
    assertThat(result.lastName()).isEqualTo("Smith");
    verify(userMapper).update(any(User.class));
  }

  @Test
  @DisplayName("Should patch user successfully")
  void shouldPatchUserSuccessfully() {
    // Arrange
    var userId = testUser.getId();
    var request = new UserDtos.AdminUpdateUserRequest("Jane", "Smith", "ACTIVE");

    when(userMapper.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    var result = userService.patchUser(userId, request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.firstName()).isEqualTo("Jane");
    assertThat(result.lastName()).isEqualTo("Smith");
    verify(userMapper).update(any(User.class));
  }

  @Test
  @DisplayName("Should delete user by ID successfully")
  void shouldDeleteUserByIdSuccessfully() {
    // Arrange
    var userId = testUser.getId();

    when(userMapper.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    userService.deleteUserById(userId);

    // Assert
    verify(userMapper).deleteById(userId);
  }

  @Test
  @DisplayName("Should delete user membership successfully")
  void shouldDeleteUserMembershipSuccessfully() {
    // Arrange
    var userId = testUser.getId();
    var tenantKey = "test-tenant";
    var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    membership.setUserId(userId);
    membership.setTenantKey(tenantKey);

    when(membershipMapper.findByUserIdAndTenantKey(userId, tenantKey))
        .thenReturn(Optional.of(membership));
    when(userMapper.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    userService.deleteUser(userId, tenantKey);

    // Assert
    verify(membershipMapper).deleteById(membership.getId());
    verify(userEventPublisher).publishUserRemoved(testUser, tenantKey);
  }

  private User createUser(String email, String firstName, String lastName) {
    var user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setStatus(AccountStatus.ACTIVE);
    user.setEmailVerified(true);
    user.setCreatedAt(LocalDateTime.now());
    return user;
  }
}
