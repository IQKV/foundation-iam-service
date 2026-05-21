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

  @Override
  @Transactional
  public void updateMemberAuthorities(final UUID userId, final String tenantKey, final List<String> authorities) {
    final TenantMembership membership = resolveMembership(userId, tenantKey);
    authorityMapper.deleteByMembershipId(membership.getId());

    for (final String auth : authorities) {
      final TenantMemberAuthority newAuth = new TenantMemberAuthority();
      newAuth.setId(UUID.randomUUID());
      newAuth.setMembershipId(membership.getId());
      newAuth.setAuthority(auth);
      authorityMapper.insert(newAuth);
    }
  }

  @Override
  public List<UserDtos.UserMembershipResponse> getUserMemberships(final UUID userId) {
    final List<TenantMembership> memberships = membershipMapper.findByUserId(userId);
    return memberships.stream().map(membership -> {
      final String tenantName = tenantMapper.findByTenantKey(membership.getTenantKey())
          .map(t -> t.getName())
          .orElse(membership.getTenantKey());
      final List<String> authorities = getAuthorities(membership.getId());
      return new UserDtos.UserMembershipResponse(
          membership.getTenantKey(),
          tenantName,
          membership.getStatus().name(),
          authorities
      );
    }).collect(Collectors.toList());
  }
}
