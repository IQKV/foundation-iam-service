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

package com.iqkv.foundation.iamservice.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.JwtTokenGenerator;
import com.iqkv.foundation.iamservice.infrastructure.config.InvitationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.PlatformConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.RolloutMode;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService Unit Tests")
class InvitationServiceImplTest {

  @Mock
  private InvitationMapper invitationMapper;
  @Mock
  private TenantMapper tenantMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private TenantMembershipMapper membershipMapper;
  @Mock
  private TenantMemberAuthorityMapper authorityMapper;
  @Mock
  private MembershipService membershipService;
  @Mock
  private AccountLockoutManager accountLockoutManager;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtTokenGenerator jwtTokenGenerator;
  @Mock
  private MessagingService messagingService;
  @Mock
  private InvitationConfigurationProperties invitationProps;
  @Mock
  private NotificationConfigurationProperties notificationProps;
  @Mock
  private PlatformConfigurationProperties platformConfig;

  private InvitationServiceImpl invitationService;

  private User testUser;
  private Tenant testTenant;
  private TenantMembership testMembership;
  private TenantInvitation testInvitation;

  @BeforeEach
  void setUp() {
    invitationService = new InvitationServiceImpl(
        invitationMapper,
        tenantMapper,
        userMapper,
        membershipMapper,
        authorityMapper,
        membershipService,
        accountLockoutManager,
        passwordEncoder,
        jwtTokenGenerator,
        messagingService,
        invitationProps,
        notificationProps,
        platformConfig
    );

    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("inviter@example.com");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setStatus(AccountStatus.ACTIVE);

    testTenant = new Tenant();
    testTenant.setTenantKey("test-tenant");
    testTenant.setName("Test Tenant");
    testTenant.setStatus(TenantStatus.ACTIVE);

    testMembership = new TenantMembership();
    testMembership.setId(UUID.randomUUID());
    testMembership.setUserId(testUser.getId());
    testMembership.setTenantKey("test-tenant");
    testMembership.setStatus(MembershipStatus.ACTIVE);

    testInvitation = new TenantInvitation();
    testInvitation.setId(UUID.randomUUID());
    testInvitation.setTenantKey("test-tenant");
    testInvitation.setInvitedEmail("invitee@example.com");
    testInvitation.setInvitedBy(testUser.getId());
    testInvitation.setAuthority("MEMBER");
    testInvitation.setToken("invitation-token-123");
    testInvitation.setStatus(InvitationStatus.PENDING);
    testInvitation.setExpiresAt(Instant.now().plusSeconds(86400));
  }

  @Test
  @DisplayName("Should send invitation successfully")
  void shouldSendInvitationSuccessfully() {
    // Arrange
    var request = new InvitationDtos.SendInvitationRequest("invitee@example.com", "MEMBER");
    var inviterId = testUser.getId();
    var tenantKey = "test-tenant";

    when(platformConfig.rolloutMode()).thenReturn(RolloutMode.MULTI_TENANT);
    when(tenantMapper.findByTenantKey(tenantKey)).thenReturn(Optional.of(testTenant));
    when(membershipService.resolveMembership(inviterId, tenantKey)).thenReturn(testMembership);
    when(membershipService.getAuthorities(testMembership.getId())).thenReturn(List.of("TENANT_OWNER"));
    when(invitationMapper.existsPendingForTenantAndEmail(tenantKey, "invitee@example.com")).thenReturn(false);
    when(userMapper.findByEmail("invitee@example.com")).thenReturn(Optional.empty());
    when(invitationProps.tokenTtl()).thenReturn(Duration.ofHours(24));
    when(userMapper.findById(inviterId)).thenReturn(Optional.of(testUser));
    when(notificationProps.baseUrl()).thenReturn("https://example.com");
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    var result = invitationService.sendInvitation(tenantKey, inviterId, request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo("invitee@example.com");
    assertThat(result.authority()).isEqualTo("MEMBER");
    assertThat(result.status()).isEqualTo("PENDING");
    verify(invitationMapper).insert(any(TenantInvitation.class));
    verify(messagingService).publishUserInvited(any(), eq("Test Tenant"));
  }

  @Test
  @DisplayName("Should preview invitation successfully")
  void shouldPreviewInvitationSuccessfully() {
    // Arrange
    var token = "invitation-token-123";

    when(invitationMapper.findByToken(token)).thenReturn(Optional.of(testInvitation));
    when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
    when(userMapper.existsByEmail("invitee@example.com")).thenReturn(false);

    // Act
    var result = invitationService.previewInvitation(token);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.tenantName()).isEqualTo("Test Tenant");
    assertThat(result.email()).isEqualTo("invitee@example.com");
    assertThat(result.authority()).isEqualTo("MEMBER");
    assertThat(result.requiresSignup()).isTrue();
  }

