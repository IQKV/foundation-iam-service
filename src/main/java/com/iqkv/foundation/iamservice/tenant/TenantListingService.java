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

package com.iqkv.foundation.iamservice.tenant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import org.springframework.stereotype.Service;

/**
 * Service to prepare tenant listings (including personal workspace) for users.
 */
@Service
public class TenantListingService {

  private static final String PERSONAL_WORKSPACE_NAME = "Personal Workspace";

  private final TenantMapper tenantMapper;
  private final TenantMembershipMapper membershipMapper;
  private final MembershipService membershipService;

  public TenantListingService(
      final TenantMapper tenantMapper,
      final TenantMembershipMapper membershipMapper,
      final MembershipService membershipService
  ) {
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.membershipService = membershipService;
  }

  /**
   * Prepares tenant list for a user:
   * - Includes platform tenant as Personal Workspace at the top of the list
   * - Excludes other internal tenants
   * - Only includes active memberships in active tenants
   *
   * @param userId User ID to fetch tenants for
   * @return Prepared list of tenants
   */
  public List<AuthenticationDtos.TenantMembershipSummary> prepareTenantList(final UUID userId) {
    final List<TenantMembership> memberships = membershipMapper.findByUserId(userId);
    final List<AuthenticationDtos.TenantMembershipSummary> result = new ArrayList<>();
    AuthenticationDtos.TenantMembershipSummary personalWorkspace = null;

    for (final TenantMembership membership : memberships) {
      if (membership.getStatus() != com.iqkv.foundation.iamservice.membership.MembershipStatus.ACTIVE) {
        continue;
      }
      final var tenant = tenantMapper.findByTenantKey(membership.getTenantKey()).orElse(null);
      if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
        continue;
      }
      final var authorities = membershipService.getAuthorities(membership.getId());
      final var isPersonal = Boolean.TRUE.equals(tenant.getIsInternal());

      if (isPersonal) {
        personalWorkspace = new AuthenticationDtos.TenantMembershipSummary(
            tenant.getTenantKey(),
            PERSONAL_WORKSPACE_NAME,
            membership.getStatus().name(),
            authorities,
            true
        );
      } else {
        result.add(new AuthenticationDtos.TenantMembershipSummary(
            tenant.getTenantKey(),
            tenant.getName(),
            membership.getStatus().name(),
            authorities,
            false
        ));
      }
    }

    if (personalWorkspace != null) {
      result.add(0, personalWorkspace);
    }

    return result;
  }
}
