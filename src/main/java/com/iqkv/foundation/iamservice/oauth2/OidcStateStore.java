package com.iqkv.foundation.iamservice.oauth2;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OidcStateStore {

  private static final Logger log = LoggerFactory.getLogger(OidcStateStore.class);
  private static final String KEY_PREFIX = "iam:oidc:state:";

  private final StringRedisTemplate stringRedisTemplate;

  public OidcStateStore(final StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void store(UUID jti, String codeVerifier, long ttlSeconds) {
    stringRedisTemplate.opsForValue().set(redisKey(jti), codeVerifier, Duration.ofSeconds(ttlSeconds));
    log.debug("Stored state with jti: {} (expires in {} seconds)", jti, ttlSeconds);
  }

  public String retrieveAndRemove(UUID jti) {
    final String codeVerifier = stringRedisTemplate.opsForValue().getAndDelete(redisKey(jti));
    if (codeVerifier == null || codeVerifier.isBlank()) {
      log.warn("State not found or expired for jti: {}", jti);
      return null;
    }
    log.debug("Retrieved and removed state with jti: {}", jti);
    return codeVerifier;
  }

  private String redisKey(final UUID jti) {
    return KEY_PREFIX + jti;
  }
}
