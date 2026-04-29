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

package com.iqkv.foundation.iamservice.infrastructure.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.iqkv.foundation.iamservice.infrastructure.config.AuthConfigurationProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the RSA public key as a JSON Web Key Set (JWKS) at the standard
 * {@code /.well-known/jwks.json} endpoint so downstream services can verify
 * RS256 JWTs locally without calling back to this service.
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "JWKS", description = "JSON Web Key Set for RS256 token verification")
public class JwksRestResource {

  private final Map<String, Object> jwks;

  public JwksRestResource(final AuthConfigurationProperties authProps,
                          final ResourceLoader resourceLoader) {
    this.jwks = buildJwks(authProps.jwt().publicKeyPath(), resourceLoader);
  }

  @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get JSON Web Key Set",
             description = "Returns the RSA public key in JWKS format for local JWT verification by downstream services.")
  @ApiResponse(responseCode = "200", description = "JWKS returned")
  public ResponseEntity<Map<String, Object>> jwks() {
    return ResponseEntity.ok(jwks);
  }

  private static Map<String, Object> buildJwks(final String publicKeyPath,
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
      final RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(keyBytes));

      final String n = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(publicKey.getModulus().toByteArray());
      final String e = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(publicKey.getPublicExponent().toByteArray());

      final Map<String, Object> jwk = Map.of(
          "kty", "RSA",
          "use", "sig",
          "alg", "RS256",
          "n", n,
          "e", e
      );
      return Map.of("keys", List.of(jwk));
    } catch (final IOException | java.security.GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to build JWKS from public key", ex);
    }
  }
}
