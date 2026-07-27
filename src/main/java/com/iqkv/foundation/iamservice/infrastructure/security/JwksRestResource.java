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

package com.iqkv.foundation.iamservice.infrastructure.security;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the RSA public key as a JSON Web Key Set (JWKS) at the standard
 * {@code /.well-known/jwks.json} endpoint so downstream services can verify
 * RS256 JWTs locally without calling back to this service.
 *
 * <p>The JWK Set is built once at construction time from the shared {@link RSAPublicKey} bean
 * provided by {@link com.iqkv.foundation.iamservice.infrastructure.config.RsaKeyConfig}.
 * This avoids repeated PEM parsing and ensures the {@code kid} in the JWKS document always
 * matches the {@code kid} stamped into issued JWTs.
 *
 * <p>The response carries a {@code Cache-Control: public, max-age=3600} header so that downstream
 * services and intermediary caches can reuse the key set for up to one hour without re-fetching.
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "JWKS", description = "JSON Web Key Set for RS256 token verification")
public class JwksRestResource {

  /**
   * Cache the key set for 1 hour; the value changes only on key rotation.
   */
  private static final CacheControl JWKS_CACHE = CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic();

  private final JwkSetResponse jwkSetResponse;

  public JwksRestResource(final RSAPublicKey jwtPublicKey, final String jwtKeyId) {
    this.jwkSetResponse = buildJwkSet(jwtPublicKey, jwtKeyId);
  }

  @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Get JSON Web Key Set",
      description = "Returns the RSA public key in JWKS format for local JWT verification by downstream services.")
  @ApiResponse(responseCode = "200", description = "JWKS returned")
  public ResponseEntity<JwkSetResponse> jwks() {
    return ResponseEntity.ok()
        .cacheControl(JWKS_CACHE)
        .body(jwkSetResponse);
  }

  private static JwkSetResponse buildJwkSet(final RSAPublicKey publicKey, final String kid) {
    final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    // BigInteger.toByteArray() may prepend a 0x00 sign byte when the high bit is set.
    // Strip it so that the base64url-encoded modulus is the canonical unsigned representation
    // expected by RFC 7518 §6.3.1.2 and consumed correctly by all standard JWKS parsers.
    byte[] modulusBytes = publicKey.getModulus().toByteArray();
    if (modulusBytes.length > 1 && modulusBytes[0] == 0x00) {
      modulusBytes = Arrays.copyOfRange(modulusBytes, 1, modulusBytes.length);
    }

    final String n = urlEncoder.encodeToString(modulusBytes);
    final String e = urlEncoder.encodeToString(publicKey.getPublicExponent().toByteArray());

    final JwkSetResponse.Jwk jwk = new JwkSetResponse.Jwk("RSA", "sig", "RS256", kid, n, e);
    return new JwkSetResponse(List.of(jwk));
  }
}
