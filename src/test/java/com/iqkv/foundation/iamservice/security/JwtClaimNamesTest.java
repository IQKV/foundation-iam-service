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

package com.iqkv.foundation.iamservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtClaimNames Tests")
class JwtClaimNamesTest {

  @Test
  @DisplayName("Standard JWT claim names should have expected values")
  void shouldHaveCorrectStandardClaimNames() {
    assertThat(JwtClaimNames.SUB).isEqualTo("sub");
    assertThat(JwtClaimNames.ISS).isEqualTo("iss");
    assertThat(JwtClaimNames.IAT).isEqualTo("iat");
    assertThat(JwtClaimNames.EXP).isEqualTo("exp");
    assertThat(JwtClaimNames.JTI).isEqualTo("jti");
  }

  @Test
  @DisplayName("Custom claim names should have expected values")
  void shouldHaveCorrectCustomClaimNames() {
    assertThat(JwtClaimNames.TYPE).isEqualTo("type");
    assertThat(JwtClaimNames.USER_ID).isEqualTo("user_id");
    assertThat(JwtClaimNames.EMAIL).isEqualTo("email");
    assertThat(JwtClaimNames.FIRST_NAME).isEqualTo("first_name");
    assertThat(JwtClaimNames.LAST_NAME).isEqualTo("last_name");
    assertThat(JwtClaimNames.TENANT_ID).isEqualTo("tenant_id");
    assertThat(JwtClaimNames.AUTHORITIES).isEqualTo("authorities");
    assertThat(JwtClaimNames.EMAIL_VERIFIED).isEqualTo("email_verified");
    assertThat(JwtClaimNames.PLAN_CODE).isEqualTo("plan_code");
    assertThat(JwtClaimNames.ONBOARDING_COMPLETED).isEqualTo("onboarding_completed");
    assertThat(JwtClaimNames.PROFILE_COMPLETED).isEqualTo("profile_completed");
  }

  @Test
  @DisplayName("Token type constants should have expected values")
  void shouldHaveCorrectTokenTypeConstants() {
    assertThat(JwtClaimNames.TYPE_ACCESS).isEqualTo("access");
    assertThat(JwtClaimNames.TYPE_REFRESH).isEqualTo("refresh");
  }

  @Test
  @DisplayName("Access and refresh type constants should be distinct")
  void tokenTypeConstantsShouldBeDistinct() {
    assertThat(JwtClaimNames.TYPE_ACCESS).isNotEqualTo(JwtClaimNames.TYPE_REFRESH);
  }

  @Test
  @DisplayName("Issuer constant should have expected value")
  void shouldHaveCorrectIssuer() {
    assertThat(JwtClaimNames.ISSUER).isEqualTo("foundation-iam-service");
  }
}
