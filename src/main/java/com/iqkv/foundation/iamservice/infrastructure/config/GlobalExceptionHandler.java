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

package com.iqkv.foundation.iamservice.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingException;
import com.iqkv.foundation.iamservice.oauth2.OidcIdentityNotFoundException;
import com.iqkv.foundation.iamservice.oauth2.OidcProvisioningException;
import com.iqkv.foundation.iamservice.shared.exception.AccountBannedException;
import com.iqkv.foundation.iamservice.shared.exception.AccountLockedException;
import com.iqkv.foundation.iamservice.shared.exception.AccountNotActiveException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidAccountStatusException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidObjectKeyException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidPasswordException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTokenSignatureException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidVerificationTokenException;
import com.iqkv.foundation.iamservice.shared.exception.InvitationAlreadyPendingException;
import com.iqkv.foundation.iamservice.shared.exception.InvitationNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.MagicLinkRateLimitException;
import com.iqkv.foundation.iamservice.shared.exception.MagicLinkTokenNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.NoPlatformAuthorityException;
import com.iqkv.foundation.iamservice.shared.exception.PasswordResetRateLimitException;
import com.iqkv.foundation.iamservice.shared.exception.PasswordResetTokenNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.PlanFeatureNotAvailableException;
import com.iqkv.foundation.iamservice.shared.exception.PlanMemberQuotaException;
import com.iqkv.foundation.iamservice.shared.exception.SchemaProvisioningException;
import com.iqkv.foundation.iamservice.shared.exception.SiteAnnouncementNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.TenantContextMismatchException;
import com.iqkv.foundation.iamservice.shared.exception.TenantManagementException;
import com.iqkv.foundation.iamservice.shared.exception.TenantMembershipAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotAvailableException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TenantSuspendedException;
import com.iqkv.foundation.iamservice.shared.exception.TokenExpiredException;
import com.iqkv.foundation.iamservice.shared.exception.TokenRevokedException;
import com.iqkv.foundation.iamservice.shared.exception.UserManagementException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.UserSignupException;
import com.iqkv.foundation.iamservice.shared.exception.VerificationRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MDC_CORRELATION_ID = "correlationId";

  private final MessageSource messageSource;

  public GlobalExceptionHandler(final MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private String msg(final String key, final Locale locale, final Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }

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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex,
                                                        final HttpServletRequest request,
                                                        final Locale locale) {
    log.warn("Validation failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        msg("error.detail.validation-failed", locale),
        request);
    final List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> Map.of("field", fe.getField(), "message",
            fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
        .toList();
    pd.setProperty("fields", fields);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(final ConstraintViolationException ex,
                                                                 final HttpServletRequest request,
                                                                 final Locale locale) {
    log.warn("Constraint violation: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.constraint-violation", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(InvalidAccountStatusException.class)
  public ResponseEntity<ProblemDetail> handleInvalidAccountStatus(final InvalidAccountStatusException ex,
                                                                  final HttpServletRequest request,
                                                                  final Locale locale) {
    log.warn("Invalid account status: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(InvalidVerificationTokenException.class)
  public ResponseEntity<ProblemDetail> handleInvalidVerificationToken(final InvalidVerificationTokenException ex,
                                                                      final HttpServletRequest request,
                                                                      final Locale locale) {
    log.warn("Invalid verification token: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-verification-token", locale),
        400,
        msg("error.detail.invalid-verification-token", locale),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(PasswordResetTokenNotFoundException.class)
  public ResponseEntity<ProblemDetail> handlePasswordResetTokenNotFound(final PasswordResetTokenNotFoundException ex,
                                                                        final HttpServletRequest request,
                                                                        final Locale locale) {
    log.warn("Password reset token not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-password-reset-token", locale),
        400,
        msg("error.detail.invalid-password-reset-token", locale),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(InvalidPasswordException.class)
  public ResponseEntity<ProblemDetail> handleInvalidPassword(final InvalidPasswordException ex,
                                                             final HttpServletRequest request,
                                                             final Locale locale) {
    log.warn("Invalid password: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-password", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(InvalidObjectKeyException.class)
  public ResponseEntity<ProblemDetail> handleInvalidObjectKey(final InvalidObjectKeyException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
    log.warn("Invalid object key: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(OidcProvisioningException.class)
  public ResponseEntity<ProblemDetail> handleOidcProvisioning(final OidcProvisioningException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
    log.warn("OIDC provisioning/linking failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(OidcIdentityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleOidcIdentityNotFound(final OidcIdentityNotFoundException ex,
                                                                  final HttpServletRequest request,
                                                                  final Locale locale) {
    log.warn("OIDC identity not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.not-found", locale),
        404,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler({AuthenticationException.class})
  public ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException ex,
                                                            final HttpServletRequest request,
                                                            final Locale locale) {
    log.warn("Authentication failed: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.unauthorized", locale),
        401,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(InvalidTokenTypeException.class)
  public ResponseEntity<ProblemDetail> handleInvalidTokenType(final InvalidTokenTypeException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
    log.warn("Invalid token type: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-token-type", locale),
        401,
        msg("error.authentication.invalid-token-type", locale),
        request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(TokenExpiredException.class)
  public ResponseEntity<ProblemDetail> handleTokenExpired(final TokenExpiredException ex,
                                                          final HttpServletRequest request,
                                                          final Locale locale) {
    log.warn("Token expired: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.token-expired", locale),
        401,
        msg("error.authentication.token-expired", locale),
        request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(InvalidTokenSignatureException.class)
  public ResponseEntity<ProblemDetail> handleInvalidTokenSignature(final InvalidTokenSignatureException ex,
                                                                   final HttpServletRequest request,
                                                                   final Locale locale) {
    log.warn("Invalid token signature: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-token-signature", locale),
        401,
        msg("error.authentication.invalid-token-signature", locale),
        request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(TokenRevokedException.class)
  public ResponseEntity<ProblemDetail> handleTokenRevoked(final TokenRevokedException ex,
                                                          final HttpServletRequest request,
                                                          final Locale locale) {
    log.warn("Token revoked: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.token-revoked", locale),
        401,
        msg("error.authentication.token-revoked", locale),
        request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException ex,
                                                          final HttpServletRequest request,
                                                          final Locale locale) {
    log.warn("Access denied: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.forbidden", locale),
        403,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(PlanFeatureNotAvailableException.class)
  public ResponseEntity<ProblemDetail> handlePlanFeatureNotAvailable(final PlanFeatureNotAvailableException ex,
                                                                     final HttpServletRequest request,
                                                                     final Locale locale) {
    log.warn("Plan feature not available: featureCode={}, planMessage={}", ex.getFeatureCode(), ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        "Plan upgrade required",
        403,
        ex.getMessage(),
        request);
    pd.setProperty("featureCode", ex.getFeatureCode());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(PlanMemberQuotaException.class)
  public ResponseEntity<ProblemDetail> handlePlanMemberQuota(final PlanMemberQuotaException ex,
                                                             final HttpServletRequest request,
                                                             final Locale locale) {
    log.warn("Plan member quota exceeded: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        "Plan upgrade required",
        402,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(pd);
  }

  @ExceptionHandler(MembershipNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleMembershipNotFound(final MembershipNotFoundException ex,
                                                                final HttpServletRequest request,
                                                                final Locale locale) {
    log.warn("Membership not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.forbidden", locale),
        403,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(NoPlatformAuthorityException.class)
  public ResponseEntity<ProblemDetail> handleNoPlatformAuthority(final NoPlatformAuthorityException ex,
                                                                 final HttpServletRequest request,
                                                                 final Locale locale) {
    log.warn("Platform admin sign-in rejected — no platform authortities: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.forbidden", locale),
        403,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantContextMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTenantContextMismatch(final TenantContextMismatchException ex,
                                                                   final HttpServletRequest request,
                                                                   final Locale locale) {
    log.warn("Tenant context mismatch: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.tenant-context-mismatch", locale),
        403,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantSuspendedException.class)
  public ResponseEntity<ProblemDetail> handleTenantSuspended(final TenantSuspendedException ex,
                                                             final HttpServletRequest request,
                                                             final Locale locale) {
    log.warn("Tenant suspended: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.tenant-suspended", locale),
        403,
        msg("error.detail.tenant-suspended", locale),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(TenantNotAvailableException.class)
  public ResponseEntity<ProblemDetail> handleTenantNotAvailable(final TenantNotAvailableException ex,
                                                                final HttpServletRequest request,
                                                                final Locale locale) {
    log.warn("Tenant not available: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.tenant-not-available", locale),
        403,
        msg("error.detail.tenant-not-available", locale),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ProblemDetail> handleAccountLocked(final AccountLockedException ex,
                                                           final HttpServletRequest request,
                                                           final Locale locale) {
    log.warn("Account locked: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.account-locked", locale),
        403,
        msg("error.detail.account-locked", locale),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AccountNotActiveException.class)
  public ResponseEntity<ProblemDetail> handleAccountNotActive(final AccountNotActiveException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
    log.warn("Account not active: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.account-not-active", locale),
        403,
        msg("error.detail.account-not-active", locale),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(AccountBannedException.class)
  public ResponseEntity<ProblemDetail> handleAccountBanned(final AccountBannedException ex,
                                                           final HttpServletRequest request,
                                                           final Locale locale) {
    log.warn("Account banned: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.account-banned", locale),
        403,
        msg("error.detail.account-banned", locale),
        request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(SiteAnnouncementNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleSiteAnnouncementNotFound(final SiteAnnouncementNotFoundException ex,
                                                                      final HttpServletRequest request,
                                                                      final Locale locale) {
    log.warn("Site announcement not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.not-found", locale),
        404,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFound(final UserNotFoundException ex,
                                                          final HttpServletRequest request,
                                                          final Locale locale) {
    log.warn("User not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.user-not-found", locale),
        404,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(TenantManagementException.class)
  public ResponseEntity<ProblemDetail> handleTenantManagement(final TenantManagementException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
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
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.tenant-management-error", locale),
        status,
        ex.getMessage(),
        request);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(TenantMembershipAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleMembershipAlreadyExists(final TenantMembershipAlreadyExistsException ex,
                                                                     final HttpServletRequest request,
                                                                     final Locale locale) {
    log.warn("Membership already exists: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.conflict", locale),
        409,
        msg("error.membership.already-exists", locale),
        request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(InvitationNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleInvitationNotFound(final InvitationNotFoundException ex,
                                                                final HttpServletRequest request,
                                                                final Locale locale) {
    log.warn("Invitation not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.not-found", locale),
        404,
        msg("error.invitation.not-found", locale),
        request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(InvitationAlreadyPendingException.class)
  public ResponseEntity<ProblemDetail> handleInvitationAlreadyPending(final InvitationAlreadyPendingException ex,
                                                                      final HttpServletRequest request,
                                                                      final Locale locale) {
    log.warn("Invitation already pending: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.conflict", locale),
        409,
        msg("error.invitation.already-pending", locale),
        request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(UserSignupException.class)
  public ResponseEntity<ProblemDetail> handleUserSignup(final UserSignupException ex,
                                                        final HttpServletRequest request,
                                                        final Locale locale) {
    log.warn("User signup error: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.registration-error", locale),
        409,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(UserManagementException.class)
  public ResponseEntity<ProblemDetail> handleUserManagement(final UserManagementException ex,
                                                            final HttpServletRequest request,
                                                            final Locale locale) {
    log.warn("User management error: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.unprocessable-entity", locale),
        422,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
  }

  @ExceptionHandler(VerificationRateLimitException.class)
  public ResponseEntity<ProblemDetail> handleVerificationRateLimit(final VerificationRateLimitException ex,
                                                                   final HttpServletRequest request,
                                                                   final Locale locale) {
    log.warn("Verification rate limit exceeded: retryAfter={}s", ex.getRetryAfterSeconds());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.too-many-requests", locale),
        429,
        msg("error.verification.rate-limit", locale, ex.getRetryAfterSeconds()),
        request);
    pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
        .body(pd);
  }

  @ExceptionHandler(PasswordResetRateLimitException.class)
  public ResponseEntity<ProblemDetail> handlePasswordResetRateLimit(final PasswordResetRateLimitException ex,
                                                                    final HttpServletRequest request,
                                                                    final Locale locale) {
    log.warn("Password reset rate limit exceeded: retryAfter={}s", ex.getRetryAfterSeconds());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.too-many-requests", locale),
        429,
        msg("error.password-reset.rate-limit", locale, ex.getRetryAfterSeconds()),
        request);
    pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
        .body(pd);
  }

  @ExceptionHandler(MagicLinkTokenNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleMagicLinkTokenNotFound(final MagicLinkTokenNotFoundException ex,
                                                                    final HttpServletRequest request,
                                                                    final Locale locale) {
    log.warn("Magic link token not found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.invalid-password-reset-token", locale),
        400,
        msg("error.detail.invalid-password-reset-token", locale),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(MagicLinkRateLimitException.class)
  public ResponseEntity<ProblemDetail> handleMagicLinkRateLimit(final MagicLinkRateLimitException ex,
                                                                final HttpServletRequest request,
                                                                final Locale locale) {
    log.warn("Magic link rate limit exceeded: retryAfter={}s", ex.getRetryAfterSeconds());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.too-many-requests", locale),
        429,
        "Too many magic link requests. Retry after " + ex.getRetryAfterSeconds() + " seconds.",
        request);
    pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
        .body(pd);
  }

  @ExceptionHandler(MessagingException.class)
  public ResponseEntity<ProblemDetail> handleMessaging(final MessagingException ex,
                                                       final HttpServletRequest request,
                                                       final Locale locale) {
    log.error("Messaging service error: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.service-unavailable", locale),
        503,
        msg("error.detail.messaging-unavailable", locale),
        request);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMethodNotSupported(final HttpRequestMethodNotSupportedException ex,
                                                                final HttpServletRequest request,
                                                                final Locale locale) {
    log.warn("Method not supported: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        "Method Not Allowed",
        405,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(final HttpMediaTypeNotSupportedException ex,
                                                                   final HttpServletRequest request,
                                                                   final Locale locale) {
    log.warn("Media type not supported: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        "Unsupported Media Type",
        415,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(pd);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ProblemDetail> handleMediaTypeNotAcceptable(final HttpMediaTypeNotAcceptableException ex,
                                                                    final HttpServletRequest request,
                                                                    final Locale locale) {
    log.warn("Media type not acceptable: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        "Not Acceptable",
        406,
        ex.getMessage(),
        request);
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(pd);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ProblemDetail> handleMissingParameter(final MissingServletRequestParameterException ex,
                                                              final HttpServletRequest request,
                                                              final Locale locale) {
    log.warn("Missing parameter: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(final MethodArgumentTypeMismatchException ex,
                                                          final HttpServletRequest request,
                                                          final Locale locale) {
    log.warn("Type mismatch: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.validation-failed", locale),
        400,
        ex.getMessage(),
        request);
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoHandlerFound(final NoHandlerFoundException ex,
                                                            final HttpServletRequest request,
                                                            final Locale locale) {
    log.warn("No handler found: {}", ex.getMessage());
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.not-found", locale),
        404,
        "No resource found at " + request.getRequestURI(),
        request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneral(final Exception ex,
                                                     final HttpServletRequest request,
                                                     final Locale locale) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);
    final ProblemDetail pd = problem("about:blank",
        msg("error.title.internal-server-error", locale),
        500,
        msg("error.detail.unexpected", locale),
        request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }
}
