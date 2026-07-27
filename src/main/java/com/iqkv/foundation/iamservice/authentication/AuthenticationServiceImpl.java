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

package com.iqkv.foundation.iamservice.authentication;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.ban.BanService;
import com.iqkv.foundation.iamservice.denylist.TokenDenylistService;
import com.iqkv.foundation.iamservice.email.EmailVerificationToken;
import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.infrastructure.messaging.SigninAttemptEvent;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.shared.exception.AccountBannedException;
import com.iqkv.foundation.iamservice.shared.exception.AccountLockedException;
import com.iqkv.foundation.iamservice.shared.exception.AccountNotActiveException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidVerificationTokenException;
import com.iqkv.foundation.iamservice.shared.exception.NoPlatformAuthorityException;
import com.iqkv.foundation.iamservice.shared.exception.TenantContextMismatchException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotAvailableException;
import com.iqkv.foundation.iamservice.shared.exception.TenantSuspendedException;
import com.iqkv.foundation.iamservice.shared.exception.TokenRevokedException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.VerificationRateLimitException;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantListingService;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantService;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.UserMapper;
import com.iqkv.foundation.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int RESEND_RATE_LIMIT = 3;
  private static final Duration RESEND_WINDOW = Duration.ofHours(1);
  private static final Duration EMAIL_TOKEN_TTL = Duration.ofHours(24);

  private final UserMapper userMapper;
  private final TenantMapper tenantMapper;
  private final TenantMembershipMapper membershipMapper;
  private final MembershipService membershipService;
  private final AccountLockoutManager accountLockoutManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenGenerator jwtTokenGenerator;
  private final JwtDecoder jwtDecoder;
  private final TokenDenylistService tokenDenylistService;
  private final EmailVerificationTokenMapper emailVerificationTokenMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;
  private final PlatformAuthorityMapper platformAuthorityMapper;
  private final TenantService tenantService;
  private final IamServiceMetrics metrics;
  private final BanService banService;
  private final TenantListingService tenantListingService;

  public AuthenticationServiceImpl(
      final UserMapper userMapper,
      final TenantMapper tenantMapper,
      final TenantMembershipMapper membershipMapper,
      final MembershipService membershipService,
      final AccountLockoutManager accountLockoutManager,
      final PasswordEncoder passwordEncoder,
      final JwtTokenGenerator jwtTokenGenerator,
      final JwtDecoder jwtDecoder,
      final TokenDenylistService tokenDenylistService,
      final EmailVerificationTokenMapper emailVerificationTokenMapper,
      final MessagingService messagingService,
      final NotificationConfigurationProperties notificationProps,
      final PlatformAuthorityMapper platformAuthorityMapper,
      final TenantService tenantService,
      final IamServiceMetrics metrics,
      final BanService banService,
      final TenantListingService tenantListingService) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.membershipService = membershipService;
    this.accountLockoutManager = accountLockoutManager;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenGenerator = jwtTokenGenerator;
    this.jwtDecoder = jwtDecoder;
    this.tokenDenylistService = tokenDenylistService;
    this.emailVerificationTokenMapper = emailVerificationTokenMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
    this.platformAuthorityMapper = platformAuthorityMapper;
    this.tenantService = tenantService;
    this.metrics = metrics;
    this.banService = banService;
    this.tenantListingService = tenantListingService;
  }

  @Override
  public AuthenticationDtos.TokenResponse signIn(final AuthenticationDtos.SignInRequest request) {
    final String tenantKey = TenantContext.getCurrentTenant();
    return metrics.authDurationTimer(tenantKey, "login").record(() -> {
      UUID userIdForAudit = null;
      SigninAttemptEvent.FailureReason failureReasonForAudit = null;

      try {
        final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
            .orElseThrow(() -> new TenantNotAvailableException("Tenant not available"));

        // Allow access to internal (personal) tenants even if status is SUSPENDED
        final boolean isInternalTenant = Boolean.TRUE.equals(tenant.getIsInternal());

        if (!isInternalTenant && tenant.getStatus() == TenantStatus.SUSPENDED) {
          log.warn("Signin failed: tenant suspended, tenantKey={}, email={}", tenantKey, request.email());
          failureReasonForAudit = SigninAttemptEvent.FailureReason.TENANT_SUSPENDED;
          publishSigninAttemptEvent(request.email(), null, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, failureReasonForAudit);
          throw new TenantSuspendedException("Tenant suspended");
        }
        if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.PROVISIONING_FAILED) {
          log.warn("Signin failed: tenant not available, tenantKey={}, email={}", tenantKey, request.email());
          failureReasonForAudit = SigninAttemptEvent.FailureReason.TENANT_NOT_AVAILABLE;
          publishSigninAttemptEvent(request.email(), null, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, failureReasonForAudit);
          throw new TenantNotAvailableException("Tenant not available");
        }

        final var user = userMapper.findByEmail(request.email())
            .orElse(null);

        if (user == null) {
          log.warn("Signin failed: invalid credentials, tenantKey={}", tenantKey);
          publishSigninAttemptEvent(request.email(), null, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.INVALID_CREDENTIALS);
          throw new BadCredentialsException("Invalid credentials");
        }

        userIdForAudit = user.getId();

        if (user.getStatus() == AccountStatus.LOCKED) {
          log.warn("Signin failed: account locked, userId={}, tenantKey={}", user.getId(), tenantKey);
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountLockedException();
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
          log.warn("Signin failed: account not active, userId={}, tenantKey={}", user.getId(), tenantKey);
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_NOT_ACTIVE);
          throw new AccountNotActiveException();
        }

        if (banService.isUserBanned(user.getId(), tenantKey)) {
          log.warn("Signin failed: account banned, userId={}, tenantKey={}", user.getId(), tenantKey);
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountBannedException();
        }

        if (accountLockoutManager.isLocked(request.email())) {
          log.warn("Signin failed: account locked, tenantKey={}, email={}", tenantKey, request.email());
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
          log.warn("Signin failed: invalid password, userId={}, tenantKey={}", user.getId(), tenantKey);
          accountLockoutManager.recordFailedAttempt(request.email());
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.INVALID_CREDENTIALS);
          throw new BadCredentialsException("Invalid credentials");
        }

        final var membership = membershipService.resolveMembership(user.getId(), tenantKey);
        final var authorities = membershipService.getAuthorities(membership.getId());

        accountLockoutManager.reset(request.email());

        // Set first sign in time if not already set (WHERE clause ensures idempotency)
        if (user.getFirstSignInAt() == null) {
          userMapper.setFirstSignInAt(user.getId(), Instant.now());
        }

        final String accessToken = jwtTokenGenerator.generateAccessToken(
            user, tenantKey, authorities, tenant.getActivePlanCode());
        final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, tenantKey);

        // Publish successful signin attempt event
        publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
            SigninAttemptEvent.AttemptResult.SUCCESS, null);

        log.info("Signin successful: userId={}, tenantKey={}", user.getId(), tenantKey);
        metrics.recordAuthOutcome(tenantKey, "login", "success", null);
        return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, tenantKey);
      } catch (final Exception e) {
        // Only publish if we haven't already published in the specific catch blocks above
        if (!(e instanceof TenantSuspendedException
              || e instanceof TenantNotAvailableException
              || e instanceof AccountNotActiveException
              || e instanceof AccountLockedException
              || e instanceof AccountBannedException
              || e instanceof BadCredentialsException)) {
          log.error("Signin failed with unknown error, tenantKey={}, email={}", tenantKey, request.email(), e);
          publishSigninAttemptEvent(request.email(), userIdForAudit, tenantKey,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.UNKNOWN);
        }

        metrics.recordAuthOutcome(tenantKey, "login", "failure", getAuthFailureReason(e));
        throw e;
      }
    });
  }

  private String getAuthFailureReason(final Exception e) {
    if (e instanceof BadCredentialsException) {
      return "bad_credentials";
    } else if (e instanceof AccountLockedException) {
      return "account_locked";
    } else if (e instanceof AccountNotActiveException) {
      return "user_inactive";
    } else if (e instanceof TenantSuspendedException) {
      return "tenant_suspended";
    } else if (e instanceof TenantNotAvailableException) {
      return "tenant_not_available";
    } else if (e instanceof NoPlatformAuthorityException) {
      return "no_platform_authority";
    } else if (e instanceof TokenRevokedException) {
      return "token_revoked";
    } else if (e instanceof com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException) {
      return "invalid_token_type";
    } else if (e instanceof TenantContextMismatchException) {
      return "tenant_mismatch";
    } else if (e instanceof AccountBannedException) {
      return "account_banned";
    }
    return "unknown";
  }

  @Override
  public AuthenticationDtos.TokenResponse adminSignIn(final AuthenticationDtos.SignInRequest request) {
    return metrics.authDurationTimer(null, "admin_login").record(() -> {
      UUID userIdForAudit = null;

      try {
        final var user = userMapper.findByEmail(request.email())
            .orElse(null);

        if (user == null) {
          log.warn("Admin signin failed: invalid credentials");
          publishSigninAttemptEvent(request.email(), null, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.INVALID_CREDENTIALS);
          throw new BadCredentialsException("Invalid credentials");
        }

        userIdForAudit = user.getId();

        if (user.getStatus() == AccountStatus.LOCKED) {
          log.warn("Admin signin failed: account locked, userId={}", user.getId());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountLockedException();
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
          log.warn("Admin signin failed: account not active, userId={}", user.getId());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_NOT_ACTIVE);
          throw new AccountNotActiveException();
        }

        if (banService.isUserBanned(user.getId(), null)) {
          log.warn("Admin signin failed: account banned, userId={}", user.getId());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountBannedException();
        }

        if (accountLockoutManager.isLocked(request.email())) {
          log.warn("Admin signin failed: account locked, email={}", request.email());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.ACCOUNT_LOCKED);
          throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
          log.warn("Admin signin failed: invalid password, userId={}", user.getId());
          accountLockoutManager.recordFailedAttempt(request.email());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.INVALID_CREDENTIALS);
          throw new BadCredentialsException("Invalid credentials");
        }

        final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(user.getId());
        if (platformAuthorities.isEmpty()) {
          log.warn("Admin signin failed: no platform authorities, userId={}", user.getId());
          // Record the failed attempt to prevent user enumeration via timing differences,
          // then surface a 403 — not a 401 — so the admin UI can show a clear "no access" message.
          accountLockoutManager.reset(request.email());
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.INVALID_CREDENTIALS);
          throw new NoPlatformAuthorityException();
        }

        accountLockoutManager.reset(request.email());

        // tenant_id is null for platform-scoped tokens — no tenant context applies.
        final String accessToken = jwtTokenGenerator.generateAccessToken(user, null, platformAuthorities);
        final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, null);

        // Publish successful admin signin attempt event
        publishSigninAttemptEvent(request.email(), userIdForAudit, null,
            SigninAttemptEvent.AttemptResult.SUCCESS, null);

        log.info("Admin signin successful: userId={}", user.getId());
        metrics.recordAuthOutcome(null, "admin_login", "success", null);
        return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, null);
      } catch (final Exception e) {
        // Only publish if we haven't already published in the specific catch blocks above
        if (!(e instanceof AccountNotActiveException
              || e instanceof AccountLockedException
              || e instanceof AccountBannedException
              || e instanceof BadCredentialsException
              || e instanceof NoPlatformAuthorityException)) {
          log.error("Admin signin failed with unknown error, email={}", request.email(), e);
          publishSigninAttemptEvent(request.email(), userIdForAudit, null,
              SigninAttemptEvent.AttemptResult.FAILURE, SigninAttemptEvent.FailureReason.UNKNOWN);
        }

        metrics.recordAuthOutcome(null, "admin_login", "failure", getAuthFailureReason(e));
        throw e;
      }
    });
  }

  @Override
  public AuthenticationDtos.TokenResponse refresh(final AuthenticationDtos.RefreshTokenRequest request) {
    final String currentTenant = TenantContext.getCurrentTenant();
    return metrics.authDurationTimer(currentTenant, "refresh").record(() -> {
      try {
        final Jwt jwt;
        try {
          jwt = jwtDecoder.decode(request.refreshToken());
        } catch (final JwtException e) {
          log.warn("Token refresh failed: invalid token signature, tenantKey={}", currentTenant);
          throw new BadCredentialsException("Invalid token signature", e);
        }

        final String type = jwt.getClaimAsString(JwtClaimNames.TYPE);
        if (!JwtClaimNames.TYPE_REFRESH.equals(type)) {
          log.warn("Token refresh failed: invalid token type, tenantKey={}", currentTenant);
          throw new com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException();
        }

        final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
        if (!currentTenant.equals(tokenTenantId)) {
          log.warn("Token refresh failed: tenant mismatch, tenantKey={}, tokenTenantKey={}", currentTenant, tokenTenantId);
          throw new TenantContextMismatchException("Tenant context mismatch");
        }

        final String email = jwt.getClaimAsString(JwtClaimNames.SUB);
        final var user = userMapper.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
          log.warn("Token refresh failed: account not active, userId={}, tenantKey={}", user.getId(), currentTenant);
          throw new AccountNotActiveException();
        }

        if (banService.isUserBanned(user.getId(), currentTenant)) {
          log.warn("Token refresh failed: account banned, userId={}, tenantKey={}", user.getId(), currentTenant);
          throw new AccountBannedException();
        }

        final var membership = membershipService.resolveMembership(user.getId(), currentTenant);
        final var authorities = membershipService.getAuthorities(membership.getId());

        final String planCode = tenantMapper.findByTenantKey(currentTenant)
            .map(Tenant::getActivePlanCode).orElse(null);
        final String accessToken = jwtTokenGenerator.generateAccessToken(
            user, currentTenant, authorities, planCode);
        final String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user, currentTenant);

        log.info("Token refresh successful: userId={}, tenantKey={}", user.getId(), currentTenant);
        metrics.recordAuthOutcome(currentTenant, "refresh", "success", null);
        return new AuthenticationDtos.TokenResponse(accessToken, newRefreshToken, currentTenant);
      } catch (final Exception e) {
        log.warn("Token refresh failed, tenantKey={}", currentTenant, e);
        metrics.recordAuthOutcome(currentTenant, "refresh", "failure", getAuthFailureReason(e));
        throw e;
      }
    });
  }

  @Override
  public AuthenticationDtos.TokenResponse adminRefresh(final AuthenticationDtos.RefreshTokenRequest request) {
    return metrics.authDurationTimer(null, "admin_refresh").record(() -> {
      try {
        final Jwt jwt;
        try {
          jwt = jwtDecoder.decode(request.refreshToken());
        } catch (final JwtException e) {
          log.warn("Admin token refresh failed: invalid token signature");
          throw new BadCredentialsException("Invalid token signature", e);
        }

        final String type = jwt.getClaimAsString(JwtClaimNames.TYPE);
        if (!JwtClaimNames.TYPE_REFRESH.equals(type)) {
          log.warn("Admin token refresh failed: invalid token type");
          throw new com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException();
        }

        final String email = jwt.getClaimAsString(JwtClaimNames.SUB);
        final var user = userMapper.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
          log.warn("Admin token refresh failed: account not active, userId={}", user.getId());
          throw new AccountNotActiveException();
        }

        if (banService.isUserBanned(user.getId(), null)) {
          log.warn("Admin token refresh failed: account banned, userId={}", user.getId());
          throw new AccountBannedException();
        }

        final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(user.getId());
        if (platformAuthorities.isEmpty()) {
          log.warn("Admin token refresh failed: no platform authorities, userId={}", user.getId());
          throw new NoPlatformAuthorityException();
        }

        final String accessToken = jwtTokenGenerator.generateAccessToken(user, null, platformAuthorities);
        final String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user, null);

        log.info("Admin token refresh successful: userId={}", user.getId());
        metrics.recordAuthOutcome(null, "admin_refresh", "success", null);
        return new AuthenticationDtos.TokenResponse(accessToken, newRefreshToken, null);
      } catch (final Exception e) {
        log.warn("Admin token refresh failed", e);
        metrics.recordAuthOutcome(null, "admin_refresh", "failure", getAuthFailureReason(e));
        throw e;
      }
    });
  }

  @Override
  public void signOut(final String jti, final String userId, final String expiresAt) {
    tokenDenylistService.denyToken(jti, UUID.fromString(userId), Instant.parse(expiresAt));
    metrics.recordSecurityEvent(null, "token_revoked");
  }

  @Override
  public void signOutAll(final String userId, final String jti, final String expiresAt) {
    userMapper.updateLastGlobalSignoutAt(UUID.fromString(userId), Instant.now());
    tokenDenylistService.denyToken(jti, UUID.fromString(userId), Instant.parse(expiresAt));
    metrics.recordSecurityEvent(null, "token_revoked_all");
  }

  @Override
  @Transactional(readOnly = true)
  public AuthenticationDtos.ValidateTokenResponse validateToken(final String token) {
    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (final JwtException e) {
      metrics.recordSecurityEvent(null, "token_validation_failure");
      throw new BadCredentialsException("Invalid token", e);
    }

    final String jti = jwt.getClaimAsString(JwtClaimNames.JTI);
    if (jti != null && tokenDenylistService.isRevoked(jti)) {
      metrics.recordSecurityEvent(null, "token_revoked_check_failure");
      throw new TokenRevokedException();
    }

    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final String email = jwt.getClaimAsString(JwtClaimNames.EMAIL);
    final String tenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final List<String> authorities = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);

    return new AuthenticationDtos.ValidateTokenResponse(
        UUID.fromString(userId), email, tenantId,
        authorities != null ? authorities : List.of());
  }

  @Override
  public List<AuthenticationDtos.TenantMembershipSummary> listUserTenants(
      final String email, final String password) {

    final var user = userMapper.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (accountLockoutManager.isLocked(email)) {
      throw new AccountLockedException();
    }

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      accountLockoutManager.recordFailedAttempt(email);
      throw new BadCredentialsException("Invalid credentials");
    }

    accountLockoutManager.reset(email);
    return tenantListingService.prepareTenantList(user.getId());
  }

  @Override
  public void verifyEmail(final String token) {
    final var verificationToken = emailVerificationTokenMapper.findByToken(token)
        .orElseThrow(() -> {
          log.warn("Email verification failed: invalid token");
          return new InvalidVerificationTokenException("Invalid or expired verification token");
        });

    if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
      log.warn("Email verification failed: token expired, userId={}", verificationToken.getUserId());
      throw new InvalidVerificationTokenException("Invalid or expired verification token");
    }

    final var user = userMapper.findById(verificationToken.getUserId())
        .orElseThrow(() -> {
          log.warn("Email verification failed: user not found, userId={}", verificationToken.getUserId());
          return new UserNotFoundException("User not found");
        });

    userMapper.setEmailVerified(verificationToken.getUserId());
    emailVerificationTokenMapper.deleteByUserId(verificationToken.getUserId());

    log.info("Email verified: userId={}", user.getId());

    final String signinUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/signin";
    final var payload = new java.util.HashMap<String, Object>();
    payload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    payload.put("signinUrl", signinUrl);

    final var event = new NotificationEvent(
        user.getEmail(),
        notificationProps.defaultLocale(),
        NotificationEventType.EMAIL_VERIFIED,
        payload,
        Instant.now());
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish EMAIL_VERIFIED notification for user {}", user.getId(), e);
    }
  }

  @Override
  public void resendVerification(final String email) {
    final var userOpt = userMapper.findByEmail(email);
    if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
      return;
    }

    final var user = userOpt.get();
    final Instant windowStart = Instant.now().minus(RESEND_WINDOW);
    final int resendCount = emailVerificationTokenMapper.countResendsWithinWindow(user.getId(), windowStart);
    if (resendCount >= RESEND_RATE_LIMIT) {
      log.warn("Email verification resend failed: rate limit exceeded, userId={}", user.getId());
      throw new VerificationRateLimitException(RESEND_WINDOW.toSeconds());
    }

    emailVerificationTokenMapper.deleteByUserId(user.getId());

    final byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    final String tokenValue = HexFormat.of().formatHex(bytes);

    final var newToken = new EmailVerificationToken();
    newToken.setId(UUID.randomUUID());
    newToken.setUserId(user.getId());
    newToken.setToken(tokenValue);
    newToken.setExpiresAt(Instant.now().plus(EMAIL_TOKEN_TTL));
    newToken.setCreatedAt(Instant.now());
    emailVerificationTokenMapper.insert(newToken);

    log.info("Email verification resent: userId={}", user.getId());

    final String verificationUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/verify-email?token=" + tokenValue;
    final var resendPayload = new java.util.HashMap<String, Object>();
    resendPayload.put("verificationUrl", verificationUrl);
    resendPayload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    resendPayload.put("expiresInHours", 24);

    final var event = new NotificationEvent(
        user.getEmail(),
        notificationProps.defaultLocale(),
        NotificationEventType.VERIFY_EMAIL,
        resendPayload,
        Instant.now());
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish VERIFY_EMAIL notification for user {}", user.getId(), e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public String getProvisioningStatus(final String tenantKey) {
    return tenantService.getProvisioningStatus(tenantKey);
  }

  @Override
  public AuthenticationDtos.TokenResponse exchangeTenant(final UUID userId, final String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> {
          log.warn("Tenant exchange failed: tenant not available, userId={}, tenantKey={}", userId, tenantKey);
          return new TenantNotAvailableException("Tenant not available");
        });

    // Allow access to internal (personal) tenants even if status is SUSPENDED
    final boolean isInternalTenant = Boolean.TRUE.equals(tenant.getIsInternal());

    if (!isInternalTenant && tenant.getStatus() == TenantStatus.SUSPENDED) {
      log.warn("Tenant exchange failed: tenant suspended, userId={}, tenantKey={}", userId, tenantKey);
      throw new TenantSuspendedException("Tenant suspended");
    }
    if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.PROVISIONING_FAILED) {
      log.warn("Tenant exchange failed: tenant not available, userId={}, tenantKey={}", userId, tenantKey);
      throw new TenantNotAvailableException("Tenant not available");
    }

    final var user = userMapper.findById(userId)
        .orElseThrow(() -> {
          log.warn("Tenant exchange failed: user not found, userId={}", userId);
          return new UserNotFoundException("User not found");
        });
    if (user.getStatus() != AccountStatus.ACTIVE) {
      log.warn("Tenant exchange failed: account not active, userId={}, tenantKey={}", userId, tenantKey);
      throw new AccountNotActiveException();
    }

    if (banService.isUserBanned(userId, tenantKey)) {
      log.warn("Tenant exchange failed: account banned, userId={}, tenantKey={}", userId, tenantKey);
      throw new AccountBannedException();
    }

    final var membership = membershipService.resolveMembership(userId, tenantKey);
    final var authorities = membershipService.getAuthorities(membership.getId());

    final String accessToken = jwtTokenGenerator.generateAccessToken(
        user, tenantKey, authorities, tenant.getActivePlanCode());
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, tenantKey);

    log.info("Tenant exchange successful: userId={}, tenantKey={}", userId, tenantKey);
    return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, tenantKey);
  }

  /**
   * Publishes a signin attempt event for audit logging.
   * IP address and user agent will be automatically enriched by AuditEventEnricher.
   */
  private void publishSigninAttemptEvent(final String email, final UUID userId, final String tenantKey,
                                         final SigninAttemptEvent.AttemptResult result,
                                         final SigninAttemptEvent.FailureReason failureReason) {
    try {
      final var event = new SigninAttemptEvent(
          email,
          userId,
          tenantKey,
          result,
          failureReason,
          null, // IP address will be enriched by AuditEventEnricher
          null, // User agent will be enriched by AuditEventEnricher
          Instant.now()
      );
      messagingService.publishSigninAttempt(event);
    } catch (final Exception e) {
      log.warn("Failed to publish signin attempt event for email={}, result={}", email, result, e);
      // Don't fail the signin process if audit event publishing fails
    }
  }
}
