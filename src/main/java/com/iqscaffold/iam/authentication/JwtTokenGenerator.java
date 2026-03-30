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

package com.iqscaffold.iam.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.iqscaffold.iam.infrastructure.config.AuthConfigurationProperties;
import com.iqscaffold.iam.security.JwtClaimNames;
import com.iqscaffold.iam.user.User;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenGenerator {

  private final AuthConfigurationProperties authProps;
  private final RSAPrivateKey privateKey;

  public JwtTokenGenerator(final AuthConfigurationProperties authProps,
                            final ResourceLoader resourceLoader) {
    this.authProps = authProps;
    this.privateKey = loadPrivateKey(authProps.jwt().privateKeyPath(), resourceLoader);
  }

  private static RSAPrivateKey loadPrivateKey(final String privateKeyPath,
                                              final ResourceLoader resourceLoader) {
    try {
      final String pem;
      try (InputStream is = resourceLoader.getResource(privateKeyPath).getInputStream()) {
        pem = new String(is.readAllBytes());
      }
      final String stripped = pem
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replace("-----BEGIN RSA PRIVATE KEY-----", "")
          .replace("-----END RSA PRIVATE KEY-----", "")
          .replaceAll("\\s", "");
      final byte[] keyBytes = Base64.getDecoder().decode(stripped);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    } catch (final IOException | java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Failed to load RSA private key for JWT signing", e);
    }
  }

  public String generateAccessToken(final User user, final String tenantKey, final List<String> authorities) {
    final Instant now = Instant.now();
    final Instant expiry = now.plus(authProps.jwt().expiry());

    return Jwts.builder()
        .claim(JwtClaimNames.SUB, user.getEmail())
        .claim(JwtClaimNames.ISS, JwtClaimNames.ISSUER)
        .claim(JwtClaimNames.IAT, Date.from(now))
        .claim(JwtClaimNames.EXP, Date.from(expiry))
        .claim(JwtClaimNames.JTI, UUID.randomUUID().toString())
        .claim(JwtClaimNames.TYPE, JwtClaimNames.TYPE_ACCESS)
        .claim(JwtClaimNames.USER_ID, user.getId().toString())
        .claim(JwtClaimNames.USERNAME, user.getEmail())
        .claim(JwtClaimNames.EMAIL, user.getEmail())
        .claim(JwtClaimNames.FIRST_NAME, user.getFirstName())
        .claim(JwtClaimNames.LAST_NAME, user.getLastName())
        .claim(JwtClaimNames.TENANT_ID, tenantKey)
        .claim(JwtClaimNames.EMAIL_VERIFIED, user.isEmailVerified())
        .claim(JwtClaimNames.AUTHORITIES, authorities)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  public String generateRefreshToken(final User user, final String tenantKey) {
    final Instant now = Instant.now();
    final Instant expiry = now.plus(authProps.jwt().refreshExpiry());

    return Jwts.builder()
        .claim(JwtClaimNames.SUB, user.getEmail())
        .claim(JwtClaimNames.ISS, JwtClaimNames.ISSUER)
        .claim(JwtClaimNames.IAT, Date.from(now))
        .claim(JwtClaimNames.EXP, Date.from(expiry))
        .claim(JwtClaimNames.JTI, UUID.randomUUID().toString())
        .claim(JwtClaimNames.TYPE, JwtClaimNames.TYPE_REFRESH)
        .claim(JwtClaimNames.USERNAME, user.getEmail())
        .claim(JwtClaimNames.TENANT_ID, tenantKey)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }
}