  @Test
  @DisplayName("Should accept invitation for existing user successfully")
  void shouldAcceptInvitationForExistingUserSuccessfully() {
    // Arrange
    var token = "invitation-token-123";
    var request = new InvitationDtos.AcceptInvitationRequest(
        null,
        null,
        "password123"
    );
    var existingUser = new User();
    existingUser.setId(UUID.randomUUID());
    existingUser.setEmail("invitee@example.com");
    existingUser.setPasswordHash("$2a$10$hashedPassword");
    existingUser.setStatus(AccountStatus.ACTIVE);

    when(invitationMapper.findByToken(token)).thenReturn(Optional.of(testInvitation));
    when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
    when(userMapper.findByEmail("invitee@example.com")).thenReturn(Optional.of(existingUser));
    when(accountLockoutManager.isLocked("invitee@example.com")).thenReturn(false);
    when(passwordEncoder.matches("password123", existingUser.getPasswordHash())).thenReturn(true);
    when(membershipMapper.existsByUserIdAndTenantKey(existingUser.getId(), "test-tenant")).thenReturn(false);
    when(membershipService.getAuthorities(any())).thenReturn(List.of("MEMBER"));
    when(jwtTokenGenerator.generateAccessToken(any(), eq("test-tenant"), any())).thenReturn("access-token");
    when(jwtTokenGenerator.generateRefreshToken(any(), eq("test-tenant"))).thenReturn("refresh-token");

    // Act
    var result = invitationService.acceptInvitation(token, request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");
    verify(membershipMapper).insert(any(TenantMembership.class));
    verify(authorityMapper).insert(any());
    verify(invitationMapper).markAccepted(eq(testInvitation.getId()), any(Instant.class), any(LocalDateTime.class));
    verify(accountLockoutManager).reset("invitee@example.com");
  }

  @Test
  @DisplayName("Should accept invitation for new user successfully")
  void shouldAcceptInvitationForNewUserSuccessfully() {
    // Arrange
    var token = "invitation-token-123";
    var request = new InvitationDtos.AcceptInvitationRequest(
        "Jane",
        "Smith",
        "password123"
    );
    var newUser = new User();
    newUser.setId(UUID.randomUUID());
    newUser.setEmail("invitee@example.com");
    newUser.setFirstName("Jane");
    newUser.setLastName("Smith");
    newUser.setStatus(AccountStatus.ACTIVE);
    newUser.setEmailVerified(true);

    when(invitationMapper.findByToken(token)).thenReturn(Optional.of(testInvitation));
    when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
    when(userMapper.findByEmail("invitee@example.com"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(newUser));
    when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
    when(membershipMapper.existsByUserIdAndTenantKey(any(), eq("test-tenant"))).thenReturn(false);
    when(membershipService.getAuthorities(any())).thenReturn(List.of("MEMBER"));
    when(jwtTokenGenerator.generateAccessToken(any(), eq("test-tenant"), any())).thenReturn("access-token");
    when(jwtTokenGenerator.generateRefreshToken(any(), eq("test-tenant"))).thenReturn("refresh-token");

    // Act
    var result = invitationService.acceptInvitation(token, request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");
    verify(userMapper).upsertByEmail(any(User.class));
    verify(membershipMapper).insert(any(TenantMembership.class));
    verify(authorityMapper).insert(any());
  }

  @Test
  @DisplayName("Should revoke invitation successfully")
  void shouldRevokeInvitationSuccessfully() {
    // Arrange
    var invitationId = testInvitation.getId();
    var tenantKey = "test-tenant";
    var requesterId = testUser.getId();

    when(invitationMapper.findById(invitationId)).thenReturn(Optional.of(testInvitation));

    // Act
    invitationService.revokeInvitation(tenantKey, invitationId, requesterId);

    // Assert
    verify(invitationMapper).updateStatus(eq(invitationId), eq("REVOKED"), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should list pending invitations successfully")
  void shouldListPendingInvitationsSuccessfully() {
    // Arrange
    var tenantKey = "test-tenant";
    var invitation1 = createInvitation("user1@example.com", "MEMBER");
    var invitation2 = createInvitation("user2@example.com", "ADMIN");

    when(invitationMapper.findPendingByTenantKey(tenantKey))
        .thenReturn(List.of(invitation1, invitation2));

    // Act
    var result = invitationService.listInvitations(tenantKey);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).email()).isEqualTo("user1@example.com");
    assertThat(result.get(1).email()).isEqualTo("user2@example.com");
  }

  private TenantInvitation createInvitation(String email, String authority) {
    var invitation = new TenantInvitation();
    invitation.setId(UUID.randomUUID());
    invitation.setTenantKey("test-tenant");
    invitation.setInvitedEmail(email);
    invitation.setInvitedBy(testUser.getId());
    invitation.setAuthority(authority);
    invitation.setToken("token-" + UUID.randomUUID());
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setExpiresAt(Instant.now().plusSeconds(86400));
    invitation.setCreatedAt(LocalDateTime.now());
    return invitation;
  }
}
