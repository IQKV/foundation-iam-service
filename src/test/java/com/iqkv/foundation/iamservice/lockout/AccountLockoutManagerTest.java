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

package com.iqkv.foundation.iamservice.lockout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.iqkv.foundation.iamservice.infrastructure.config.AuthConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockoutManager Unit Tests")
class AccountLockoutManagerTest {

  @Mock
  private FailedLoginAttemptMapper attemptMapper;
  @Mock
  private AuthConfigurationProperties authProps;

  private AccountLockoutManager lockoutManager;

  @BeforeEach
  void setUp() {
    var rateLimiting = new AuthConfigurationProperties.Security.RateLimiting(
        5,
        java.time.Duration.ofMinutes(15)
    );
    var security = new AuthConfigurationProperties.Security(
        10,
        8,
        rateLimiting
    );
    when(authProps.security()).thenReturn(security);
    lockoutManager = new AccountLockoutManager(attemptMapper, authProps);
  }

  @Test
  @DisplayName("Should record failed attempt successfully")
  void shouldRecordFailedAttemptSuccessfully() {
    // Arrange
    var email = "user@example.com";

    // Act
    lockoutManager.recordFailedAttempt(email);

    // Assert
    verify(attemptMapper).insert(any(FailedLoginAttempt.class));
  }

  @Test
  @DisplayName("Should check if account is not locked")
  void shouldCheckAccountIsNotLocked() {
    // Arrange
    var email = "user@example.com";

    when(attemptMapper.countByEmailAndAttemptedAtAfter(eq(email), any(Instant.class))).thenReturn(2L);

    // Act
    var result = lockoutManager.isLocked(email);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should check if account is locked")
  void shouldCheckAccountIsLocked() {
    // Arrange
    var email = "user@example.com";

    when(attemptMapper.countByEmailAndAttemptedAtAfter(eq(email), any(Instant.class))).thenReturn(5L);

    // Act
    var result = lockoutManager.isLocked(email);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should reset lockout successfully")
  void shouldResetLockoutSuccessfully() {
    // Arrange
    var email = "user@example.com";

    // Act
    lockoutManager.reset(email);

    // Assert
    verify(attemptMapper).deleteByEmail(email);
  }

  @Test
  @DisplayName("Should count recent attempts successfully")
  void shouldCountRecentAttemptsSuccessfully() {
    // Arrange
    var email = "user@example.com";

    when(attemptMapper.countByEmailAndAttemptedAtAfter(eq(email), any(Instant.class))).thenReturn(3L);

    // Act
    var result = lockoutManager.isLocked(email);

    // Assert
    assertThat(result).isFalse();
    verify(attemptMapper).countByEmailAndAttemptedAtAfter(eq(email), any(Instant.class));
  }

  private FailedLoginAttempt createAttempt(String email) {
    var attempt = new FailedLoginAttempt();
    attempt.setEmail(email);
    attempt.setAttemptedAt(Instant.now());
    return attempt;
  }
}
