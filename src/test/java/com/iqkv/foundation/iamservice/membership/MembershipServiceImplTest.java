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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipService Unit Tests")
class MembershipServiceImplTest {

  @Mock
  private TenantMembershipMapper membershipMapper;
  @Mock
  private TenantMemberAuthorityMapper authorityMapper;
  @Mock
  private TenantMapper tenantMapper;

  private MembershipServiceImpl membershipService;

  private TenantMembership testMembership;

  @BeforeEach
  void setUp() {
    membershipService = new MembershipServiceImpl(membershipMapper, authorityMapper, tenantMapper);

    testMembership = new TenantMembership();
    testMembership.setId(UUID.randomUUID());
    testMembership.setUserId(UUID.randomUUID());
    testMembership.setTenantKey("test-tenant");
    testMembership.setStatus(MembershipStatus.ACTIVE);
    testMembership.setCreatedAt(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should resolve membership successfully")
  void shouldResolveMembershipSuccessfully() {
    // Arrange
    var userId = testMembership.getUserId();
    var tenantKey = "test-tenant";

    when(membershipMapper.findByUserIdAndTenantKey(userId, tenantKey))
        .thenReturn(Optional.of(testMembership));

    // Act
    var result = membershipService.resolveMembership(userId, tenantKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testMembership.getId());
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getTenantKey()).isEqualTo(tenantKey);
  }

  @Test
  @DisplayName("Should get authorities successfully")
  void shouldGetAuthoritiesSuccessfully() {
    // Arrange
    var membershipId = testMembership.getId();

    when(authorityMapper.findAuthorityValuesByMembershipId(membershipId))
        .thenReturn(List.of("MEMBER", "ADMIN"));

    // Act
    var result = membershipService.getAuthorities(membershipId);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly("MEMBER", "ADMIN");
  }

  private TenantMemberAuthority createAuthority(UUID membershipId, String authority) {
    var auth = new TenantMemberAuthority();
    auth.setId(UUID.randomUUID());
    auth.setMembershipId(membershipId);
    auth.setAuthority(authority);
    return auth;
  }
}
