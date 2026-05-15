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

package com.iqkv.foundation.iamservice.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper;
import com.iqkv.foundation.iamservice.shared.exception.NoPlatformAuthorityException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformAdminAccountService Unit Tests")
class PlatformAdminAccountServiceImplTest {

  @Mock
  private UserMapper userMapper;
  @Mock
  private PlatformAuthorityMapper platformAuthorityMapper;

  private PlatformAdminAccountServiceImpl service;

  private User platformAdmin;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new PlatformAdminAccountServiceImpl(userMapper, platformAuthorityMapper);
    platformAdmin = new User();
    platformAdmin.setId(userId);
    platformAdmin.setEmail("admin@example.com");
    platformAdmin.setFirstName("Platform");
    platformAdmin.setLastName("Admin");
    platformAdmin.setStatus(AccountStatus.ACTIVE);
    platformAdmin.setEmailVerified(true);
    platformAdmin.setCreatedAt(LocalDateTime.now());
    platformAdmin.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should return platform admin account without tenant fields")
  void shouldGetAccountSuccessfully() {
    when(userMapper.findById(userId)).thenReturn(Optional.of(platformAdmin));
    when(platformAuthorityMapper.findAuthorityValuesByUserId(userId))
        .thenReturn(List.of("PLATFORM_ADMIN"));

    final var result = service.getAccount(userId);

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo("admin@example.com");
    assertThat(result.platformAuthorities()).containsExactly("PLATFORM_ADMIN");
  }

  @Test
  @DisplayName("Should reject account lookup when user has no platform authorities")
  void shouldRejectGetAccountWithoutPlatformAuthorities() {
    when(userMapper.findById(userId)).thenReturn(Optional.of(platformAdmin));
    when(platformAuthorityMapper.findAuthorityValuesByUserId(userId)).thenReturn(List.of());

    assertThatThrownBy(() -> service.getAccount(userId))
        .isInstanceOf(NoPlatformAuthorityException.class);
  }

  @Test
  @DisplayName("Should update platform admin name")
  void shouldUpdateAccountSuccessfully() {
    when(userMapper.findById(userId)).thenReturn(Optional.of(platformAdmin));
    when(platformAuthorityMapper.findAuthorityValuesByUserId(userId))
        .thenReturn(List.of("PLATFORM_ADMIN"));

    final var request = new PlatformAdminDtos.AdminUpdateAccountRequest("New", "Name");
    final var result = service.updateAccount(userId, request);

    assertThat(result.firstName()).isEqualTo("New");
    assertThat(result.lastName()).isEqualTo("Name");
    verify(userMapper).update(any(User.class));
  }

  @Test
  @DisplayName("Should throw when user not found")
  void shouldThrowWhenUserNotFound() {
    when(userMapper.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getAccount(userId))
        .isInstanceOf(UserNotFoundException.class);
  }
}
