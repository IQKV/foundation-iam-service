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

import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthority;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.util.UserServiceConstants;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
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
 * <p>Creates a user and adds them as a MEMBER to the platform tenant.
 * Organizations/tenants are created separately via dedicated API endpoint.
 */
@Service
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "MULTI_TENANT")
public class MultiTenantSignupStrategy implements SignupStrategy {

  private static final Logger log = LoggerFactory.getLogger(MultiTenantSignupStrategy.class);
  private static final String PLATFORM_TENANT_KEY = "platform";

  private final UserMapper userMapper;
  private final TenantMapper tenantMapper;
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final PasswordEncoder passwordEncoder;

  public MultiTenantSignupStrategy(final UserMapper userMapper,
                                   final TenantMapper tenantMapper,
                                   final TenantMembershipMapper membershipMapper,
                                   final TenantMemberAuthorityMapper authorityMapper,
                                   final PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public SignupResult execute(final UserDtos.RegisterUserRequest request) {
    // Step 1: Upsert user (atomic, eliminates TOCTOU)
    final var user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setStatus(AccountStatus.ACTIVE);
    user.setEmailVerified(false);
    user.setProfileCompleted(true); // name provided at signup — profile is immediately complete
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setCreatedBy("system");
    user.setUpdatedBy("system");

    userMapper.upsertByEmail(user);
    final User canonicalUser = userMapper.findByEmail(request.email())
        .orElseThrow(() -> new UserNotFoundException("User not found after upsert: " + request.email()));

    // Step 2: Add user to platform tenant with MEMBER authority if not already a member
    ensurePlatformMembership(canonicalUser);

    // Step 3: Get platform tenant for SignupResult
    final Tenant platformTenant = tenantMapper.findByTenantKey(PLATFORM_TENANT_KEY)
        .orElseThrow(() -> new IllegalStateException("Platform tenant not found"));

    final TenantMembership platformMembership = membershipMapper
        .findByUserIdAndTenantKey(canonicalUser.getId(), PLATFORM_TENANT_KEY)
        .orElseThrow(() -> new IllegalStateException("Platform membership not found after creation"));

    log.info("Multi-tenant signup: userId={}", canonicalUser.getId());

    return new SignupResult(
        canonicalUser,
        platformTenant,
        platformMembership,
        List.of(UserServiceConstants.AUTHORITY_MEMBER)
    );
  }

  /**
   * Ensures the user has an active membership in the platform tenant with MEMBER authority.
   * Idempotent - skips creation if membership already exists.
   */
  private void ensurePlatformMembership(final User user) {
    if (!membershipMapper.existsByUserIdAndTenantKey(user.getId(), PLATFORM_TENANT_KEY)) {
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
}
