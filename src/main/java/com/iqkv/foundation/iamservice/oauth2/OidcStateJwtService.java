package com.iqkv.foundation.iamservice.oauth2;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.iqkv.foundation.iamservice.infrastructure.config.AuthConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.OAuth2ConfigurationProperties;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class OidcStateJwtService {

  private final AuthConfigurationProperties authProps;
  private final OAuth2ConfigurationProperties oauth2Props;
  private final RSAPrivateKey privateKey;
  private final RSAPublicKey publicKey;

  public OidcStateJwtService(final AuthConfigurationProperties authProps,
                             final OAuth2ConfigurationProperties oauth2Props,
                             final ResourceLoader resourceLoader) {
    this.authProps = authProps;
    this.oauth2Props = oauth2Props;
    this.privateKey = loadPrivateKey(authProps.jwt().privateKeyPath(), resourceLoader);
    this.publicKey = loadPublicKey(authProps.jwt().publicKeyPath(), resourceLoader);
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

  private static RSAPublicKey loadPublicKey(final String publicKeyPath,
                                            final ResourceLoader resourceLoader) {
    try {
      final String pem;
      try (InputStream is = resourceLoader.getResource(publicKeyPath).getInputStream()) {
        pem = new String(is.readAllBytes());
      }
      final String stripped = pem
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s", "");
      final byte[] keyBytes = Base64.getDecoder().decode(stripped);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (final IOException | java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Failed to load RSA public key for JWT verification", e);
    }
  }

  public String generateState(final OidcState state) {
    final Instant now = Instant.now();
    final Instant expiry = now.plus(oauth2Props.stateTtl());

    final var builder = Jwts.builder()
        .claim(JwtClaimNames.JTI, state.jti().toString())
        .claim("provider", state.provider())
        .claim("nonce", state.nonce())
        .claim("tenantKey", state.tenantKey())
        .claim("redirectUri", state.redirectUri())
        .claim("flowType", state.flowType())
        .claim(JwtClaimNames.IAT, Date.from(now))
        .claim(JwtClaimNames.EXP, Date.from(expiry))
        .claim(JwtClaimNames.ISS, JwtClaimNames.ISSUER);

    if (state.userId() != null) {
      builder.claim("userId", state.userId().toString());
    }

    return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
  }

  public OidcState verifyState(final String stateToken) {
    final Claims claims = Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(stateToken)
        .getPayload();

    final UUID jti = UUID.fromString(claims.get(JwtClaimNames.JTI, String.class));
    final String provider = claims.get("provider", String.class);
    final String nonce = claims.get("nonce", String.class);
    final String tenantKey = claims.get("tenantKey", String.class);
    final String redirectUri = claims.get("redirectUri", String.class);
    final String flowType = claims.get("flowType", String.class);
    final UUID userId = claims.get("userId") != null ? UUID.fromString(claims.get("userId", String.class)) : null;
    final Instant expiresAt = claims.getExpiration().toInstant();

    return new OidcState(jti, provider, nonce, tenantKey, redirectUri, flowType, userId, expiresAt);
  }
}
