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

package com.iqkv.foundation.iamservice.magiclink;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.JwtTokenGenerator;
import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos.TokenResponse;
import com.iqkv.foundation.iamservice.ban.BanService;
import com.iqkv.foundation.iamservice.infrastructure.config.AuthConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MagicLinkEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.infrastructure.messaging.UserEventPublisher;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthority;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.plan.PlanCatalogCache;
import com.iqkv.foundation.iamservice.shared.exception.AccountBannedException;
import com.iqkv.foundation.iamservice.shared.exception.AccountNotActiveException;
import com.iqkv.foundation.iamservice.shared.exception.MagicLinkRateLimitException;
import com.iqkv.foundation.iamservice.shared.exception.MagicLinkTokenNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.PlanMemberQuotaException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotAvailableException;
import com.iqkv.foundation.iamservice.shared.exception.TenantSuspendedException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.util.UserServiceConstants;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MagicLinkServiceImpl implements MagicLinkService {

  private static final Logger log = LoggerFactory.getLogger(MagicLinkServiceImpl.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String PLATFORM_TENANT_KEY = "platform";

  private final UserMapper userMapper;
  private final TenantMapper tenantMapper;
  private final MagicLinkTokenMapper magicLinkTokenMapper;
  private final MembershipService membershipService;
  private final BanService banService;
  private final JwtTokenGenerator jwtTokenGenerator;
  private final MessagingService messagingService;
  private final AuthConfigurationProperties authProps;
  private final NotificationConfigurationProperties notificationProps;
  private final IamServiceMetrics metrics;
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final PlanCatalogCache planCatalogCache;
  private final PasswordEncoder passwordEncoder;
  private final UserEventPublisher userEventPublisher;

  public MagicLinkServiceImpl(
      final UserMapper userMapper,
      final TenantMapper tenantMapper,
      final MagicLinkTokenMapper magicLinkTokenMapper,
      final MembershipService membershipService,
      final BanService banService,
      final JwtTokenGenerator jwtTokenGenerator,
      final MessagingService messagingService,
      final AuthConfigurationProperties authProps,
      final NotificationConfigurationProperties notificationProps,
      final IamServiceMetrics metrics,
      final TenantMembershipMapper membershipMapper,
      final TenantMemberAuthorityMapper authorityMapper,
      final PlanCatalogCache planCatalogCache,
      final PasswordEncoder passwordEncoder,
      final UserEventPublisher userEventPublisher) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.magicLinkTokenMapper = magicLinkTokenMapper;
    this.membershipService = membershipService;
    this.banService = banService;
    this.jwtTokenGenerator = jwtTokenGenerator;
    this.messagingService = messagingService;
    this.authProps = authProps;
    this.notificationProps = notificationProps;
    this.metrics = metrics;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.planCatalogCache = planCatalogCache;
    this.passwordEncoder = passwordEncoder;
    this.userEventPublisher = userEventPublisher;
  }

  @Override
  public void initiate(final String email, String tenantKey) {
    User user;
    final var userOpt = userMapper.findByEmail(email);
    final boolean isNewUser;
    if (userOpt.isEmpty()) {
      // Auto-create new user if they don't exist
      user = new User();
      user.setId(UUID.randomUUID());
      user.setEmail(email);
      user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
      user.setStatus(com.iqkv.foundation.iamservice.user.AccountStatus.ACTIVE);
      user.setEmailVerified(false);
      user.setLocale(notificationProps.defaultLocale());
      user.setCreatedAt(LocalDateTime.now());
      user.setUpdatedAt(LocalDateTime.now());
      user.setCreatedBy("system");
      user.setUpdatedBy("system");
      userMapper.upsertByEmail(user);
      // Retrieve the canonical user after upsert
      user = userMapper.findByEmail(email)
          .orElseThrow(() -> new com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException(
              "User not found after upsert: " + email));
      isNewUser = true;
    } else {
      user = userOpt.get();
      isNewUser = false;
    }

    if (isNewUser) {
      // Publish user created event
      try {
        userEventPublisher.publishUserCreated(user);
      } catch (final Exception e) {
        log.warn("Failed to publish USER_CREATED audit event for userId={}", user.getId(), e);
      }
      metrics.recordUserLifecycleEvent("registered");
    }

    // Determine the final tenant key
    final String resolvedTenantKey;
    if (tenantKey == null || tenantKey.isBlank()) {
      // If not provided, use platform tenant
      resolvedTenantKey = PLATFORM_TENANT_KEY;
    } else {
      resolvedTenantKey = tenantKey;
    }

    // Validate tenant
    final var tenantOpt = tenantMapper.findByTenantKey(resolvedTenantKey);
    if (tenantOpt.isEmpty()) {
      return; // prevent enumeration
    }

    final var tenant = tenantOpt.get();
    final boolean isInternalTenant = Boolean.TRUE.equals(tenant.getIsInternal());
    if (!isInternalTenant && tenant.getStatus() == TenantStatus.SUSPENDED) {
      return;
    }
    if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.PROVISIONING_FAILED) {
      return;
    }

    // Ensure user has platform membership (personal workspace) regardless of target tenant
    ensurePlatformMembership(user);

    // Check if user is a member, banned or inactive? Well, to prevent enumeration, we don't expose that
    final Instant windowStart = Instant.now().minus(authProps.magicLink().rateLimitWindow());
    final int count = magicLinkTokenMapper.countResendsWithinWindow(user.getId(), windowStart);
    if (count >= authProps.magicLink().rateLimitMaxRequests()) {
      throw new MagicLinkRateLimitException(authProps.magicLink().rateLimitWindow().toSeconds());
    }

    magicLinkTokenMapper.deleteByUserId(user.getId());

    final byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    final String tokenValue = HexFormat.of().formatHex(bytes);

    final var mlt = new MagicLinkToken();
    mlt.setId(UUID.randomUUID());
    mlt.setUserId(user.getId());
    mlt.setTenantKey(resolvedTenantKey);
    mlt.setToken(tokenValue);
    mlt.setExpiresAt(Instant.now().plus(authProps.magicLink().tokenTtl()));
    mlt.setLastResendAt(Instant.now());
    mlt.setCreatedAt(Instant.now());
    magicLinkTokenMapper.insert(mlt);
    metrics.recordUserLifecycleEvent("magic_link_initiated");

    // Audit event — fire-and-forget
    try {
      final var mlEvent = new MagicLinkEvent(
          user.getId(),
          user.getEmail(),
          resolvedTenantKey,
          MagicLinkEvent.EventType.MAGIC_LINK_INITIATED,
          null,
          Instant.now());
      messagingService.publishMagicLinkEvent(mlEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish MAGIC_LINK_INITIATED audit event for userId={}", user.getId(), e);
    }

    // Send email notification
    sendMagicLinkEmail(email, user, tokenValue);
  }

  @Override
  public void resend(final String email, String tenantKey) {
    final var userOpt = userMapper.findByEmail(email);
    if (userOpt.isEmpty()) {
      return; // prevent enumeration
    }

    final User user = userOpt.get();

    // Determine the final tenant key
    final String resolvedTenantKey;
    if (tenantKey == null || tenantKey.isBlank()) {
      // If not provided, use platform tenant
      resolvedTenantKey = PLATFORM_TENANT_KEY;
    } else {
      resolvedTenantKey = tenantKey;
    }

    // Find existing magic link token for user and tenant
    final var existingTokenOpt = magicLinkTokenMapper.findByUserIdAndTenantKey(user.getId(), resolvedTenantKey);
    if (existingTokenOpt.isEmpty()) {
      return; // prevent enumeration
    }

    final var existingToken = existingTokenOpt.get();

    // Check rate limit
    final Instant windowStart = Instant.now().minus(authProps.magicLink().rateLimitWindow());
    final int count = magicLinkTokenMapper.countResendsWithinWindow(user.getId(), windowStart);
    if (count >= authProps.magicLink().rateLimitMaxRequests()) {
      throw new MagicLinkRateLimitException(authProps.magicLink().rateLimitWindow().toSeconds());
    }

    // Increment resend count and update last resend time
    magicLinkTokenMapper.incrementResendCount(user.getId(), Instant.now());
    metrics.recordUserLifecycleEvent("magic_link_resent");

    // Audit event — fire-and-forget
    try {
      final var mlEvent = new MagicLinkEvent(
          user.getId(),
          user.getEmail(),
          resolvedTenantKey,
          MagicLinkEvent.EventType.MAGIC_LINK_INITIATED,
          null,
          Instant.now());
      messagingService.publishMagicLinkEvent(mlEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish MAGIC_LINK_INITIATED audit event for userId={}", user.getId(), e);
    }

    // Send email notification with existing token
    sendMagicLinkEmail(email, user, existingToken.getToken());
  }

  /**
   * Ensures the user has an active membership in the platform tenant with MEMBER authority.
   * Idempotent - skips creation if membership already exists.
   */
  private void ensurePlatformMembership(final User user) {
    if (!membershipMapper.existsByUserIdAndTenantKey(user.getId(), PLATFORM_TENANT_KEY)) {
      // Quota check: enforce maxUsers from the active plan (0 = unlimited)
      final var platformTenant = tenantMapper.findByTenantKey(PLATFORM_TENANT_KEY)
          .orElseThrow(() -> new IllegalStateException("Platform tenant not found"));
      final int maxUsers = planCatalogCache.forPlan(platformTenant.getActivePlanCode()).maxUsers();
      if (maxUsers > 0) {
        final long current = membershipMapper.countByTenantKey(PLATFORM_TENANT_KEY);
        if (current >= maxUsers) {
          throw new PlanMemberQuotaException(current, maxUsers);
        }
      }
      log.info("Adding user {} to platform tenant", user.getId());

      final var platformMembership = new TenantMembership();
      platformMembership.setId(UUID.randomUUID());
      platformMembership.setUserId(user.getId());
      platformMembership.setTenantKey(PLATFORM_TENANT_KEY);
      platformMembership.setStatus(MembershipStatus.ACTIVE);
      platformMembership.setCreatedAt(LocalDateTime.now());
      platformMembership.setUpdatedAt(LocalDateTime.now());
      platformMembership.setCreatedBy(user.getId().toString());
      platformMembership.setUpdatedBy(user.getId().toString());
      membershipMapper.insert(platformMembership);

      final var platformAuthority = new TenantMemberAuthority();
      platformAuthority.setId(UUID.randomUUID());
      platformAuthority.setMembershipId(platformMembership.getId());
      platformAuthority.setAuthority(UserServiceConstants.AUTHORITY_MEMBER);
      authorityMapper.insert(platformAuthority);
    }
  }

  private void sendMagicLinkEmail(String email, User user, String tokenValue) {
    final String magicLinkUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/magic-link/verify?token=" + tokenValue;
    final var payload = new java.util.HashMap<String, Object>();
    payload.put("magicLinkUrl", magicLinkUrl);
    payload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    payload.put("token", tokenValue);

    final var event = new NotificationEvent(
        email,
        notificationProps.defaultLocale(),
        NotificationEventType.MAGIC_LINK_SENT,
        payload,
        Instant.now());
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish MAGIC_LINK_SENT notification for user {}", user.getId(), e);
    }
  }

  @Override
  public TokenResponse exchange(final String token) {
    final var mlt = magicLinkTokenMapper.findByToken(token)
        .orElseThrow(MagicLinkTokenNotFoundException::new);

    if (mlt.getExpiresAt().isBefore(Instant.now())) {
      throw new MagicLinkTokenNotFoundException();
    }

    final var user = userMapper.findById(mlt.getUserId())
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    if (user.getStatus() != com.iqkv.foundation.iamservice.user.AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (banService.isUserBanned(user.getId(), mlt.getTenantKey())) {
      throw new AccountBannedException();
    }

    final var tenant = tenantMapper.findByTenantKey(mlt.getTenantKey())
        .orElseThrow(() -> new TenantNotAvailableException("Tenant not available"));
    final boolean isInternalTenant = Boolean.TRUE.equals(tenant.getIsInternal());
    if (!isInternalTenant && tenant.getStatus() == TenantStatus.SUSPENDED) {
      throw new TenantSuspendedException("Tenant suspended");
    }
    if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.PROVISIONING_FAILED) {
      throw new TenantNotAvailableException("Tenant not available");
    }

    final var membership = membershipService.resolveMembership(user.getId(), mlt.getTenantKey());
    final var authorities = membershipService.getAuthorities(membership.getId());

    magicLinkTokenMapper.deleteByToken(token);
    userMapper.setFirstSignInAt(user.getId(), Instant.now());
    metrics.recordUserLifecycleEvent("magic_link_exchanged");

    // Audit event — fire-and-forget
    try {
      final var mlEvent = new MagicLinkEvent(
          user.getId(),
          user.getEmail(),
          mlt.getTenantKey(),
          MagicLinkEvent.EventType.MAGIC_LINK_EXCHANGED,
          null,
          Instant.now());
      messagingService.publishMagicLinkEvent(mlEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish MAGIC_LINK_EXCHANGED audit event for userId={}", user.getId(), e);
    }

    final String accessToken = jwtTokenGenerator.generateAccessToken(
        user, mlt.getTenantKey(), authorities, tenant.getActivePlanCode());
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, mlt.getTenantKey());

    return new TokenResponse(accessToken, refreshToken, mlt.getTenantKey());
  }
}

