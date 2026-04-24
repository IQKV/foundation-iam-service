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

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iqkv.foundation.iamservice.authentication.JwtTokenGenerator;
import com.iqkv.foundation.iamservice.infrastructure.config.InvitationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.lockout.AccountLockoutManager;
import com.iqkv.foundation.iamservice.membership.MembershipService;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthority;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.shared.exception.InvitationAlreadyPendingException;
import com.iqkv.foundation.iamservice.shared.exception.InvitationNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TenantMembershipAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotAvailableException;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantMapper;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;

@Service
@Transactional
public class InvitationServiceImpl implements InvitationService {

  private static final Logger log = LoggerFactory.getLogger(InvitationServiceImpl.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final InvitationMapper invitationMapper;
  private final TenantMapper tenantMapper;
  private final UserMapper userMapper;
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final MembershipService membershipService;
  private final AccountLockoutManager accountLockoutManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenGenerator jwtTokenGenerator;
  private final MessagingService messagingService;
  private final InvitationConfigurationProperties invitationProps;
  private final NotificationConfigurationProperties notificationProps;

  public InvitationServiceImpl(
      final InvitationMapper invitationMapper,
      final TenantMapper tenantMapper,
      final UserMapper userMapper,
      final TenantMembershipMapper membershipMapper,
      final TenantMemberAuthorityMapper authorityMapper,
      final MembershipService membershipService,
      final AccountLockoutManager accountLockoutManager,
      final PasswordEncoder passwordEncoder,
      final JwtTokenGenerator jwtTokenGenerator,
      final MessagingService messagingService,
      final InvitationConfigurationProperties invitationProps,
      final NotificationConfigurationProperties notificationProps) {
    this.invitationMapper = invitationMapper;
    this.tenantMapper = tenantMapper;
    this.userMapper = userMapper;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.membershipService = membershipService;
    this.accountLockoutManager = accountLockoutManager;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenGenerator = jwtTokenGenerator;
    this.messagingService = messagingService;
    this.invitationProps = invitationProps;
    this.notificationProps = notificationProps;
  }

  // -------------------------------------------------------------------------
  // Send invitation
  // -------------------------------------------------------------------------

  @Override
  public InvitationDtos.InvitationResponse sendInvitation(
      final String tenantKey,
      final UUID inviterId,
      final InvitationDtos.SendInvitationRequest request) {

    final Tenant tenant = requireActiveTenant(tenantKey);

    // Verify inviter has sufficient authority (TENANT_OWNER or ADMIN)
    final var inviterMembership = membershipService.resolveMembership(inviterId, tenantKey);
    final List<String> inviterAuthorities = membershipService.getAuthorities(inviterMembership.getId());
    if (!inviterAuthorities.contains("TENANT_OWNER") && !inviterAuthorities.contains("ADMIN")) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Only TENANT_OWNER or ADMIN can send invitations");
    }

    // ADMIN cannot grant a role higher than their own
    if ("ADMIN".equals(request.role()) && !inviterAuthorities.contains("TENANT_OWNER")
        && !inviterAuthorities.contains("ADMIN")) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Insufficient authority to grant role " + request.role());
    }

    final String normalizedEmail = request.email().toLowerCase();

    // Default authority to MEMBER when not specified
    final String authority = (request.authority() != null && !request.authority().isBlank())
        ? request.authority()
        : "MEMBER";

