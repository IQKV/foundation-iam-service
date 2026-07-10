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

package com.iqkv.foundation.iamservice.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shared Exceptions Tests")
class SharedExceptionsTest {

  @Test
  @DisplayName("AccountLockedException — should have expected default message")
  void accountLockedException_shouldHaveDefaultMessage() {
    final var ex = new AccountLockedException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Account temporarily locked");
  }

  @Test
  @DisplayName("AccountNotActiveException — should have expected default message")
  void accountNotActiveException_shouldHaveDefaultMessage() {
    final var ex = new AccountNotActiveException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Account is not active");
  }

  @Test
  @DisplayName("AccountBannedException — should have expected default message")
  void accountBannedException_shouldHaveDefaultMessage() {
    final var ex = new AccountBannedException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Account is banned");
  }

  @Test
  @DisplayName("AccountBannedException — should accept custom message")
  void accountBannedException_shouldAcceptCustomMessage() {
    final var ex = new AccountBannedException("Banned from tenant");
    assertThat(ex.getMessage()).isEqualTo("Banned from tenant");
  }

  @Test
  @DisplayName("InvalidTokenTypeException — should have expected default message")
  void invalidTokenTypeException_shouldHaveDefaultMessage() {
    final var ex = new InvalidTokenTypeException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Invalid token type");
  }

  @Test
  @DisplayName("TokenRevokedException — should have expected default message")
  void tokenRevokedException_shouldHaveDefaultMessage() {
    final var ex = new TokenRevokedException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Token has been revoked");
  }

  @Test
  @DisplayName("NoPlatformAuthorityException — should have expected default message")
  void noPlatformAuthorityException_shouldHaveDefaultMessage() {
    final var ex = new NoPlatformAuthorityException();
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isNotBlank();
  }

  @Test
  @DisplayName("TenantNotAvailableException — should preserve custom message")
  void tenantNotAvailableException_shouldPreserveMessage() {
    final var ex = new TenantNotAvailableException("Tenant not available");
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Tenant not available");
  }

  @Test
  @DisplayName("TenantSuspendedException — should preserve custom message")
  void tenantSuspendedException_shouldPreserveMessage() {
    final var ex = new TenantSuspendedException("Tenant suspended");
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Tenant suspended");
  }

  @Test
  @DisplayName("TenantContextMismatchException — should preserve custom message")
  void tenantContextMismatchException_shouldPreserveMessage() {
    final var ex = new TenantContextMismatchException("Tenant context mismatch");
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Tenant context mismatch");
  }

  @Test
  @DisplayName("UserNotFoundException — should preserve custom message")
  void userNotFoundException_shouldPreserveMessage() {
    final var ex = new UserNotFoundException("User not found");
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("User not found");
  }

  @Test
  @DisplayName("UserNotFoundException — should preserve cause")
  void userNotFoundException_shouldPreserveCause() {
    final var cause = new IllegalStateException("root cause");
    final var ex = new UserNotFoundException("User not found", cause);
    assertThat(ex.getCause()).isSameAs(cause);
  }

  @Test
  @DisplayName("InvalidVerificationTokenException — should preserve custom message")
  void invalidVerificationTokenException_shouldPreserveMessage() {
    final var ex = new InvalidVerificationTokenException("Token expired");
    assertThat(ex).isInstanceOf(RuntimeException.class);
    assertThat(ex.getMessage()).isEqualTo("Token expired");
  }
}
