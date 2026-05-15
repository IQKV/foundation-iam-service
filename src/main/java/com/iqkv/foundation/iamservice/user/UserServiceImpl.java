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

import com.iqkv.foundation.iamservice.email.EmailVerificationToken;
import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.infrastructure.messaging.UserEventPublisher;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.shared.exception.InvalidAccountStatusException;
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

  private final UserMapper userMapper;
  private final TenantMembershipMapper membershipMapper;
  private final EmailVerificationTokenMapper emailVerificationTokenMapper;
  private final MessagingService messagingService;
  private final UserEventPublisher userEventPublisher;
  private final NotificationConfigurationProperties notificationProps;
  private final SignupStrategy signupStrategy;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(final UserMapper userMapper,
                         final TenantMembershipMapper membershipMapper,
                         final EmailVerificationTokenMapper emailVerificationTokenMapper,
                         final MessagingService messagingService,
                         final UserEventPublisher userEventPublisher,
                         final NotificationConfigurationProperties notificationProps,
                         final SignupStrategy signupStrategy,
                         final PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.membershipMapper = membershipMapper;
    this.emailVerificationTokenMapper = emailVerificationTokenMapper;
    this.messagingService = messagingService;
    this.userEventPublisher = userEventPublisher;
    this.notificationProps = notificationProps;
    this.signupStrategy = signupStrategy;
    this.passwordEncoder = passwordEncoder;
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
    final var notificationEvent = new NotificationEvent(
        canonicalUser.getEmail(),
        notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en",
        NotificationEventType.VERIFY_EMAIL,
        Map.of(
            "verificationUrl", verificationUrl,
            "firstName", canonicalUser.getFirstName(),
            "expiresInHours", 24),
        Instant.now());
    messagingService.publishNotification(notificationEvent);

    log.info("User registered: userId={}, tenantKey={}", canonicalUser.getId(), result.tenant().getTenantKey());
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
    user.setStatus(AccountStatus.ACTIVE);
    user.setEmailVerified(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setCreatedBy("system");
    user.setUpdatedBy("system");
    userMapper.upsertByEmail(user);
    return UserDtoMapper.toResponse(userMapper.findByEmail(request.email())
        .orElseThrow(() -> new UserNotFoundException("User not found after insert: " + request.email())));
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
    return UserDtoMapper.toResponse(user);
  }

  @Override
  public void deleteUserById(final UUID userId) {
    userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    userMapper.deleteById(userId);
    log.info("User deleted: userId={}", userId);
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
                                          final String lastName, final String updatedBy) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setUpdatedAt(LocalDateTime.now());
    user.setUpdatedBy(updatedBy);
    userMapper.update(user);
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
}
