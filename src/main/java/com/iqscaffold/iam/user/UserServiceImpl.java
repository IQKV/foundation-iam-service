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

package com.iqscaffold.iam.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iqscaffold.iam.email.EmailVerificationToken;
import com.iqscaffold.iam.email.EmailVerificationTokenMapper;
import com.iqscaffold.iam.infrastructure.config.NotificationConfigurationProperties;
import com.iqscaffold.iam.infrastructure.messaging.MessagingService;
import com.iqscaffold.iam.infrastructure.messaging.NotificationEvent;
import com.iqscaffold.iam.infrastructure.messaging.NotificationEventType;
import com.iqscaffold.iam.infrastructure.messaging.UserEventPublisher;
import com.iqscaffold.iam.membership.MembershipStatus;
import com.iqscaffold.iam.membership.TenantMemberAuthority;
import com.iqscaffold.iam.membership.TenantMemberAuthorityMapper;
import com.iqscaffold.iam.membership.TenantMembership;
import com.iqscaffold.iam.membership.TenantMembershipMapper;
import com.iqscaffold.iam.shared.exception.MembershipNotFoundException;
import com.iqscaffold.iam.shared.exception.TenantAlreadyExistsException;
import com.iqscaffold.iam.shared.exception.UserNotFoundException;
import com.iqscaffold.iam.tenant.Tenant;
import com.iqscaffold.iam.tenant.TenantMapper;
import com.iqscaffold.iam.tenant.TenantStatus;
import com.iqscaffold.iam.user.dto.UserDtoMapper;
import com.iqscaffold.iam.user.dto.UserDtos;

@Service
@Transactional
public class UserServiceImpl implements UserService {

  private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

  private static final char[] NANOID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int NANOID_SIZE = 8;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserMapper userMapper;
  private final TenantMapper tenantMapper;
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final EmailVerificationTokenMapper emailVerificationTokenMapper;
  private final MessagingService messagingService;
  private final UserEventPublisher userEventPublisher;
  private final PasswordEncoder passwordEncoder;
  private final NotificationConfigurationProperties notificationProps;

  public UserServiceImpl(final UserMapper userMapper,
                         final TenantMapper tenantMapper,
                         final TenantMembershipMapper membershipMapper,
                         final TenantMemberAuthorityMapper authorityMapper,
                         final EmailVerificationTokenMapper emailVerificationTokenMapper,
                         final MessagingService messagingService,
                         final UserEventPublisher userEventPublisher,
                         final PasswordEncoder passwordEncoder,
                         final NotificationConfigurationProperties notificationProps) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.emailVerificationTokenMapper = emailVerificationTokenMapper;
    this.messagingService = messagingService;
    this.userEventPublisher = userEventPublisher;
    this.passwordEncoder = passwordEncoder;
    this.notificationProps = notificationProps;
  }

  @Override
  public UserDtos.SignupResponse registerUser(final UserDtos.RegisterUserRequest request) {
    // Step 1: Upsert user (atomic, eliminates TOCTOU)
    final var user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setStatus(AccountStatus.ACTIVE);
    user.setEmailVerified(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setCreatedBy("system");
    user.setUpdatedBy("system");

    userMapper.upsertByEmail(user);
    final User canonicalUser = userMapper.findByEmail(request.email())
        .orElseThrow(() -> new UserNotFoundException("User not found after upsert: " + request.email()));

    // Step 2: Create tenant atomically (INSERT ... ON CONFLICT DO NOTHING)
    final String tenantKey = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NANOID_ALPHABET, NANOID_SIZE);
    final var tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantKey(tenantKey);
    tenant.setName(request.tenantName());
    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setCreatedAt(LocalDateTime.now());
    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setCreatedBy(canonicalUser.getId().toString());
    tenant.setUpdatedBy(canonicalUser.getId().toString());

    tenantMapper.insertIfAbsent(tenant);
    final Tenant canonicalTenant = tenantMapper.findByTenantKey(tenantKey).orElse(null);
    if (canonicalTenant == null) {
      // insertIfAbsent did nothing — name was already taken
      throw new TenantAlreadyExistsException("Tenant name already taken");
    }

    // Step 3: Create membership
    final var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    membership.setUserId(canonicalUser.getId());
    membership.setTenantKey(tenantKey);
    membership.setStatus(MembershipStatus.ACTIVE);
    membership.setCreatedAt(LocalDateTime.now());
    membership.setUpdatedAt(LocalDateTime.now());
    membership.setCreatedBy(canonicalUser.getId().toString());
    membership.setUpdatedBy(canonicalUser.getId().toString());
    membershipMapper.insert(membership);

    // Step 4: Grant TENANT_OWNER authority
    final var authority = new TenantMemberAuthority();
    authority.setId(UUID.randomUUID());
    authority.setMembershipId(membership.getId());
    authority.setAuthority("TENANT_OWNER");
    authorityMapper.insert(authority);

    // Step 5: Publish tenant created event
    messagingService.publishTenantCreated(tenantKey, request.tenantName());

    // Step 6: Publish user created event
    userEventPublisher.publishUserCreated(canonicalUser);

    // Step 7: Generate email verification token
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

    // Step 8: Publish verification email notification
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

    log.info("User registered: userId={}, tenantKey={}", canonicalUser.getId(), tenantKey);
    return UserDtoMapper.toSignupResponse(canonicalUser, canonicalTenant);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDtos.UserResponse getUserById(final UUID userId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    return UserDtoMapper.toResponse(user);
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

    userMapper.findById(userId).ifPresent(userEventPublisher::publishUserDeleted);
    log.info("User membership removed: userId={}, tenantKey={}", userId, tenantKey);
  }
}
