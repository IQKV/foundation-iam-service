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

package com.iqkv.foundation.iamservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthConfigurationProperties Tests")
class AuthConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create Jwt configuration")
  void shouldCreateJwtConfiguration() {
    var jwt = new AuthConfigurationProperties.Jwt(
        "/path/to/private.key",
        "/path/to/public.key",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "iam-service"
    );

    assertThat(jwt.privateKeyPath()).isEqualTo("/path/to/private.key");
    assertThat(jwt.publicKeyPath()).isEqualTo("/path/to/public.key");
    assertThat(jwt.expiry()).isEqualTo(Duration.ofMinutes(15));
    assertThat(jwt.refreshExpiry()).isEqualTo(Duration.ofDays(7));
    assertThat(jwt.issuer()).isEqualTo("iam-service");
  }

  @Test
  @DisplayName("Should create Security configuration")
  void shouldCreateSecurityConfiguration() {
    var rateLimiting = new AuthConfigurationProperties.Security.RateLimiting(
        5,
        Duration.ofMinutes(15)
    );
    var security = new AuthConfigurationProperties.Security(
        10,
        8,
        rateLimiting
    );

    assertThat(security.passwordEncoderStrength()).isEqualTo(10);
    assertThat(security.minLength()).isEqualTo(8);
    assertThat(security.rateLimiting()).isNotNull();
    assertThat(security.rateLimiting().loginAttempts()).isEqualTo(5);
    assertThat(security.rateLimiting().lockoutDuration()).isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  @DisplayName("Should create PasswordReset configuration")
  void shouldCreatePasswordResetConfiguration() {
    var passwordReset = new AuthConfigurationProperties.PasswordReset(
        Duration.ofHours(24),
        Duration.ofHours(1),
        3
    );

    assertThat(passwordReset.tokenTtl()).isEqualTo(Duration.ofHours(24));
    assertThat(passwordReset.rateLimitWindow()).isEqualTo(Duration.ofHours(1));
    assertThat(passwordReset.rateLimitMaxRequests()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should create MagicLink configuration")
  void shouldCreateMagicLinkConfiguration() {
    var magicLink = new AuthConfigurationProperties.MagicLink(
        Duration.ofHours(24),
        Duration.ofHours(1),
        3
    );

    assertThat(magicLink.tokenTtl()).isEqualTo(Duration.ofHours(24));
    assertThat(magicLink.rateLimitWindow()).isEqualTo(Duration.ofHours(1));
    assertThat(magicLink.rateLimitMaxRequests()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should create complete AuthConfigurationProperties")
  void shouldCreateCompleteAuthConfiguration() {
    var jwt = new AuthConfigurationProperties.Jwt(
        "/path/to/private.key",
        "/path/to/public.key",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        "iam-service"
    );
    var rateLimiting = new AuthConfigurationProperties.Security.RateLimiting(
        5,
        Duration.ofMinutes(15)
    );
    var security = new AuthConfigurationProperties.Security(
        10,
        8,
        rateLimiting
    );
    var passwordReset = new AuthConfigurationProperties.PasswordReset(
        Duration.ofHours(24),
        Duration.ofHours(1),
        3
    );
    var magicLink = new AuthConfigurationProperties.MagicLink(
        Duration.ofHours(24),
        Duration.ofHours(1),
        3
    );
    var config = new AuthConfigurationProperties(jwt, security, passwordReset, magicLink);

    assertThat(config.jwt()).isEqualTo(jwt);
    assertThat(config.security()).isEqualTo(security);
    assertThat(config.passwordReset()).isEqualTo(passwordReset);
    assertThat(config.magicLink()).isEqualTo(magicLink);
  }
}
