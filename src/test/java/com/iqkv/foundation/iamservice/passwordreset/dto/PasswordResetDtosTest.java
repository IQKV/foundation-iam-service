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

package com.iqkv.foundation.iamservice.passwordreset.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordResetDtos Tests")
class PasswordResetDtosTest {

  @Test
  @DisplayName("Should create ForgotPasswordRequest")
  void shouldCreateForgotPasswordRequest() {
    var request = new PasswordResetDtos.ForgotPasswordRequest("user@example.com");

    assertThat(request.email()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Should create ResetPasswordRequest")
  void shouldCreateResetPasswordRequest() {
    var request = new PasswordResetDtos.ResetPasswordRequest(
        "reset-token",
        "NewPass123!"
    );

    assertThat(request.token()).isEqualTo("reset-token");
    assertThat(request.newPassword()).isEqualTo("NewPass123!");
  }
}
