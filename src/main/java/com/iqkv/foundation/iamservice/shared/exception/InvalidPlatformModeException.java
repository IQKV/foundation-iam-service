package com.iqkv.foundation.iamservice.shared.exception;

/**
 * Thrown when the platform rollout mode configuration is invalid or missing.
 * This exception causes the service to fail startup and set readiness to DOWN.
 */
public class InvalidPlatformModeException extends RuntimeException {

  public InvalidPlatformModeException(final String message) {
    super(message);
  }

  public InvalidPlatformModeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
