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

package dev.iqkv.iamservice.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.iqkv.iamservice.shared.exception.MembershipNotFoundException;

@Service
@Transactional(readOnly = true)
public class MembershipServiceImpl implements MembershipService {

  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;

  public MembershipServiceImpl(final TenantMembershipMapper membershipMapper,
                                final TenantMemberAuthorityMapper authorityMapper) {
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
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
}
