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

package com.iqkv.foundation.iamservice.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OidcIdentity Tests")
class OidcIdentityTest {

  // -------------------------------------------------------------------------
  // fromOidcClaims
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("fromOidcClaims — should map all standard OIDC claims")
  void fromOidcClaims_shouldMapAllClaims() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "google-sub-123");
    claims.put("email", "jane@example.com");
    claims.put("email_verified", true);
    claims.put("given_name", "Jane");
    claims.put("family_name", "Doe");
    claims.put("picture", "https://cdn.example.com/avatar.jpg");

    final var identity = OidcIdentity.fromOidcClaims("google", claims, "raw-id-token");

    assertThat(identity.provider()).isEqualTo("google");
    assertThat(identity.providerSub()).isEqualTo("google-sub-123");
    assertThat(identity.email()).isEqualTo("jane@example.com");
    assertThat(identity.emailVerified()).isTrue();
    assertThat(identity.firstName()).isEqualTo("Jane");
    assertThat(identity.lastName()).isEqualTo("Doe");
    assertThat(identity.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.jpg");
    assertThat(identity.rawIdToken()).isEqualTo("raw-id-token");
  }

  @Test
  @DisplayName("fromOidcClaims — should split full name when given_name and family_name absent")
  void fromOidcClaims_shouldSplitFullNameWhenGivenAndFamilyAbsent() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "sub-456");
    claims.put("name", "John Smith");

    final var identity = OidcIdentity.fromOidcClaims("microsoft", claims, null);

    assertThat(identity.firstName()).isEqualTo("John");
    assertThat(identity.lastName()).isEqualTo("Smith");
  }

  @Test
  @DisplayName("fromOidcClaims — should use given_name over name-split when both present")
  void fromOidcClaims_shouldPreferGivenNameOverNameSplit() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "sub-789");
    claims.put("name", "Robert Smith");
    claims.put("given_name", "Bob");
    claims.put("family_name", "Smith");

    final var identity = OidcIdentity.fromOidcClaims("google", claims, null);

    assertThat(identity.firstName()).isEqualTo("Bob");
    assertThat(identity.lastName()).isEqualTo("Smith");
  }

  @Test
  @DisplayName("fromOidcClaims — should return null last name when full name is single word")
  void fromOidcClaims_shouldReturnNullLastNameForSingleWordName() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "sub-000");
    claims.put("name", "Cher");

    final var identity = OidcIdentity.fromOidcClaims("google", claims, null);

    assertThat(identity.firstName()).isEqualTo("Cher");
    assertThat(identity.lastName()).isNull();
  }

  @Test
  @DisplayName("fromOidcClaims — should handle email_verified as string 'true'")
  void fromOidcClaims_shouldHandleEmailVerifiedAsString() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "sub-str");
    claims.put("email_verified", "true");

    final var identity = OidcIdentity.fromOidcClaims("google", claims, null);

    assertThat(identity.emailVerified()).isTrue();
  }

  @Test
  @DisplayName("fromOidcClaims — should fall back to avatar_url when picture absent")
  void fromOidcClaims_shouldFallBackToAvatarUrl() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "sub-av");
    claims.put("avatar_url", "https://example.com/avatar.png");

    final var identity = OidcIdentity.fromOidcClaims("custom", claims, null);

    assertThat(identity.avatarUrl()).isEqualTo("https://example.com/avatar.png");
  }

  @Test
  @DisplayName("fromOidcClaims — should throw OidcProvisioningException when sub is missing")
  void fromOidcClaims_shouldThrowWhenSubMissing() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("email", "user@example.com");

    assertThatThrownBy(() -> OidcIdentity.fromOidcClaims("google", claims, null))
        .isInstanceOf(OidcProvisioningException.class)
        .hasMessageContaining("sub");
  }

  // -------------------------------------------------------------------------
  // fromGitHubClaims
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("fromGitHubClaims — should map all GitHub-specific claims")
  void fromGitHubClaims_shouldMapAllClaims() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("id", "gh-12345");
    claims.put("name", "Alice Wonderland");
    claims.put("avatar_url", "https://avatars.githubusercontent.com/u/12345");

    final var identity = OidcIdentity.fromGitHubClaims(claims, "alice@example.com", "gh-token");

    assertThat(identity.provider()).isEqualTo("github");
    assertThat(identity.providerSub()).isEqualTo("gh-12345");
    assertThat(identity.email()).isEqualTo("alice@example.com");
    assertThat(identity.emailVerified()).isTrue();
    assertThat(identity.firstName()).isEqualTo("Alice");
    assertThat(identity.lastName()).isEqualTo("Wonderland");
    assertThat(identity.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");
    assertThat(identity.rawIdToken()).isEqualTo("gh-token");
  }

  @Test
  @DisplayName("fromGitHubClaims — should use login as name fallback when name absent")
  void fromGitHubClaims_shouldUseLoginAsFallback() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("id", "gh-99");
    claims.put("login", "codercat");

    final var identity = OidcIdentity.fromGitHubClaims(claims, "cat@example.com", null);

    assertThat(identity.firstName()).isEqualTo("codercat");
    assertThat(identity.lastName()).isNull();
  }

  @Test
  @DisplayName("fromGitHubClaims — should mark emailVerified false when email is null")
  void fromGitHubClaims_shouldMarkEmailVerifiedFalseWhenEmailNull() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("id", "gh-77");

    final var identity = OidcIdentity.fromGitHubClaims(claims, null, null);

    assertThat(identity.emailVerified()).isFalse();
  }

  @Test
  @DisplayName("fromGitHubClaims — should mark emailVerified false when email is blank")
  void fromGitHubClaims_shouldMarkEmailVerifiedFalseWhenEmailBlank() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("id", "gh-88");

    final var identity = OidcIdentity.fromGitHubClaims(claims, "  ", null);

    assertThat(identity.emailVerified()).isFalse();
  }

  @Test
  @DisplayName("fromGitHubClaims — should throw OidcProvisioningException when id is missing")
  void fromGitHubClaims_shouldThrowWhenIdMissing() {
    final Map<String, Object> claims = new HashMap<>();
    claims.put("login", "someuser");

    assertThatThrownBy(() -> OidcIdentity.fromGitHubClaims(claims, "user@example.com", null))
        .isInstanceOf(OidcProvisioningException.class)
        .hasMessageContaining("id");
  }

  // -------------------------------------------------------------------------
  // Record equality
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should be equal when all fields match")
  void shouldBeEqualWhenAllFieldsMatch() {
    final var a = new OidcIdentity("google", "sub-1", "a@b.com", true, "A", "B", null, "tok");
    final var b = new OidcIdentity("google", "sub-1", "a@b.com", true, "A", "B", null, "tok");

    assertThat(a).isEqualTo(b);
  }
}
