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

package com.iqkv.foundation.iamservice.authentication;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.denylist.TokenDenylistService;
import com.iqkv.foundation.iamservice.email.EmailVerificationToken;
import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
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
import com.iqkv.foundation.iamservice.tenancy.TenantContext;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantService;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.UserMapper;
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
      final TenantService tenantService) {
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
  }

  @Override
  public AuthenticationDtos.TokenResponse signIn(final AuthenticationDtos.SignInRequest request) {
    final String tenantKey = TenantContext.getCurrentTenant();

    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotAvailableException("Tenant not available"));

    if (tenant.getStatus() == TenantStatus.SUSPENDED) {
      throw new TenantSuspendedException("Tenant suspended");
    }
    if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.PROVISIONING_FAILED) {
      throw new TenantNotAvailableException("Tenant not available");
    }

    final var user = userMapper.findByEmail(request.email())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (accountLockoutManager.isLocked(request.email())) {
      throw new AccountLockedException();
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      accountLockoutManager.recordFailedAttempt(request.email());
      throw new BadCredentialsException("Invalid credentials");
    }

    final var membership = membershipService.resolveMembership(user.getId(), tenantKey);
    final var authorities = membershipService.getAuthorities(membership.getId());

    accountLockoutManager.reset(request.email());

    final String accessToken = jwtTokenGenerator.generateAccessToken(user, tenantKey, authorities);
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, tenantKey);

    return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, tenantKey);
  }

  @Override
  public AuthenticationDtos.TokenResponse adminSignIn(final AuthenticationDtos.SignInRequest request) {
    final var user = userMapper.findByEmail(request.email())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (accountLockoutManager.isLocked(request.email())) {
      throw new AccountLockedException();
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      accountLockoutManager.recordFailedAttempt(request.email());
      throw new BadCredentialsException("Invalid credentials");
    }

    final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(user.getId());
    if (platformAuthorities.isEmpty()) {
      // Record the failed attempt to prevent user enumeration via timing differences,
      // then surface a 403 — not a 401 — so the admin UI can show a clear "no access" message.
      accountLockoutManager.reset(request.email());
      throw new NoPlatformAuthorityException();
    }

    accountLockoutManager.reset(request.email());

    // tenant_id is null for platform-scoped tokens — no tenant context applies.
    final String accessToken = jwtTokenGenerator.generateAccessToken(user, null, platformAuthorities);
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, null);

    return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, null);
  }

  @Override
  public AuthenticationDtos.TokenResponse refresh(final AuthenticationDtos.RefreshTokenRequest request) {
    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(request.refreshToken());
    } catch (final JwtException e) {
      throw new BadCredentialsException("Invalid token signature", e);
    }

    final String type = jwt.getClaimAsString(JwtClaimNames.TYPE);
    if (!JwtClaimNames.TYPE_REFRESH.equals(type)) {
      throw new com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException();
    }

    final String tokenTenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final String currentTenant = TenantContext.getCurrentTenant();
    if (!currentTenant.equals(tokenTenantId)) {
      throw new TenantContextMismatchException("Tenant context mismatch");
    }

    final String email = jwt.getClaimAsString(JwtClaimNames.SUB);
    final var user = userMapper.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    final var membership = membershipService.resolveMembership(user.getId(), currentTenant);
    final var authorities = membershipService.getAuthorities(membership.getId());

    final String accessToken = jwtTokenGenerator.generateAccessToken(user, currentTenant, authorities);
    final String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user, currentTenant);

    return new AuthenticationDtos.TokenResponse(accessToken, newRefreshToken, currentTenant);
  }

  @Override
  public AuthenticationDtos.TokenResponse adminRefresh(final AuthenticationDtos.RefreshTokenRequest request) {
    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(request.refreshToken());
    } catch (final JwtException e) {
      throw new BadCredentialsException("Invalid token signature", e);
    }

    final String type = jwt.getClaimAsString(JwtClaimNames.TYPE);
    if (!JwtClaimNames.TYPE_REFRESH.equals(type)) {
      throw new com.iqkv.foundation.iamservice.shared.exception.InvalidTokenTypeException();
    }

    final String email = jwt.getClaimAsString(JwtClaimNames.SUB);
    final var user = userMapper.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(user.getId());
    if (platformAuthorities.isEmpty()) {
      throw new NoPlatformAuthorityException();
    }

    final String accessToken = jwtTokenGenerator.generateAccessToken(user, null, platformAuthorities);
    final String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user, null);

    return new AuthenticationDtos.TokenResponse(accessToken, newRefreshToken, null);
  }

  @Override
  public void signOut(final String jti, final String userId, final String expiresAt) {
    tokenDenylistService.denyToken(jti, UUID.fromString(userId), Instant.parse(expiresAt));
  }

  @Override
  public void signOutAll(final String userId, final String jti, final String expiresAt) {
    userMapper.updateLastGlobalSignoutAt(UUID.fromString(userId), Instant.now());
    tokenDenylistService.denyToken(jti, UUID.fromString(userId), Instant.parse(expiresAt));
  }

  @Override
  @Transactional(readOnly = true)
  public AuthenticationDtos.ValidateTokenResponse validateToken(final String token) {
    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (final JwtException e) {
      throw new BadCredentialsException("Invalid token", e);
    }

    final String jti = jwt.getClaimAsString(JwtClaimNames.JTI);
    if (jti != null && tokenDenylistService.isRevoked(jti)) {
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

    final List<TenantMembership> memberships = membershipMapper.findByUserId(user.getId());
    final List<AuthenticationDtos.TenantMembershipSummary> result = new ArrayList<>();

    for (final TenantMembership membership : memberships) {
      if (membership.getStatus() != com.iqkv.foundation.iamservice.membership.MembershipStatus.ACTIVE) {
        continue;
      }
      final var tenant = tenantMapper.findByTenantKey(membership.getTenantKey()).orElse(null);
      if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
        continue;
      }
      final var authorities = membershipService.getAuthorities(membership.getId());
      result.add(new AuthenticationDtos.TenantMembershipSummary(
          tenant.getTenantKey(),
          tenant.getName(),
          membership.getStatus().name(),
          authorities));
    }

    return result;
  }

  @Override
  public AuthenticationDtos.TokenResponse exchangeTenant(final UUID userId, final String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotAvailableException("Tenant not available"));

    if (tenant.getStatus() == TenantStatus.SUSPENDED) {
      throw new TenantSuspendedException("Tenant suspended");
    }
    if (tenant.getStatus() != TenantStatus.ACTIVE) {
      throw new TenantNotAvailableException("Tenant not available");
    }

    final var user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    final var membership = membershipService.resolveMembership(userId, tenantKey);
    final var authorities = membershipService.getAuthorities(membership.getId());

    final String accessToken = jwtTokenGenerator.generateAccessToken(user, tenantKey, authorities);
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, tenantKey);
    return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, tenantKey);
  }

  @Override
  public void verifyEmail(final String token) {
    final var verificationToken = emailVerificationTokenMapper.findByToken(token)
        .orElseThrow(() -> new InvalidVerificationTokenException("Invalid or expired verification token"));

    if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
      throw new InvalidVerificationTokenException("Invalid or expired verification token");
    }

    final var user = userMapper.findById(verificationToken.getUserId())
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    userMapper.setEmailVerified(verificationToken.getUserId());
    emailVerificationTokenMapper.deleteByUserId(verificationToken.getUserId());

    final String signinUrl = notificationProps.baseUrl() + "/signin";
    final var event = new NotificationEvent(
        user.getEmail(),
        notificationProps.defaultLocale(),
        NotificationEventType.EMAIL_VERIFIED,
        Map.of("firstName", user.getFirstName(), "signinUrl", signinUrl),
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

    final String verificationUrl = notificationProps.baseUrl() + "/verify-email?token=" + tokenValue;
    final var event = new NotificationEvent(
        user.getEmail(),
        notificationProps.defaultLocale(),
        NotificationEventType.VERIFY_EMAIL,
        Map.of("verificationUrl", verificationUrl, "firstName", user.getFirstName(), "expiresInHours", 24),
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
}
