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

package com.iqkv.foundation.iamservice.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Provides the RSA public key as a singleton bean so that it is loaded from the PEM file exactly
 * once at startup and shared across {@link SecurityConfig} (JWT decoding) and
 * {@link com.iqkv.foundation.iamservice.infrastructure.security.JwksRestResource} (JWKS endpoint).
 *
 * <p>Also derives the stable {@code kid} (key ID) for this key pair — a lowercase hex-encoded
 * SHA-256 digest of the DER-encoded public key — so that issued JWTs and the JWKS document always
 * advertise the same identifier.
 */
@Configuration
public class RsaKeyConfig {

  private final AuthConfigurationProperties authProps;
  private final ResourceLoader resourceLoader;

  public RsaKeyConfig(final AuthConfigurationProperties authProps,
                      final ResourceLoader resourceLoader) {
    this.authProps = authProps;
    this.resourceLoader = resourceLoader;
  }

  /**
   * Loads and parses the RSA public key from the configured PEM file.
   * The returned instance is safe to share across beans.
   */
  @Bean
  public RSAPublicKey jwtPublicKey() {
    return loadPublicKey(authProps.jwt().publicKeyPath(), resourceLoader);
  }

  /**
   * Computes a stable key ID for the {@link #jwtPublicKey()} bean.
   *
   * <p>The {@code kid} is a lowercase hex-encoded SHA-256 digest of the DER-encoded SubjectPublicKeyInfo
   * bytes (i.e. the raw bytes behind the PEM). This is deterministic and changes only when the key pair
   * is rotated, making it safe to cache in downstream services.
   */
  @Bean
  public String jwtKeyId(final RSAPublicKey jwtPublicKey) {
    try {
      final byte[] der = jwtPublicKey.getEncoded(); // DER / SubjectPublicKeyInfo
      final byte[] digest = MessageDigest.getInstance("SHA-256").digest(der);
      return HexFormat.of().formatHex(digest);
    } catch (final java.security.NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the JDK spec — this can never happen in practice
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  // ---------------------------------------------------------------------------
  // Package-visible static helper — reused by RsaKeyConfig tests if needed
  // ---------------------------------------------------------------------------

  static RSAPublicKey loadPublicKey(final String publicKeyPath,
                                    final ResourceLoader resourceLoader) {
    try {
      final String pem;
      try (InputStream is = resourceLoader.getResource(publicKeyPath).getInputStream()) {
        pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
      final String stripped = pem
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s", "");
      final byte[] keyBytes = Base64.getDecoder().decode(stripped);
      return (RSAPublicKey) KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (final IOException | java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Failed to load RSA public key", e);
    }
  }
}
