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

package com.iqscaffold.iam.infrastructure.config;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.iqscaffold.iam.infrastructure.messaging.MessagingException;
import com.iqscaffold.iam.shared.exception.AccountLockedException;
import com.iqscaffold.iam.shared.exception.InvalidTenantStateException;
import com.iqscaffold.iam.shared.exception.InvalidTokenSignatureException;
import com.iqscaffold.iam.shared.exception.InvalidTokenTypeException;
import com.iqscaffold.iam.shared.exception.InvalidVerificationTokenException;
import com.iqscaffold.iam.shared.exception.MembershipNotFoundException;
import com.iqscaffold.iam.shared.exception.PasswordResetRateLimitException;
import com.iqscaffold.iam.shared.exception.PasswordResetTokenNotFoundException;
import com.iqscaffold.iam.shared.exception.SchemaProvisioningException;
import com.iqscaffold.iam.shared.exception.TenantAlreadyExistsException;
import com.iqscaffold.iam.shared.exception.TenantContextMismatchException;
import com.iqscaffold.iam.shared.exception.TenantManagementException;
import com.iqscaffold.iam.shared.exception.TenantMembershipAlreadyExistsException;
import com.iqscaffold.iam.shared.exception.TenantNotAvailableException;
import com.iqscaffold.iam.shared.exception.TenantNotFoundException;
import com.iqscaffold.iam.shared.exception.TenantSuspendedException;
import com.iqscaffold.iam.shared.exception.TokenExpiredException;
import com.iqscaffold.iam.shared.exception.UserManagementException;
import com.iqscaffold.iam.shared.exception.UserNotFoundException;
import com.iqscaffold.iam.shared.exception.UserRegistrationException;
import com.iqscaffold.iam.shared.exception.VerificationRateLimitException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MDC_CORRELATION_ID = "correlationId";

  // ── Helper ────────────────────────────────────────────────────────────────

  private ProblemDetail problem(final String type,
                                final String title,
                                final int status,
                                final String detail,
                                final HttpServletRequest request) {
    final ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setType(URI.create(type));
    pd.setTitle(title);
    pd.setDetail(detail);
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("correlationId", MDC.get(MDC_CORRELATION_ID));
    pd.setProperty("requestId", "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    return pd;
  }

  // ── 400 Bad Request ───────────────────────────────────────────────────────

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex,
                                                        final HttpServletRequest request) {
    log.warn("Validation failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Validation Failed", 400,
        "Request validation failed", request);
    final List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> Map.of("field", fe.getField(), "message",
            fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
        .toList();
    pd.setProperty("fields", fields);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(final ConstraintViolationException ex,
                                                                 final HttpServletRequest request) {
    log.warn("Constraint violation: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Constraint Violation", 400,
        ex.getMessage(), request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(InvalidVerificationTokenException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVerificationToken(final InvalidVerificationTokenException ex,
                                                                      final HttpServletRequest request) {
    log.warn("Invalid verification token: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Invalid Verification Token", 400,
        "Invalid or expired verification token", request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(PasswordResetTokenNotFoundException.class)
  public ResponseEntity<ProblemDetail> handlePasswordResetTokenNotFound(final PasswordResetTokenNotFoundException ex,
                                                                        final HttpServletRequest request) {
    log.warn("Password reset token not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Invalid Password Reset Token", 400,
        "Invalid or expired password reset token", request);
    return ResponseEntity.badRequest().body(pd);
  }

  // ── 401 Unauthorized ──────────────────────────────────────────────────────

  @ExceptionHandler({AuthenticationException.class})
  public ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException ex,
                                                            final HttpServletRequest request) {
    log.warn("Authentication failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Unauthorized", 401,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(InvalidTokenTypeException.class)
  public ResponseEntity<ProblemDetail> handleInvalidTokenType(final InvalidTokenTypeException ex,
                                                              final HttpServletRequest request) {
    log.warn("Invalid token type: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Invalid Token Type", 401,
        "Invalid token type", request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(TokenExpiredException.class)
  public ResponseEntity<ProblemDetail> handleTokenExpired(final TokenExpiredException ex,
                                                          final HttpServletRequest request) {
    log.warn("Token expired: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Token Expired", 401,
        "Refresh token expired", request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(InvalidTokenSignatureException.class)
  public ResponseEntity<ProblemDetail> handleInvalidTokenSignature(final InvalidTokenSignatureException ex,
                                                                   final HttpServletRequest request) {
    log.warn("Invalid token signature: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Invalid Token Signature", 401,
        "Invalid token signature", request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  // ── 403 Forbidden ─────────────────────────────────────────────────────────

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException ex,
                                                          final HttpServletRequest request) {
    log.warn("Access denied: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Forbidden", 403,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(MembershipNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleMembershipNotFound(final MembershipNotFoundException ex,
                                                                final HttpServletRequest request) {
    log.warn("Membership not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Forbidden", 403,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantContextMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTenantContextMismatch(final TenantContextMismatchException ex,
                                                                   final HttpServletRequest request) {
    log.warn("Tenant context mismatch: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Tenant Context Mismatch", 403,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantSuspendedException.class)
  public ResponseEntity<ProblemDetail> handleTenantSuspended(final TenantSuspendedException ex,
                                                             final HttpServletRequest request) {
    log.warn("Tenant suspended: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Tenant Suspended", 403,
        "Tenant suspended", request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantNotAvailableException.class)
  public ResponseEntity<ProblemDetail> handleTenantNotAvailable(final TenantNotAvailableException ex,
                                                                final HttpServletRequest request) {
    log.warn("Tenant not available: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Tenant Not Available", 403,
        "Tenant not available", request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ProblemDetail> handleAccountLocked(final AccountLockedException ex,
                                                           final HttpServletRequest request) {
    log.warn("Account locked: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Account Locked", 403,
        "Account temporarily locked", request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  // ── 404 Not Found ─────────────────────────────────────────────────────────

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFound(final UserNotFoundException ex,
                                                          final HttpServletRequest request) {
    log.warn("User not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "User Not Found", 404,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  // ── 409 Conflict / 404 / 422 / 503 — TenantManagementException ───────────

  @ExceptionHandler(TenantManagementException.class)
  public ResponseEntity<ProblemDetail> handleTenantManagement(final TenantManagementException ex,
                                                              final HttpServletRequest request) {
    final int status = switch (ex) {
      case TenantAlreadyExistsException ignored -> 409;
      case TenantNotFoundException ignored -> 404;
      case SchemaProvisioningException ignored -> 503;
      case InvalidTenantStateException ignored -> 409;
      default -> 422;
    };
    if (status >= 500) {
      log.error("Tenant management error ({}): {}", status, ex.getMessage(), ex);
    } else {
      log.warn("Tenant management error ({}): {}", status, ex.getMessage());
    }
    final ProblemDetail pd = problem("about:blank", "Tenant Management Error", status,
        ex.getMessage(), request);
    return ResponseEntity.status(status).body(pd);
  }

  // ── 409 Conflict ──────────────────────────────────────────────────────────

  @ExceptionHandler(TenantMembershipAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleMembershipAlreadyExists(final TenantMembershipAlreadyExistsException ex,
                                                                     final HttpServletRequest request) {
    log.warn("Membership already exists: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Conflict", 409,
        "User is already a member of this tenant", request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(UserRegistrationException.class)
  public ResponseEntity<ProblemDetail> handleUserRegistration(final UserRegistrationException ex,
                                                              final HttpServletRequest request) {
    log.warn("User registration error: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Registration Error", 409,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  // ── 422 Unprocessable ─────────────────────────────────────────────────────

  @ExceptionHandler(UserManagementException.class)
  public ResponseEntity<ProblemDetail> handleUserManagement(final UserManagementException ex,
                                                            final HttpServletRequest request) {
    log.warn("User management error: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank", "Unprocessable Entity", 422,
        ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
  }

  // ── 429 Too Many Requests ─────────────────────────────────────────────────

  @ExceptionHandler(VerificationRateLimitException.class)
  public ResponseEntity<ProblemDetail> handleVerificationRateLimit(final VerificationRateLimitException ex,
                                                                   final HttpServletRequest request) {
    log.warn("Verification rate limit exceeded: retryAfter={}s", ex.getRetryAfterSeconds());
    final ProblemDetail pd = problem("about:blank", "Too Many Requests", 429,
        ex.getMessage(), request);
    pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
        .body(pd);
  }

  @ExceptionHandler(PasswordResetRateLimitException.class)
  public ResponseEntity<ProblemDetail> handlePasswordResetRateLimit(final PasswordResetRateLimitException ex,
                                                                    final HttpServletRequest request) {
    log.warn("Password reset rate limit exceeded: retryAfter={}s", ex.getRetryAfterSeconds());
    final ProblemDetail pd = problem("about:blank", "Too Many Requests", 429,
        ex.getMessage(), request);
    pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
        .body(pd);
  }

  // ── 503 Service Unavailable ───────────────────────────────────────────────

  @ExceptionHandler(MessagingException.class)
  public ResponseEntity<ProblemDetail> handleMessaging(final MessagingException ex,
                                                       final HttpServletRequest request) {
    log.error("Messaging service error: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Service Unavailable", 503,
        "Messaging service unavailable", request);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
  }

  // ── 500 Catch-all ─────────────────────────────────────────────────────────

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneral(final Exception ex,
                                                     final HttpServletRequest request) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank", "Internal Server Error", 500,
        "An unexpected error occurred", request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }
}
