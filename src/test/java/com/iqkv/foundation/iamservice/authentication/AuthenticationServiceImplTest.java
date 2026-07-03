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

package com.iqkv.foundation.iamservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.denylist.TokenDenylistService;
import com.iqkv.foundation.iamservice.email.EmailVerificationToken;
import com.iqkv.foundation.iamservice.email.EmailVerificationTokenMapper;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.shared.exception.AccountNotActiveException;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import com.iqkv.foundation.tenancy.TenantContext;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceImplTest {

  @Mock
  private UserMapper userMapper;
  @Mock
  private TenantMapper tenantMapper;
  @Mock
  private TenantMembershipMapper membershipMapper;
  @Mock
  private MembershipService membershipService;
  @Mock
  private AccountLockoutManager accountLockoutManager;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtTokenGenerator jwtTokenGenerator;
  @Mock
  private JwtDecoder jwtDecoder;
  @Mock
  private TokenDenylistService tokenDenylistService;
  @Mock
  private EmailVerificationTokenMapper emailVerificationTokenMapper;
  @Mock
  private MessagingService messagingService;
  @Mock
  private NotificationConfigurationProperties notificationProps;
  @Mock
  private com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper platformAuthorityMapper;
  @Mock
  private com.iqkv.foundation.iamservice.tenant.TenantService tenantService;
  @Mock
  private com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics metrics;
  @Mock
  private com.iqkv.foundation.iamservice.ban.BanService banService;
  @Mock
  private com.iqkv.foundation.iamservice.tenant.TenantListingService tenantListingService;

  private AuthenticationServiceImpl authenticationService;

  private User testUser;
  private Tenant testTenant;
  private TenantMembership testMembership;

  @BeforeEach
  void setUp() {
    authenticationService = new AuthenticationServiceImpl(
        userMapper,
        tenantMapper,
        membershipMapper,
        membershipService,
        accountLockoutManager,
        passwordEncoder,
        jwtTokenGenerator,
        jwtDecoder,
        tokenDenylistService,
        emailVerificationTokenMapper,
        messagingService,
        notificationProps,
        platformAuthorityMapper,
        tenantService,
        metrics,
        banService,
        tenantListingService
    );

    lenient().when(metrics.authDurationTimer(any(), any()))
        .thenReturn(Timer.builder("test").register(new SimpleMeterRegistry()));

    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("user@example.com");
    testUser.setPasswordHash("$2a$10$hashedPassword");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setStatus(AccountStatus.ACTIVE);
    testUser.setEmailVerified(true);
    testUser.setCreatedAt(LocalDateTime.now());

    testTenant = new Tenant();
    testTenant.setTenantKey("test-tenant");
    testTenant.setName("Test Tenant");
    testTenant.setStatus(TenantStatus.ACTIVE);

    testMembership = new TenantMembership();
    testMembership.setId(UUID.randomUUID());
    testMembership.setUserId(testUser.getId());
    testMembership.setTenantKey("test-tenant");
    testMembership.setStatus(MembershipStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should sign in user successfully with valid credentials")
  void shouldSignInSuccessfully() {
    // Arrange
    var request = new AuthenticationDtos.SignInRequest("user@example.com", "password123");
    var authorities = List.of("MEMBER");

    // Set tenant context
    TenantContext.setCurrentTenant("test-tenant");

    when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
    when(accountLockoutManager.isLocked("user@example.com")).thenReturn(false);
    when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
    when(membershipService.resolveMembership(testUser.getId(), "test-tenant")).thenReturn(testMembership);
    when(membershipService.getAuthorities(testMembership.getId())).thenReturn(authorities);
    when(jwtTokenGenerator.generateAccessToken(testUser, "test-tenant", authorities, null))
        .thenReturn("access-token-123");
    when(jwtTokenGenerator.generateRefreshToken(testUser, "test-tenant"))
        .thenReturn("refresh-token-456");

    // Act
    var result = authenticationService.signIn(request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("access-token-123");
    assertThat(result.refreshToken()).isEqualTo("refresh-token-456");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");
    verify(accountLockoutManager).reset("user@example.com");

    // Cleanup
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should refresh token successfully with valid refresh token")
  void shouldRefreshTokenSuccessfully() {
    // Arrange
    var request = new AuthenticationDtos.RefreshTokenRequest("valid-refresh-token");
    var authorities = List.of("MEMBER");
    var jwt = createMockJwt("user@example.com", "test-tenant", JwtClaimNames.TYPE_REFRESH);

    // Set tenant context
    TenantContext.setCurrentTenant("test-tenant");

    when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
    when(membershipService.resolveMembership(testUser.getId(), "test-tenant")).thenReturn(testMembership);
    when(membershipService.getAuthorities(testMembership.getId())).thenReturn(authorities);
    when(jwtTokenGenerator.generateAccessToken(testUser, "test-tenant", authorities, null))
        .thenReturn("new-access-token");
    when(jwtTokenGenerator.generateRefreshToken(testUser, "test-tenant"))
        .thenReturn("new-refresh-token");

    // Act
    var result = authenticationService.refresh(request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("new-access-token");
    assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");

    // Cleanup
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should sign out user successfully")
  void shouldSignOutSuccessfully() {
    // Arrange
    var jti = "token-id-123";
    var userId = UUID.randomUUID().toString();
    var expiresAt = Instant.now().plusSeconds(3600).toString();

    // Act
    authenticationService.signOut(jti, userId, expiresAt);

    // Assert
    verify(tokenDenylistService).denyToken(eq(jti), eq(UUID.fromString(userId)), any(Instant.class));
  }

  @Test
  @DisplayName("Should sign out all sessions successfully")
  void shouldSignOutAllSuccessfully() {
    // Arrange
    var userId = UUID.randomUUID().toString();
    var jti = "token-id-123";
    var expiresAt = Instant.now().plusSeconds(3600).toString();

    // Act
    authenticationService.signOutAll(userId, jti, expiresAt);

    // Assert
    verify(userMapper).updateLastGlobalSignoutAt(eq(UUID.fromString(userId)), any(Instant.class));
    verify(tokenDenylistService).denyToken(eq(jti), eq(UUID.fromString(userId)), any(Instant.class));
  }

  @Test
  @DisplayName("Should validate token successfully")
  void shouldValidateTokenSuccessfully() {
    // Arrange
    var token = "valid-token";
    var userId = UUID.randomUUID();
    var jwt = createMockJwtWithClaims(
        "jti-123",
        userId.toString(),
        "user@example.com",
        "test-tenant",
        List.of("MEMBER", "ADMIN")
    );

    when(jwtDecoder.decode(token)).thenReturn(jwt);
    when(tokenDenylistService.isRevoked("jti-123")).thenReturn(false);

    // Act
    var result = authenticationService.validateToken(token);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.tenantId()).isEqualTo("test-tenant");
    assertThat(result.authorities()).containsExactly("MEMBER", "ADMIN");
  }

  @Test
  @DisplayName("Should list user tenants successfully")
  void shouldListUserTenantsSuccessfully() {
    // Arrange
    var email = "user@example.com";
    var password = "password123";
    var authorities = List.of("MEMBER");
    var expectedTenantSummary = new AuthenticationDtos.TenantMembershipSummary(
        "test-tenant", "Test Tenant", "ACTIVE", authorities, false
    );

    when(userMapper.findByEmail(email)).thenReturn(Optional.of(testUser));
    when(accountLockoutManager.isLocked(email)).thenReturn(false);
    when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
    when(tenantListingService.prepareTenantList(testUser.getId())).thenReturn(List.of(expectedTenantSummary));

    // Act
    var result = authenticationService.listUserTenants(email, password);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).tenantKey()).isEqualTo("test-tenant");
    assertThat(result.get(0).tenantName()).isEqualTo("Test Tenant");
    assertThat(result.get(0).authorities()).containsExactly("MEMBER");
    verify(accountLockoutManager).reset(email);
  }

  @Test
  @DisplayName("Should exchange tenant tokens successfully")
  void shouldExchangeTenantTokensSuccessfully() {
    // Arrange
    var authorities = List.of("MEMBER");

    when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
    when(userMapper.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(membershipService.resolveMembership(testUser.getId(), "test-tenant")).thenReturn(testMembership);
    when(membershipService.getAuthorities(testMembership.getId())).thenReturn(authorities);
    when(jwtTokenGenerator.generateAccessToken(testUser, "test-tenant", authorities, null))
        .thenReturn("exchanged-access-token");
    when(jwtTokenGenerator.generateRefreshToken(testUser, "test-tenant"))
        .thenReturn("exchanged-refresh-token");

    // Act
    var result = authenticationService.exchangeTenant(testUser.getId(), "test-tenant");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("exchanged-access-token");
    assertThat(result.refreshToken()).isEqualTo("exchanged-refresh-token");
    assertThat(result.tenantKey()).isEqualTo("test-tenant");
  }

  @Test
  @DisplayName("Should verify email successfully")
  void shouldVerifyEmailSuccessfully() {
    // Arrange
    var token = "verification-token-123";
    var verificationToken = new EmailVerificationToken();
    verificationToken.setUserId(testUser.getId());
    verificationToken.setToken(token);
    verificationToken.setExpiresAt(Instant.now().plusSeconds(3600));

    when(emailVerificationTokenMapper.findByToken(token)).thenReturn(Optional.of(verificationToken));
    when(userMapper.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(notificationProps.baseUrl()).thenReturn("https://example.com");
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    authenticationService.verifyEmail(token);

    // Assert
    verify(userMapper).setEmailVerified(testUser.getId());
    verify(emailVerificationTokenMapper).deleteByUserId(testUser.getId());
    verify(messagingService).publishNotification(any());
  }

  @Test
  @DisplayName("Should resend verification email successfully")
  void shouldResendVerificationSuccessfully() {
    // Arrange
    var email = "user@example.com";
    testUser.setEmailVerified(false);

    when(userMapper.findByEmail(email)).thenReturn(Optional.of(testUser));
    when(emailVerificationTokenMapper.countResendsWithinWindow(eq(testUser.getId()), any(Instant.class)))
        .thenReturn(0);
    when(notificationProps.baseUrl()).thenReturn("https://example.com");
    when(notificationProps.defaultLocale()).thenReturn("en");

    // Act
    authenticationService.resendVerification(email);

    // Assert
    verify(emailVerificationTokenMapper).deleteByUserId(testUser.getId());
    verify(emailVerificationTokenMapper).insert(any(EmailVerificationToken.class));
    verify(messagingService).publishNotification(any());
  }

  // -------------------------------------------------------------------------
  // AccountStatus enforcement tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("signIn — suspended user should be rejected with AccountNotActiveException")
  void signIn_suspendedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.SUSPENDED);
    TenantContext.setCurrentTenant("test-tenant");
    try {
      when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
      when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> authenticationService.signIn(
          new AuthenticationDtos.SignInRequest("user@example.com", "password123")))
          .isInstanceOf(AccountNotActiveException.class);

      verifyNoInteractions(accountLockoutManager, passwordEncoder, membershipService, jwtTokenGenerator);
    } finally {
      TenantContext.clear();
    }
  }

  @Test
  @DisplayName("signIn — deleted user should be rejected with AccountNotActiveException")
  void signIn_deletedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.DELETED);
    TenantContext.setCurrentTenant("test-tenant");
    try {
      when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
      when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> authenticationService.signIn(
          new AuthenticationDtos.SignInRequest("user@example.com", "password123")))
          .isInstanceOf(AccountNotActiveException.class);

      verifyNoInteractions(accountLockoutManager, passwordEncoder, membershipService, jwtTokenGenerator);
    } finally {
      TenantContext.clear();
    }
  }

  @Test
  @DisplayName("signIn — locked (status) user should be rejected with AccountNotActiveException")
  void signIn_lockedStatusUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.LOCKED);
    TenantContext.setCurrentTenant("test-tenant");
    try {
      when(tenantMapper.findByTenantKey("test-tenant")).thenReturn(Optional.of(testTenant));
      when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> authenticationService.signIn(
          new AuthenticationDtos.SignInRequest("user@example.com", "password123")))
          .isInstanceOf(AccountNotActiveException.class);

      verifyNoInteractions(accountLockoutManager, passwordEncoder, membershipService, jwtTokenGenerator);
    } finally {
      TenantContext.clear();
    }
  }

  @Test
  @DisplayName("adminSignIn — suspended user should be rejected with AccountNotActiveException")
  void adminSignIn_suspendedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.SUSPENDED);
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> authenticationService.adminSignIn(
        new AuthenticationDtos.SignInRequest("user@example.com", "password123")))
        .isInstanceOf(AccountNotActiveException.class);

    verifyNoInteractions(accountLockoutManager, passwordEncoder, platformAuthorityMapper, jwtTokenGenerator);
  }

  @Test
  @DisplayName("adminSignIn — deleted user should be rejected with AccountNotActiveException")
  void adminSignIn_deletedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.DELETED);
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> authenticationService.adminSignIn(
        new AuthenticationDtos.SignInRequest("user@example.com", "password123")))
        .isInstanceOf(AccountNotActiveException.class);

    verifyNoInteractions(accountLockoutManager, passwordEncoder, platformAuthorityMapper, jwtTokenGenerator);
  }

  @Test
  @DisplayName("refresh — suspended user should be rejected with AccountNotActiveException")
  void refresh_suspendedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.SUSPENDED);
    var jwt = createMockJwt("user@example.com", "test-tenant", JwtClaimNames.TYPE_REFRESH);
    TenantContext.setCurrentTenant("test-tenant");
    try {
      when(jwtDecoder.decode("refresh-token")).thenReturn(jwt);
      when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> authenticationService.refresh(
          new AuthenticationDtos.RefreshTokenRequest("refresh-token")))
          .isInstanceOf(AccountNotActiveException.class);

      verifyNoInteractions(membershipService, jwtTokenGenerator);
    } finally {
      TenantContext.clear();
    }
  }

  @Test
  @DisplayName("adminRefresh — suspended user should be rejected with AccountNotActiveException")
  void adminRefresh_suspendedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.SUSPENDED);
    var jwt = createMockJwt("user@example.com", null, JwtClaimNames.TYPE_REFRESH);
    when(jwtDecoder.decode("refresh-token")).thenReturn(jwt);
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> authenticationService.adminRefresh(
        new AuthenticationDtos.RefreshTokenRequest("refresh-token")))
        .isInstanceOf(AccountNotActiveException.class);

    verifyNoInteractions(platformAuthorityMapper, jwtTokenGenerator);
  }

  @Test
  @DisplayName("listUserTenants — suspended user should be rejected with AccountNotActiveException")
  void listUserTenants_suspendedUser_throwsAccountNotActiveException() {
    testUser.setStatus(AccountStatus.SUSPENDED);
    when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> authenticationService.listUserTenants("user@example.com", "password123"))
        .isInstanceOf(AccountNotActiveException.class);

    verifyNoInteractions(accountLockoutManager, passwordEncoder, membershipMapper, jwtTokenGenerator);
  }

  private Jwt createMockJwt(String email, String tenantId, String type) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim(JwtClaimNames.SUB, email)
        .claim(JwtClaimNames.TENANT_ID, tenantId)
        .claim(JwtClaimNames.TYPE, type)
        .build();
  }

  private Jwt createMockJwtWithClaims(String jti, String userId, String email,
                                      String tenantId, List<String> authorities) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim(JwtClaimNames.JTI, jti)
        .claim(JwtClaimNames.USER_ID, userId)
        .claim(JwtClaimNames.EMAIL, email)
        .claim(JwtClaimNames.TENANT_ID, tenantId)
        .claim(JwtClaimNames.AUTHORITIES, authorities)
        .build();
  }
}
