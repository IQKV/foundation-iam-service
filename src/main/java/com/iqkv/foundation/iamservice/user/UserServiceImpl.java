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

package com.iqkv.foundation.iamservice.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.iqkv.foundation.iamservice.email.EmailVerificationToken;
import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.infrastructure.messaging.UserEventPublisher;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.shared.exception.InvalidAccountStatusException;
import com.iqkv.foundation.iamservice.shared.exception.InvalidPasswordException;
import com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.signup.SignupStrategy;
import com.iqkv.foundation.iamservice.user.dto.UserDtoMapper;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

  private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Pattern PASSWORD_PATTERN =
      Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$");

  private final UserMapper userMapper;
  private final TenantMembershipMapper membershipMapper;
  private final EmailVerificationTokenMapper emailVerificationTokenMapper;
  private final MessagingService messagingService;
  private final UserEventPublisher userEventPublisher;
  private final NotificationConfigurationProperties notificationProps;
  private final SignupStrategy signupStrategy;
  private final PasswordEncoder passwordEncoder;
  private final IamServiceMetrics metrics;
  private final AccountLockoutManager accountLockoutManager;

  public UserServiceImpl(final UserMapper userMapper,
                         final TenantMembershipMapper membershipMapper,
                         final EmailVerificationTokenMapper emailVerificationTokenMapper,
                         final MessagingService messagingService,
                         final UserEventPublisher userEventPublisher,
                         final NotificationConfigurationProperties notificationProps,
                         final SignupStrategy signupStrategy,
                         final PasswordEncoder passwordEncoder,
                         final IamServiceMetrics metrics,
                         final AccountLockoutManager accountLockoutManager) {
    this.userMapper = userMapper;
    this.membershipMapper = membershipMapper;
    this.emailVerificationTokenMapper = emailVerificationTokenMapper;
    this.messagingService = messagingService;
    this.userEventPublisher = userEventPublisher;
    this.notificationProps = notificationProps;
    this.signupStrategy = signupStrategy;
    this.passwordEncoder = passwordEncoder;
    this.metrics = metrics;
    this.accountLockoutManager = accountLockoutManager;
  }

  @Override
  public UserDtos.SignupResponse registerUser(final UserDtos.RegisterUserRequest request) {
    // Delegate mode-specific signup logic (user upsert, tenant resolution/creation,
    // membership creation, authority grant) to the active SignupStrategy.
    final var result = signupStrategy.execute(request);

    final var canonicalUser = result.user();

    // Publish user.created event (mode-independent, requirement 4.7)
    userEventPublisher.publishUserCreated(canonicalUser);

    // Generate email verification token (mode-independent, requirement 4.7)
    final byte[] tokenBytes = new byte[32];
    SECURE_RANDOM.nextBytes(tokenBytes);
    final String tokenHex = HexFormat.of().formatHex(tokenBytes);

    final var verificationToken = new EmailVerificationToken();
    verificationToken.setId(UUID.randomUUID());
    verificationToken.setUserId(canonicalUser.getId());
    verificationToken.setToken(tokenHex);
    verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
    verificationToken.setResendCount(0);
    verificationToken.setCreatedAt(Instant.now());
    emailVerificationTokenMapper.insert(verificationToken);

    // Publish verification email notification (mode-independent, requirement 4.7)
    final String verificationUrl = notificationProps.baseUrl()
                                   + "/api/v1/iam/users/email/verify?token=" + tokenHex;
    final var verificationPayload = new java.util.HashMap<String, Object>();
    verificationPayload.put("verificationUrl", verificationUrl);
    verificationPayload.put("firstName", canonicalUser.getFirstName() != null ? canonicalUser.getFirstName() : "");
    verificationPayload.put("expiresInHours", 24);

    final var notificationEvent = new NotificationEvent(
        canonicalUser.getEmail(),
        notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en",
        NotificationEventType.VERIFY_EMAIL,
        verificationPayload,
        Instant.now());
    messagingService.publishNotification(notificationEvent);

    // Send tenant-owner welcome email when the user becomes a TENANT_OWNER (MULTI_TENANT signup).
    // The email is fire-and-forget — a failure must never roll back the registration transaction.
    if (result.authorities().contains(com.iqkv.foundation.iamservice.shared.util.UserServiceConstants.AUTHORITY_TENANT_OWNER)) {
      try {
        final String dashboardUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/dashboard";
        final var welcomePayload = new java.util.HashMap<String, Object>();
        welcomePayload.put("firstName", canonicalUser.getFirstName() != null ? canonicalUser.getFirstName() : "");
        welcomePayload.put("tenantName", result.tenant().getName() != null ? result.tenant().getName() : "");
        welcomePayload.put("tenantKey", result.tenant().getTenantKey());
        welcomePayload.put("dashboardUrl", dashboardUrl);

        final var welcomeEvent = new NotificationEvent(
            canonicalUser.getId(),
            canonicalUser.getEmail(),
            notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en",
            NotificationEventType.TENANT_OWNER_WELCOME,
            welcomePayload,
            Instant.now());
        messagingService.publishNotification(welcomeEvent);
      } catch (final Exception e) {
        log.warn("Failed to publish TENANT_OWNER_WELCOME notification for userId={}", canonicalUser.getId(), e);
      }
    }

    // Send member welcome notification when the user joins as a regular MEMBER (SINGLE_TENANT signup).
    // Uses the user's preferred locale if set, falling back to the platform default.
    // Fire-and-forget — must not affect the registration transaction.
    if (result.authorities().contains(com.iqkv.foundation.iamservice.shared.util.UserServiceConstants.AUTHORITY_MEMBER)) {
      try {
        final String userLocale = canonicalUser.getLocale() != null && !canonicalUser.getLocale().isBlank()
            ? canonicalUser.getLocale()
            : (notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en");
        final String dashboardUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/dashboard";
        final var memberWelcomePayload = new java.util.HashMap<String, Object>();
        memberWelcomePayload.put("firstName", canonicalUser.getFirstName() != null ? canonicalUser.getFirstName() : "");
        memberWelcomePayload.put("tenantName", result.tenant().getName() != null ? result.tenant().getName() : "");
        memberWelcomePayload.put("dashboardUrl", dashboardUrl);

        final var memberWelcomeEvent = new NotificationEvent(
            canonicalUser.getId(),
            canonicalUser.getEmail(),
            userLocale,
            NotificationEventType.MEMBER_WELCOME,
            memberWelcomePayload,
            Instant.now());
        messagingService.publishNotification(memberWelcomeEvent);
      } catch (final Exception e) {
        log.warn("Failed to publish MEMBER_WELCOME notification for userId={}", canonicalUser.getId(), e);
      }
    }

    log.info("User registered: userId={}, tenantKey={}", canonicalUser.getId(), result.tenant().getTenantKey());
    metrics.recordUserLifecycleEvent("registered");
    return UserDtoMapper.toSignupResponse(canonicalUser, result.tenant());
  }

  @Override
  @Transactional(readOnly = true)
  public UserDtos.UserCountResponse countUsers() {
    return new UserDtos.UserCountResponse(userMapper.countAll(null, null, Boolean.FALSE));
  }

  @Override
  @Transactional(readOnly = true)
  public UserDtos.PagedUserResponse listUsers(final UserDtos.UserListQuery query) {
    // Allowlist validation — prevents any unsanitised value reaching the ${}
    // substitution in UserMapper.xml. The <choose> block is the last line of
    // defence; this guard makes the intent explicit and testable.
    final String safeSortBy = java.util.Set.of("email", "firstName", "lastName", "updatedAt", "createdAt")
        .contains(query.sortBy()) ? query.sortBy() : "createdAt";
    final String safeSortDir = "asc".equalsIgnoreCase(query.sortDir()) ? "asc" : "desc";

    // Normalise filter params — blank/null treated as "no filter".
    final String safeSearch = (query.search() != null && !query.search().isBlank())
        ? query.search().strip() : null;
    final String safeStatus = java.util.Arrays.stream(AccountStatus.values())
        .map(Enum::name)
        .filter(name -> name.equalsIgnoreCase(query.status()))
        .findFirst()
        .orElse(null);

    final int offset = query.page() * query.size();
    final Boolean excludeAdmins = query.excludePlatformAdmins() != null ? query.excludePlatformAdmins() : Boolean.FALSE;

    final var users = userMapper.findAll(query.size(), offset, safeSortBy, safeSortDir, safeSearch, safeStatus, excludeAdmins).stream()
        .map(UserDtoMapper::toResponse)
        .toList();
    final long total = userMapper.countAll(safeSearch, safeStatus, excludeAdmins);
    final int totalPages = (int) Math.ceil((double) total / query.size());
    return new UserDtos.PagedUserResponse(users, query.page(), query.size(), total, totalPages);
  }

  @Override
  public UserDtos.UserResponse createUser(final UserDtos.AdminCreateUserRequest request) {
    final var user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setLocale(request.locale());
    user.setStatus(AccountStatus.ACTIVE);
    user.setEmailVerified(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setCreatedBy("system");
    user.setUpdatedBy("system");
    userMapper.upsertByEmail(user);
    final User created = userMapper.findByEmail(request.email())
        .orElseThrow(() -> new UserNotFoundException("User not found after insert: " + request.email()));
    try {
      userEventPublisher.publishUserCreated(created);
    } catch (final Exception e) {
      log.warn("Failed to publish USER_CREATED audit event for userId={}", created.getId(), e);
    }
    return UserDtoMapper.toResponse(created);
  }

  @Override
  public UserDtos.UserResponse patchUser(final UUID userId, final UserDtos.AdminUpdateUserRequest request) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    if (request.firstName() != null) {
      user.setFirstName(request.firstName());
    }
    if (request.lastName() != null) {
      user.setLastName(request.lastName());
    }
    if (request.locale() != null) {
      user.setLocale(request.locale());
    }
    final String previousStatus = user.getStatus() != null ? user.getStatus().name() : null;
    if (request.status() != null) {
      try {
        user.setStatus(AccountStatus.valueOf(request.status()));
      } catch (final IllegalArgumentException e) {
        final String[] allowed = java.util.Arrays.stream(AccountStatus.values())
            .map(Enum::name)
            .toArray(String[]::new);
        throw new InvalidAccountStatusException(request.status(), allowed);
      }
    }
    user.setUpdatedAt(LocalDateTime.now());
    user.setUpdatedBy("system");
    userMapper.update(user);
    // Publish status-change event when the status field was actually mutated
    try {
      if (request.status() != null && !request.status().equalsIgnoreCase(previousStatus)) {
        userEventPublisher.publishUserStatusChanged(user, request.status());
      } else {
        userEventPublisher.publishUserUpdated(user);
      }
    } catch (final Exception e) {
      log.warn("Failed to publish user mutation audit event for userId={}", userId, e);
    }
    return UserDtoMapper.toResponse(user);
  }

  @Override
  public void deleteUserById(final UUID userId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    userMapper.deleteById(userId);
    log.info("User deleted: userId={}", userId);
    try {
      userEventPublisher.publishUserDeleted(user);
    } catch (final Exception e) {
      log.warn("Failed to publish USER_DELETED audit event for userId={}", userId, e);
    }
  }

  @Override
  public void setUserPassword(final UUID userId, final String newPassword, final String actorId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128
        || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
      throw new InvalidPasswordException("New password does not meet the password policy requirements");
    }

    final String newHash = passwordEncoder.encode(newPassword);
    userMapper.updatePassword(userId, newHash, Instant.now());

    // Invalidate all existing sessions for the target user
    userMapper.updateLastGlobalSignoutAt(userId, Instant.now());
    log.info("Password changed by admin: userId={}, actorId={}", userId, actorId);

    // Audit event — fire-and-forget
    try {
      final UUID actorUuid = actorId != null ? UUID.fromString(actorId) : null;
      final var pwEvent = new com.iqkv.foundation.iamservice.infrastructure.messaging.PasswordEvent(
          userId, user.getEmail(), null,
          com.iqkv.foundation.iamservice.infrastructure.messaging.PasswordEvent.EventType.PASSWORD_CHANGED_BY_ADMIN,
          actorUuid, Instant.now());
      messagingService.publishPasswordEvent(pwEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_CHANGED_BY_ADMIN audit event for userId={}", userId, e);
    }

    // Notify the user — fire-and-forget, must not affect the operation outcome
    try {
      final var event = new NotificationEvent(
          user.getEmail(),
          notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en",
          NotificationEventType.PASSWORD_CHANGED,
          Map.of(
              "firstName", user.getFirstName() != null ? user.getFirstName() : "",
              "changedByAdmin", true),
          Instant.now());
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_CHANGED notification for userId={}", userId, e);
    }
  }

  @Override
  public void changePassword(final UUID userId, final String currentPassword, final String newPassword) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    // Re-authenticate: verify the supplied current password
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new org.springframework.security.authentication.BadCredentialsException("Current password is incorrect");
    }

    // Validate new password strength (same policy as signup / password reset)
    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128
        || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
      throw new InvalidPasswordException("New password does not meet the password policy requirements");
    }

    final String newHash = passwordEncoder.encode(newPassword);
    userMapper.updatePassword(userId, newHash, Instant.now());

    // Invalidate all existing sessions — same pattern as password reset
    userMapper.updateLastGlobalSignoutAt(userId, Instant.now());
    log.info("Password changed by user: userId={}", userId);

    // Audit event — fire-and-forget
    try {
      final var pwEvent = new com.iqkv.foundation.iamservice.infrastructure.messaging.PasswordEvent(
          userId, user.getEmail(), null,
          com.iqkv.foundation.iamservice.infrastructure.messaging.PasswordEvent.EventType.PASSWORD_CHANGED_SELF,
          userId, Instant.now());
      messagingService.publishPasswordEvent(pwEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_CHANGED_SELF audit event for userId={}", userId, e);
    }

    // Notify the user — fire-and-forget, must not affect the operation outcome
    try {
      final var event = new NotificationEvent(
          user.getEmail(),
          notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en",
          NotificationEventType.PASSWORD_CHANGED,
          Map.of(
              "firstName", user.getFirstName() != null ? user.getFirstName() : "",
              "changedByAdmin", false),
          Instant.now());
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_CHANGED notification for userId={}", userId, e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public UserDtos.UserResponse getUserById(final UUID userId) {
    return userMapper.findByIdWithAuthorities(userId)
        .map(UserDtoMapper::toResponse)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  @Override
  public UserDtos.UserResponse updateUser(final UUID userId, final String firstName,
                                          final String lastName, final String locale,
                                          final String updatedBy) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    user.setFirstName(firstName);
    user.setLastName(lastName);
    if (locale != null) {
      user.setLocale(locale);
    }
    user.setUpdatedAt(LocalDateTime.now());
    user.setUpdatedBy(updatedBy);
    userMapper.update(user);
    try {
      userEventPublisher.publishUserUpdated(user);
    } catch (final Exception e) {
      log.warn("Failed to publish USER_UPDATED audit event for userId={}", userId, e);
    }
    return UserDtoMapper.toResponse(user);
  }

  @Override
  public void deleteUser(final UUID userId, final String tenantKey) {
    final TenantMembership membership = membershipMapper.findByUserIdAndTenantKey(userId, tenantKey)
        .orElseThrow(() -> new MembershipNotFoundException(userId, tenantKey));
    membershipMapper.deleteById(membership.getId());

    userMapper.findById(userId).ifPresent(user -> userEventPublisher.publishUserRemoved(user, tenantKey));
    log.info("User membership removed: userId={}, tenantKey={}", userId, tenantKey);
  }

  @Override
  public void unlockUser(final UUID userId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    accountLockoutManager.reset(user.getEmail());
    log.info("User unlocked: userId={}", userId);
    try {
      userEventPublisher.publishUserUnlocked(user);
    } catch (final Exception e) {
      log.warn("Failed to publish USER_UNLOCKED audit event for userId={}", userId, e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public UserDtos.TenantUserStatsResponse getTenantUserStats(final String tenantKey,
                                                             final UserDtos.TenantUserStatsQuery query) {
    // ── Resolve & validate the date range ────────────────────────────────────
    final java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
    final java.time.LocalDate from = parseDateOrDefault(query.from(), today.minusDays(29));
    final java.time.LocalDate to   = parseDateOrDefault(query.to(),   today);

    // Guard: from must not be after to, and the window is capped at 366 days to
    // prevent accidentally huge time-series result sets.
    final java.time.LocalDate safeFrom;
    final java.time.LocalDate safeTo;
    if (from.isAfter(to)) {
      safeFrom = to.minusDays(29);
      safeTo   = to;
    } else if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 366) {
      safeFrom = to.minusDays(365);
      safeTo   = to;
    } else {
      safeFrom = from;
      safeTo   = to;
    }

    // Granularity: only "month" or "day" (default)
    final String granularity = "month".equalsIgnoreCase(query.granularity()) ? "month" : "day";

    // ── Aggregate counts ──────────────────────────────────────────────────────
    final long totalMembers     = userMapper.countMembersByTenantKey(tenantKey, null, null);
    final long activeMembers    = userMapper.countMembersByTenantKeyAndStatus(tenantKey, AccountStatus.ACTIVE.name());
    final long lockedMembers    = userMapper.countMembersByTenantKeyAndStatus(tenantKey, AccountStatus.LOCKED.name());
    final long suspendedMembers = userMapper.countMembersByTenantKeyAndStatus(tenantKey, AccountStatus.SUSPENDED.name());
    final long emailVerified    = userMapper.countEmailVerifiedMembersByTenantKey(tenantKey);

    // ── Build signup series with zero-gap fill ────────────────────────────────
    final java.util.List<UserDtos.UserSignupSeriesPoint> rawSeries =
        userMapper.countMemberSignupsByTenantKeyBetween(tenantKey, safeFrom, safeTo, granularity);

    final java.util.Map<String, Long> seriesMap = new java.util.LinkedHashMap<>();
    rawSeries.forEach(p -> seriesMap.put(p.period(), p.signups()));

    // Generate every expected bucket label and fill zeros for missing ones
    final java.util.List<UserDtos.UserSignupSeriesPoint> filledSeries = new java.util.ArrayList<>();
    if ("month".equals(granularity)) {
      java.time.YearMonth cursor = java.time.YearMonth.from(safeFrom);
      final java.time.YearMonth end = java.time.YearMonth.from(safeTo);
      while (!cursor.isAfter(end)) {
        final String label = cursor.toString(); // "YYYY-MM"
        filledSeries.add(new UserDtos.UserSignupSeriesPoint(label, seriesMap.getOrDefault(label, 0L)));
        cursor = cursor.plusMonths(1);
      }
    } else {
      java.time.LocalDate cursor = safeFrom;
      while (!cursor.isAfter(safeTo)) {
        final String label = cursor.toString(); // "YYYY-MM-DD"
        filledSeries.add(new UserDtos.UserSignupSeriesPoint(label, seriesMap.getOrDefault(label, 0L)));
        cursor = cursor.plusDays(1);
      }
    }

    return new UserDtos.TenantUserStatsResponse(
        tenantKey,
        totalMembers,
        activeMembers,
        lockedMembers,
        suspendedMembers,
        emailVerified,
        filledSeries,
        safeFrom.toString(),
        safeTo.toString(),
        granularity);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static java.time.LocalDate parseDateOrDefault(final String raw,
                                                         final java.time.LocalDate fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      return java.time.LocalDate.parse(raw.strip());
    } catch (final java.time.format.DateTimeParseException e) {
      return fallback;
    }
  }
}
