package com.iqkv.foundation.iamservice.oauth2;

import java.util.Map;

public record OidcIdentity(
    String provider,
    String providerSub,
    String email,
    boolean emailVerified,
    String firstName,
    String lastName,
    String avatarUrl,
    String rawIdToken
) {

  public static OidcIdentity fromOidcClaims(final String provider,
                                            final Map<String, Object> claims,
                                            final String rawIdToken) {
    final String fullName = stringClaim(claims, "name");
    final String givenName = firstNonBlank(stringClaim(claims, "given_name"), firstNameFrom(fullName));
    final String familyName = firstNonBlank(stringClaim(claims, "family_name"), lastNameFrom(fullName));
    return new OidcIdentity(
        provider,
        requiredClaim(claims, "sub"),
        stringClaim(claims, "email"),
        booleanClaim(claims, "email_verified"),
        givenName,
        familyName,
        firstNonBlank(stringClaim(claims, "picture"), stringClaim(claims, "avatar_url")),
        rawIdToken
    );
  }

  public static OidcIdentity fromGitHubClaims(final Map<String, Object> claims,
                                              final String verifiedEmail,
                                              final String rawIdToken) {
    final String fullName = firstNonBlank(stringClaim(claims, "name"), stringClaim(claims, "login"));
    return new OidcIdentity(
        "github",
        requiredClaim(claims, "id"),
        verifiedEmail,
        verifiedEmail != null && !verifiedEmail.isBlank(),
        firstNameFrom(fullName),
        lastNameFrom(fullName),
        stringClaim(claims, "avatar_url"),
        rawIdToken
    );
  }

  private static String requiredClaim(final Map<String, Object> claims, final String key) {
    final String value = stringClaim(claims, key);
    if (value == null || value.isBlank()) {
      throw new OidcProvisioningException("Missing required identity claim: " + key);
    }
    return value;
  }

  private static String stringClaim(final Map<String, Object> claims, final String key) {
    final Object value = claims.get(key);
    return value != null ? String.valueOf(value) : null;
  }

  private static boolean booleanClaim(final Map<String, Object> claims, final String key) {
    final Object value = claims.get(key);
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value instanceof String stringValue) {
      return Boolean.parseBoolean(stringValue);
    }
    return false;
  }

  private static String firstNonBlank(final String first, final String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }

  private static String firstNameFrom(final String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return null;
    }
    final String[] parts = fullName.trim().split("\\s+", 2);
    return parts[0];
  }

  private static String lastNameFrom(final String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return null;
    }
    final String[] parts = fullName.trim().split("\\s+", 2);
    return parts.length > 1 ? parts[1] : null;
  }
}
