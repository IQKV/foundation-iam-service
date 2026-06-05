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

package com.iqkv.foundation.iamservice.membership;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.UserManagementException;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MembershipServiceImpl implements MembershipService {

  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final TenantMapper tenantMapper;

  public MembershipServiceImpl(final TenantMembershipMapper membershipMapper,
                               final TenantMemberAuthorityMapper authorityMapper,
                               final TenantMapper tenantMapper) {
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.tenantMapper = tenantMapper;
  }

  @Override
  public TenantMembership resolveMembership(final UUID userId, final String tenantKey) {
    final TenantMembership membership = membershipMapper.findByUserIdAndTenantKey(userId, tenantKey)
        .orElseThrow(() -> new MembershipNotFoundException(userId, tenantKey));

    if (membership.getStatus() == MembershipStatus.SUSPENDED
        || membership.getStatus() == MembershipStatus.REMOVED) {
      throw new MembershipNotFoundException(userId, tenantKey);
    }

    return membership;
  }

  @Override
  public List<String> getAuthorities(final UUID membershipId) {
    return authorityMapper.findAuthorityValuesByMembershipId(membershipId);
  }

  // Helper method to count active TENANT_OWNERs in a tenant
  private long countTenantOwners(final String tenantKey) {
    return membershipMapper.findByTenantKey(tenantKey).stream()
        .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
        .filter(membership -> getAuthorities(membership.getId()).contains("TENANT_OWNER"))
        .count();
  }

  @Override
  @Transactional
  public void updateMemberAuthorities(final UUID actingUserId, final String tenantKey, final UUID targetUserId, final List<String> authorities) {
    final TenantMembership targetMembership = resolveMembership(targetUserId, tenantKey);
    final List<String> currentTargetAuthorities = getAuthorities(targetMembership.getId());

    // Check 1: If acting user is target user, is a TENANT_OWNER, and trying to remove TENANT_OWNER
    if (actingUserId.equals(targetUserId) && currentTargetAuthorities.contains("TENANT_OWNER") && !authorities.contains("TENANT_OWNER")) {
      final long currentOwnerCount = countTenantOwners(tenantKey);
      if (currentOwnerCount <= 1) {
        throw new UserManagementException("Cannot remove your own TENANT_OWNER authority as you are the last owner");
      }
    }

    // Check 2: If removing TENANT_OWNER from target, ensure at least one owner remains
    if (currentTargetAuthorities.contains("TENANT_OWNER") && !authorities.contains("TENANT_OWNER")) {
      final long currentOwnerCount = countTenantOwners(tenantKey);
      if (currentOwnerCount <= 1) {
        throw new UserManagementException("Cannot remove the last TENANT_OWNER from the tenant");
      }
    }

    // Proceed with updating authorities
    authorityMapper.deleteByMembershipId(targetMembership.getId());

    for (final String auth : authorities) {
      final TenantMemberAuthority newAuth = new TenantMemberAuthority();
      newAuth.setId(UUID.randomUUID());
      newAuth.setMembershipId(targetMembership.getId());
      newAuth.setAuthority(auth);
      authorityMapper.insert(newAuth);
    }
  }

  @Override
  @Transactional
  public void transferOwnership(final UUID fromUserId, final UUID toUserId, final String tenantKey) {
    // Validate: can't transfer to self
    if (fromUserId.equals(toUserId)) {
      throw new UserManagementException("Cannot transfer ownership to yourself");
    }

    // Validate: resolve both memberships
    final TenantMembership fromMembership = resolveMembership(fromUserId, tenantKey);
    resolveMembership(toUserId, tenantKey);

    // Validate: from user must be TENANT_OWNER
    final List<String> fromAuthorities = getAuthorities(fromMembership.getId());
    if (!fromAuthorities.contains("TENANT_OWNER")) {
      throw new UserManagementException("Current user is not the tenant owner");
    }

    // Update from user's authorities: remove TENANT_OWNER, leave MEMBER only
    final List<String> newFromAuthorities = fromAuthorities.stream()
        .filter(auth -> !"TENANT_OWNER".equals(auth))
        .filter(auth -> "MEMBER".equals(auth))
        .collect(Collectors.toList());
    if (newFromAuthorities.isEmpty()) {
      newFromAuthorities.add("MEMBER");
    }
    updateMemberAuthorities(fromUserId, tenantKey, fromUserId, newFromAuthorities);

    // Update to user's authorities: set to TENANT_OWNER only
    final List<String> newToAuthorities = new java.util.ArrayList<>();
    newToAuthorities.add("TENANT_OWNER");
    updateMemberAuthorities(fromUserId, tenantKey, toUserId, newToAuthorities);
  }

  @Override
  public List<UserDtos.UserMembershipResponse> getUserMemberships(final UUID userId) {
    final List<TenantMembership> memberships = membershipMapper.findByUserId(userId);
    return memberships.stream().map(membership -> {
      final var tenant = tenantMapper.findByTenantKey(membership.getTenantKey()).orElse(null);
      final String tenantName = tenant != null ? tenant.getName() : membership.getTenantKey();
      final boolean isPersonal = tenant != null && Boolean.TRUE.equals(tenant.getIsInternal());
      final List<String> authorities = getAuthorities(membership.getId());
      return new UserDtos.UserMembershipResponse(
          membership.getTenantKey(),
          tenantName,
          membership.getStatus().name(),
          authorities,
          isPersonal
      );
    }).collect(Collectors.toList());
  }
}
