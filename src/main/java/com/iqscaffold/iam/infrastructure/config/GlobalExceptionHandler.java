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

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.iqscaffold.iam.shared.exception.AccountLockedException;
import com.iqscaffold.iam.shared.exception.InvalidTenantStateException;
import com.iqscaffold.iam.shared.exception.InvalidTokenSignatureException;
import com.iqscaffold.iam.shared.exception.InvalidTokenTypeException;
import com.iqscaffold.iam.shared.exception.InvalidVerificationTokenException;
import com.iqscaffold.iam.shared.exception.MembershipNotFoundException;
import com.iqscaffold.iam.shared.exception.PasswordResetRateLimitException;
import com.iqscaffold.iam.shared.exception.PasswordResetTokenNotFoundException;
import com.iqscaffold.iam.shared.exception.TenantAlreadyExistsException;
import com.iqscaffold.iam.shared.exception.TenantMembershipAlreadyExistsException;
import com.iqscaffold.iam.shared.exception.TenantNotAvailableException;
import com.iqscaffold.iam.shared.exception.TenantNotFoundException;
import com.iqscaffold.iam.shared.exception.TenantSuspendedException;
import com.iqscaffold.iam.shared.exception.TokenExpiredException;
import com.iqscaffold.iam.shared.exception.UserNotFoundException;
import com.iqscaffold.iam.shared.exception.VerificationRateLimitException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TenantNotFoundException.class)
  public ProblemDetail handleTenantNotFound(final TenantNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(TenantAlreadyExistsException.class)
  public ProblemDetail handleTenantAlreadyExists(final TenantAlreadyExistsException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(InvalidTenantStateException.class)
  public ProblemDetail handleInvalidTenantState(final InvalidTenantStateException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(TenantSuspendedException.class)
  public ProblemDetail handleTenantSuspended(final TenantSuspendedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(TenantNotAvailableException.class)
  public ProblemDetail handleTenantNotAvailable(final TenantNotAvailableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(MembershipNotFoundException.class)
  public ProblemDetail handleMembershipNotFound(final MembershipNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(TenantMembershipAlreadyExistsException.class)
  public ProblemDetail handleMembershipAlreadyExists(final TenantMembershipAlreadyExistsException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ProblemDetail handleUserNotFound(final UserNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(AccountLockedException.class)
  public ProblemDetail handleAccountLocked(final AccountLockedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(InvalidTokenTypeException.class)
  public ProblemDetail handleInvalidTokenType(final InvalidTokenTypeException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(TokenExpiredException.class)
  public ProblemDetail handleTokenExpired(final TokenExpiredException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(InvalidTokenSignatureException.class)
  public ProblemDetail handleInvalidTokenSignature(final InvalidTokenSignatureException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(InvalidVerificationTokenException.class)
  public ProblemDetail handleInvalidVerificationToken(final InvalidVerificationTokenException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(VerificationRateLimitException.class)
  public ProblemDetail handleVerificationRateLimit(final VerificationRateLimitException ex) {
    final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return problem;
  }

  @ExceptionHandler(PasswordResetTokenNotFoundException.class)
  public ProblemDetail handlePasswordResetTokenNotFound(final PasswordResetTokenNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(PasswordResetRateLimitException.class)
  public ProblemDetail handlePasswordResetRateLimit(final PasswordResetRateLimitException ex) {
    final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
    return problem;
  }
}
