package com.iqkv.foundation.iamservice.oauth2;

import java.time.Instant;
import java.util.UUID;

public record OidcState(
    UUID jti,
    String provider,
    String nonce,
    String tenantKey,
    String redirectUri,
    String flowType,
    UUID userId,
    Instant expiresAt
) {

}