    // ADMIN cannot grant an authority higher than their own
    if ("ADMIN".equals(authority) && !inviterAuthorities.contains("TENANT_OWNER")
        && !inviterAuthorities.contains("ADMIN")) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Insufficient authority to grant " + authority);
    }
    // Guard: no duplicate PENDING invite
    if (invitationMapper.existsPendingForTenantAndEmail(tenantKey, normalizedEmail)) {
      throw new InvitationAlreadyPendingException(normalizedEmail);
    }

    // Guard: invitee is not already an ACTIVE member
    userMapper.findByEmail(normalizedEmail).ifPresent(existingUser -> {
      if (membershipMapper.existsByUserIdAndTenantKey(existingUser.getId(), tenantKey)) {
        throw new TenantMembershipAlreadyExistsException();
      }
    });

    // Generate token
    final byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    final String tokenValue = HexFormat.of().formatHex(bytes);

    final var invitation = new TenantInvitation();
    invitation.setId(UUID.randomUUID());
    invitation.setTenantKey(tenantKey);
    invitation.setInvitedEmail(normalizedEmail);
    invitation.setInvitedBy(inviterId);
    invitation.setAuthority(authority);
    invitation.setToken(tokenValue);
    invitation.setStatus(InvitationStatus.PENDING);
    invitation.setExpiresAt(Instant.now().plus(invitationProps.tokenTtl()));
    invitation.setCreatedAt(LocalDateTime.now());
    invitation.setUpdatedAt(LocalDateTime.now());
    invitation.setCreatedBy(inviterId.toString());
    invitation.setUpdatedBy(inviterId.toString());
    invitationMapper.insert(invitation);

    // Publish user.invited domain event
    messagingService.publishUserInvited(invitation, tenant.getName());

    // Send invitation email
    final var inviter = userMapper.findById(inviterId).orElse(null);
    final String inviterName = inviter != null
        ? inviter.getFirstName() + " " + inviter.getLastName()
        : tenant.getName();
    final String acceptUrl = notificationProps.baseUrl() + "/accept-invitation?token=" + tokenValue;

    final var event = new NotificationEvent(
        normalizedEmail,
        notificationProps.defaultLocale(),
        NotificationEventType.INVITATION,
        Map.of(
            "inviterName", inviterName,
            "tenantName", tenant.getName(),
            "authority", invitation.getAuthority(),
            "acceptUrl", acceptUrl,
            "expiresAt", invitation.getExpiresAt().toString()),
        Instant.now());
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish INVITATION notification for invitationId={}", invitation.getId(), e);
    }

    log.info("Invitation sent: invitationId={} tenantKey={} email={} role={}",
        invitation.getId(), tenantKey, normalizedEmail, request.role());

    return InvitationDtoMapper.toResponse(invitation);
  }

  // -------------------------------------------------------------------------
  // Preview (public — no auth)
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public InvitationDtos.InvitationPreviewResponse previewInvitation(final String token) {
    final TenantInvitation invitation = requirePendingInvitation(token);
    final Tenant tenant = tenantMapper.findByTenantKey(invitation.getTenantKey())
        .orElseThrow(InvitationNotFoundException::new);
    final boolean requiresSignup = !userMapper.existsByEmail(invitation.getInvitedEmail());
    return InvitationDtoMapper.toPreviewResponse(invitation, tenant.getName(), requiresSignup);
  }

  // -------------------------------------------------------------------------
  // Accept (public — no auth)
  // -------------------------------------------------------------------------

  @Override
  public InvitationDtos.AcceptInvitationResponse acceptInvitation(
      final String token,
      final InvitationDtos.AcceptInvitationRequest request) {

    final TenantInvitation invitation = requirePendingInvitation(token);
    final String tenantKey = invitation.getTenantKey();

    requireActiveTenant(tenantKey);

    final User user = resolveOrCreateUser(invitation, request);

    // Guard: membership must not already exist (race condition safety)
    if (membershipMapper.existsByUserIdAndTenantKey(user.getId(), tenantKey)) {
      throw new TenantMembershipAlreadyExistsException();
    }

    // Create membership
    final var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    membership.setUserId(user.getId());
    membership.setTenantKey(tenantKey);
    membership.setStatus(MembershipStatus.ACTIVE);
    membership.setCreatedAt(LocalDateTime.now());
    membership.setUpdatedAt(LocalDateTime.now());
    membership.setCreatedBy(user.getId().toString());
    membership.setUpdatedBy(user.getId().toString());
    membershipMapper.insert(membership);

    // Grant the invited authority
    final var authority = new TenantMemberAuthority();
    authority.setId(UUID.randomUUID());
    authority.setMembershipId(membership.getId());
    authority.setAuthority(invitation.getAuthority());
    authorityMapper.insert(authority);

    // Consume the invitation
    invitationMapper.markAccepted(invitation.getId(), Instant.now(), LocalDateTime.now());

    // Issue token pair scoped to the invited tenant
    final List<String> authorities = membershipService.getAuthorities(membership.getId());
    final String accessToken = jwtTokenGenerator.generateAccessToken(user, tenantKey, authorities);
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, tenantKey);

    log.info("Invitation accepted: invitationId={} userId={} tenantKey={}",
        invitation.getId(), user.getId(), tenantKey);

    return new InvitationDtos.AcceptInvitationResponse(accessToken, refreshToken, tenantKey);
  }

  // -------------------------------------------------------------------------
  // Revoke
  // -------------------------------------------------------------------------

  @Override
  public void revokeInvitation(
      final String tenantKey,
      final UUID invitationId,
      final UUID requesterId) {

    final TenantInvitation invitation = invitationMapper.findById(invitationId)
        .orElseThrow(InvitationNotFoundException::new);

    if (!invitation.getTenantKey().equals(tenantKey)) {
      throw new InvitationNotFoundException();
    }

    if (invitation.getStatus() != InvitationStatus.PENDING) {
      throw new InvitationNotFoundException("Invitation is no longer pending");
    }

    // Requester must be the inviter, or have TENANT_OWNER / ADMIN authority
    final boolean isInviter = invitation.getInvitedBy().equals(requesterId);
    if (!isInviter) {
      final var requesterMembership = membershipService.resolveMembership(requesterId, tenantKey);
      final List<String> requesterAuthorities = membershipService.getAuthorities(requesterMembership.getId());
      if (!requesterAuthorities.contains("TENANT_OWNER") && !requesterAuthorities.contains("ADMIN")) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Insufficient authority to revoke this invitation");
      }
    }

    invitationMapper.updateStatus(invitationId, InvitationStatus.REVOKED.name(), LocalDateTime.now());
    log.info("Invitation revoked: invitationId={} tenantKey={} by={}",
        invitationId, tenantKey, requesterId);
  }

  // -------------------------------------------------------------------------
  // List
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<InvitationDtos.InvitationResponse> listInvitations(final String tenantKey) {
    return invitationMapper.findPendingByTenantKey(tenantKey).stream()
        .map(InvitationDtoMapper::toResponse)
        .toList();
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Resolves an existing user (verifying password) or creates a new one.
   * For new users the email is considered verified — they clicked the invite link.
   */
  private User resolveOrCreateUser(
      final TenantInvitation invitation,
      final InvitationDtos.AcceptInvitationRequest request) {

    final var existingOpt = userMapper.findByEmail(invitation.getInvitedEmail());

    if (existingOpt.isPresent()) {
      // Existing user path — verify password (brute-force protection applies)
      final User existing = existingOpt.get();
      if (accountLockoutManager.isLocked(existing.getEmail())) {
        throw new com.iqkv.foundation.iamservice.shared.exception.AccountLockedException();
      }
      if (!passwordEncoder.matches(request.password(), existing.getPasswordHash())) {
        accountLockoutManager.recordFailedAttempt(existing.getEmail());
        throw new BadCredentialsException("Invalid credentials");
      }
      accountLockoutManager.reset(existing.getEmail());
      return existing;
    }

    // New user path — firstName and lastName are required
    if (request.firstName() == null || request.firstName().isBlank()
        || request.lastName() == null || request.lastName().isBlank()) {
      throw new com.iqkv.foundation.iamservice.shared.exception.UserRegistrationException(
          "firstName and lastName are required for new users");
    }

    final var newUser = new User();
    newUser.setId(UUID.randomUUID());
    newUser.setEmail(invitation.getInvitedEmail());
    newUser.setPasswordHash(passwordEncoder.encode(request.password()));
    newUser.setFirstName(request.firstName().trim());
    newUser.setLastName(request.lastName().trim());
    newUser.setStatus(AccountStatus.ACTIVE);
    // Email is implicitly verified — the user clicked the invite link sent to that address
    newUser.setEmailVerified(true);
    newUser.setCreatedAt(LocalDateTime.now());
    newUser.setUpdatedAt(LocalDateTime.now());
    newUser.setCreatedBy("invitation:" + invitation.getId());
    newUser.setUpdatedBy("invitation:" + invitation.getId());
    userMapper.upsertByEmail(newUser);

    return userMapper.findByEmail(invitation.getInvitedEmail())
        .orElseThrow(() -> new com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException(
            "User not found after insert: " + invitation.getInvitedEmail()));
  }

  private Tenant requireActiveTenant(final String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotAvailableException("Tenant not found: " + tenantKey));
    if (tenant.getStatus() != TenantStatus.ACTIVE) {
      throw new TenantNotAvailableException("Tenant is not active: " + tenantKey);
    }
    return tenant;
  }

  private TenantInvitation requirePendingInvitation(final String token) {
    final TenantInvitation invitation = invitationMapper.findByToken(token)
        .orElseThrow(InvitationNotFoundException::new);
    if (!invitation.isPending()) {
      // Do not distinguish between expired/revoked/accepted — prevents enumeration
      throw new InvitationNotFoundException();
    }
    return invitation;
  }
}
