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

package com.iqkv.foundation.iamservice.signup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthority;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.util.UserServiceConstants;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SignupStrategy} implementation for {@code MULTI_TENANT} mode.
 *
 * <p>Creates a new tenant per signup, grants {@code TENANT_OWNER} authority,
 * and publishes a {@code tenant.created} event with owner fields.
 */
@Service
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "MULTI_TENANT")
public class MultiTenantSignupStrategy implements SignupStrategy {

  private static final Logger log = LoggerFactory.getLogger(MultiTenantSignupStrategy.class);

  private static final char[] NANOID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int NANOID_SIZE = 8;
  private static final String TENANT_MODE_ORIGIN_MULTI_SIGNUP = "MULTI_SIGNUP";

  private final UserMapper userMapper;
  private final TenantMapper tenantMapper;
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final MessagingService messagingService;
  private final PasswordEncoder passwordEncoder;

  public MultiTenantSignupStrategy(final UserMapper userMapper,
                                   final TenantMapper tenantMapper,
                                   final TenantMembershipMapper membershipMapper,
                                   final TenantMemberAuthorityMapper authorityMapper,
                                   final MessagingService messagingService,
                                   final PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.messagingService = messagingService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public SignupResult execute(final UserDtos.RegisterUserRequest request) {
    // Step 1: Validate tenantName is present and not blank (required in MULTI_TENANT mode)
    if (request.tenantName() == null || request.tenantName().isBlank()) {
      throw new IllegalArgumentException("tenantName is required in MULTI_TENANT mode");
    }

    // Step 2: Upsert user (atomic, eliminates TOCTOU)
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

    // Step 3: Create tenant atomically (INSERT ... ON CONFLICT DO NOTHING)
    final String tenantKey = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NANOID_ALPHABET, NANOID_SIZE);
    final var tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantKey(tenantKey);
    tenant.setName(request.tenantName());
    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setIsDefault(false);
    tenant.setTenantModeOrigin(TENANT_MODE_ORIGIN_MULTI_SIGNUP);
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

    // Step 4: Create membership with status=ACTIVE
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

    // Step 5: Grant TENANT_OWNER authority
    final var authority = new TenantMemberAuthority();
    authority.setId(UUID.randomUUID());
    authority.setMembershipId(membership.getId());
    authority.setAuthority(UserServiceConstants.AUTHORITY_TENANT_OWNER);
    authorityMapper.insert(authority);

    // Step 6: Publish tenant.created event with owner fields
    messagingService.publishTenantCreated(
        tenantKey,
        request.tenantName(),
        request.email(),
        request.firstName());

    log.info("Multi-tenant signup: userId={}, tenantKey={}", canonicalUser.getId(), tenantKey);

    return new SignupResult(
        canonicalUser,
        canonicalTenant,
        membership,
        List.of(UserServiceConstants.AUTHORITY_TENANT_OWNER));
  }
}
