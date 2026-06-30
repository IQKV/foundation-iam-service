package com.iqkv.foundation.iamservice.oauth2;

public class OidcIdentityNotFoundException extends RuntimeException {

  public OidcIdentityNotFoundException(final String message) {
    super(message);
  }
}
