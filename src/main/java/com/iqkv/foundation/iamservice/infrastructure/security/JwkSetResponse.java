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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Typed representation of a JSON Web Key Set (JWKS) document as defined by
 * <a href="https://www.rfc-editor.org/rfc/rfc7517">RFC 7517</a>.
 *
 * <p>Using a typed record instead of a raw {@code Map<String, Object>} makes the
 * response contract explicit and prevents accidental omission of required fields.
 *
 * @param keys the list of JSON Web Keys in this set
 */
public record JwkSetResponse(
    @JsonProperty("keys") List<Jwk> keys
) {

  /**
   * A single RSA public key in JWK format (RFC 7517 §4, RFC 7518 §6.3).
   *
   * @param kty key type — always {@code "RSA"} for RS256
   * @param use public-key use — {@code "sig"} indicates this key is used for signature verification
   * @param alg algorithm — {@code "RS256"}
   * @param kid key ID — SHA-256 hex fingerprint of the DER-encoded public key; used by consumers
   *            to select the correct key when verifying a JWT whose header carries the same {@code kid}
   * @param n   base64url-encoded RSA modulus (unsigned, no leading zero byte)
   * @param e   base64url-encoded RSA public exponent
   */
  public record Jwk(
      @JsonProperty("kty") String kty,
      @JsonProperty("use") String use,
      @JsonProperty("alg") String alg,
      @JsonProperty("kid") String kid,
      @JsonProperty("n") String n,
      @JsonProperty("e") String e
  ) {
  }
}
