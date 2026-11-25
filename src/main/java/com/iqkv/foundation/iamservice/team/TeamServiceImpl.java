/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.iamservice.team;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iqkv.foundation.iamservice.infrastructure.config.InvitationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.shared.exception.InvitationAlreadyPendingException;
import com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TeamInvitationNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TeamNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TenantMembershipAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

  private static final Logger log = LoggerFactory.getLogger(TeamServiceImpl.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final TeamMapper teamMapper;
  private final TeamMemberMapper teamMemberMapper;
  private final TeamInvitationMapper teamInvitationMapper;
  private final UserMapper userMapper;
  private final MessagingService messagingService;
  private final InvitationConfigurationProperties invitationProps;
  private final NotificationConfigurationProperties notificationProps;

  public TeamServiceImpl(
      final TeamMapper teamMapper,
      final TeamMemberMapper teamMemberMapper,
      final TeamInvitationMapper teamInvitationMapper,
      final UserMapper userMapper,
      final MessagingService messagingService,
      final InvitationConfigurationProperties invitationProps,
      final NotificationConfigurationProperties notificationProps) {
    this.teamMapper = teamMapper;
    this.teamMemberMapper = teamMemberMapper;
    this.teamInvitationMapper = teamInvitationMapper;
    this.userMapper = userMapper;
    this.messagingService = messagingService;
    this.invitationProps = invitationProps;
    this.notificationProps = notificationProps;
  }

  @Override
  public TeamDtos.TeamResponse createTeam(final UUID callerId, final TeamDtos.CreateTeamRequest request) {
    final var team = new Team();
    team.setId(UUID.randomUUID());
    team.setName(request.name().strip());
    team.setSlug(request.slug() != null && !request.slug().isBlank() ? request.slug().strip() : null);
    team.setStatus(TeamStatus.ACTIVE);
    team.setCreatedByUserId(callerId);
    team.setCreatedAt(LocalDateTime.now());
    team.setUpdatedAt(LocalDateTime.now());
    team.setCreatedBy(callerId.toString());
    team.setUpdatedBy(callerId.toString());
    teamMapper.insert(team);

    final var ownerMember = new TeamMember();
    ownerMember.setId(UUID.randomUUID());
    ownerMember.setTeamId(team.getId());
    ownerMember.setUserId(callerId);
    ownerMember.setRole(TeamRole.OWNER);
    ownerMember.setStatus(TeamMemberStatus.ACTIVE);
    ownerMember.setCreatedAt(LocalDateTime.now());
    ownerMember.setUpdatedAt(LocalDateTime.now());
    teamMemberMapper.insert(ownerMember);

    log.info("Team created: teamId={} createdBy={}", team.getId(), callerId);
    return toTeamResponse(team);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeamDtos.TeamResponse> listMyTeams(final UUID callerId) {
    return teamMapper.findByMemberUserId(callerId).stream()
        .map(this::toTeamResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TeamDtos.TeamWithMembersResponse getTeam(final UUID teamId, final UUID callerId) {
    final var team = requireTeam(teamId);
    requireActiveMember(teamId, callerId);
    final var members = teamMemberMapper.findActiveByTeamId(teamId).stream()
        .map(this::toMemberResponse)
        .toList();
    return new TeamDtos.TeamWithMembersResponse(
        team.getId(), team.getName(), team.getSlug(), team.getStatus().name(),
        team.getCreatedByUserId(), team.getCreatedAt(), team.getUpdatedAt(), members);
  }

  @Override
  public TeamDtos.TeamResponse updateTeam(final UUID teamId, final UUID callerId, final TeamDtos.UpdateTeamRequest request) {
    final var team = requireTeam(teamId);
    final var callerMember = requireActiveMember(teamId, callerId);
    if (callerMember.getRole() != TeamRole.OWNER && callerMember.getRole() != TeamRole.ADMIN) {
      throw new AccessDeniedException("Only OWNER or ADMIN can update team details");
    }
    if (request.name() != null && !request.name().isBlank()) {
      team.setName(request.name().strip());
    }
    if (request.status() != null && !request.status().isBlank()) {
      team.setStatus(TeamStatus.valueOf(request.status()));
    }
    team.setUpdatedAt(LocalDateTime.now());
    team.setUpdatedBy(callerId.toString());
    teamMapper.update(team);
    log.info("Team updated: teamId={} by={}", teamId, callerId);
    return toTeamResponse(team);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeamDtos.TeamMemberResponse> listMembers(final UUID teamId, final UUID callerId) {
    requireTeam(teamId);
    requireActiveMember(teamId, callerId);
    return teamMemberMapper.findActiveByTeamId(teamId).stream()
        .map(this::toMemberResponse)
        .toList();
  }

  @Override
  public TeamDtos.TeamInvitationResponse sendInvitation(
      final UUID teamId,
      final UUID callerId,
      final TeamDtos.SendTeamInvitationRequest request) {

    final var team = requireTeam(teamId);
    if (team.getStatus() != TeamStatus.ACTIVE) {
      throw new AccessDeniedException("Cannot invite to an archived team");
    }
    final var callerMember = requireActiveMember(teamId, callerId);
    if (callerMember.getRole() != TeamRole.OWNER && callerMember.getRole() != TeamRole.ADMIN) {
      throw new AccessDeniedException("Only OWNER or ADMIN can send invitations");
    }

    final String normalizedEmail = request.email().toLowerCase();
    final String role = (request.role() != null && !request.role().isBlank()) ? request.role() : "MEMBER";

    if (teamInvitationMapper.existsPendingForTeamAndEmail(teamId, normalizedEmail)) {
      throw new InvitationAlreadyPendingException(normalizedEmail);
    }

    userMapper.findByEmail(normalizedEmail).ifPresent(existingUser -> {
      if (teamMemberMapper.existsActiveByTeamIdAndUserId(teamId, existingUser.getId())) {
        throw new TenantMembershipAlreadyExistsException();
      }
    });

    final byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    final String tokenValue = HexFormat.of().formatHex(bytes);

    final var invitation = new TeamInvitation();
    invitation.setId(UUID.randomUUID());
    invitation.setTeamId(teamId);
    invitation.setInvitedEmail(normalizedEmail);
    invitation.setInvitedBy(callerId);
    invitation.setRole(TeamRole.valueOf(role));
    invitation.setToken(tokenValue);
    invitation.setStatus(TeamInvitationStatus.PENDING);
    invitation.setExpiresAt(Instant.now().plus(invitationProps.tokenTtl()));
    invitation.setCreatedAt(LocalDateTime.now());
    invitation.setUpdatedAt(LocalDateTime.now());
    invitation.setCreatedBy(callerId.toString());
    invitation.setUpdatedBy(callerId.toString());

    final int inserted = teamInvitationMapper.insertIfNotExists(invitation);
    if (inserted == 0) {
      throw new InvitationAlreadyPendingException(normalizedEmail);
    }

    sendInvitationNotification(invitation, team, callerId);

    log.info("Team invitation sent: invitationId={} teamId={} email={} role={}",
        invitation.getId(), teamId, normalizedEmail, role);

    return toInvitationResponse(invitation, team.getName());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeamDtos.TeamInvitationResponse> listInvitations(final UUID teamId, final UUID callerId) {
    final var team = requireTeam(teamId);
    requireActiveMember(teamId, callerId);
    return teamInvitationMapper.findPendingByTeamId(teamId).stream()
        .map(inv -> toInvitationResponse(inv, team.getName()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TeamDtos.TeamInvitationPreviewResponse previewInvitation(final String token) {
    final var invitation = requirePendingInvitation(token);
    final var team = requireTeam(invitation.getTeamId());
    final boolean requiresSignup = !userMapper.existsByEmail(invitation.getInvitedEmail());
    return new TeamDtos.TeamInvitationPreviewResponse(
        invitation.getId(), team.getName(), invitation.getInvitedEmail(),
        invitation.getRole().name(), invitation.getExpiresAt(), requiresSignup);
  }

  @Override
  public TeamDtos.AcceptTeamInvitationResponse acceptInvitation(final String token, final UUID callerId) {
    final var invitation = requirePendingInvitation(token);
    final var team = requireTeam(invitation.getTeamId());

    final var callerUser = userMapper.findById(callerId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + callerId));

    if (!callerUser.getEmail().equalsIgnoreCase(invitation.getInvitedEmail())) {
      throw new AccessDeniedException("Invitation email does not match your account");
    }

    if (teamMemberMapper.existsActiveByTeamIdAndUserId(invitation.getTeamId(), callerId)) {
      throw new TenantMembershipAlreadyExistsException();
    }

    final var member = new TeamMember();
    member.setId(UUID.randomUUID());
    member.setTeamId(invitation.getTeamId());
    member.setUserId(callerId);
    member.setRole(invitation.getRole());
    member.setStatus(TeamMemberStatus.ACTIVE);
    member.setCreatedAt(LocalDateTime.now());
    member.setUpdatedAt(LocalDateTime.now());
    teamMemberMapper.insert(member);

    teamInvitationMapper.markAccepted(invitation.getId(), Instant.now(), LocalDateTime.now());

    log.info("Team invitation accepted: invitationId={} teamId={} userId={}",
        invitation.getId(), invitation.getTeamId(), callerId);

    return new TeamDtos.AcceptTeamInvitationResponse(team.getId(), team.getName());
  }

  @Override
  public void revokeInvitation(final UUID teamId, final UUID invitationId, final UUID callerId) {
    requireTeam(teamId);
    final var invitation = teamInvitationMapper.findById(invitationId)
        .orElseThrow(TeamInvitationNotFoundException::new);

    if (!invitation.getTeamId().equals(teamId)) {
      throw new TeamInvitationNotFoundException();
    }
    if (invitation.getStatus() != TeamInvitationStatus.PENDING) {
      throw new TeamInvitationNotFoundException("Invitation is no longer pending");
    }

    final boolean isInviter = invitation.getInvitedBy().equals(callerId);
    if (!isInviter) {
      final var callerMember = requireActiveMember(teamId, callerId);
      if (callerMember.getRole() != TeamRole.OWNER && callerMember.getRole() != TeamRole.ADMIN) {
        throw new AccessDeniedException("Insufficient role to revoke this invitation");
      }
    }

    teamInvitationMapper.updateStatus(invitationId, TeamInvitationStatus.REVOKED.name(), LocalDateTime.now());
    log.info("Team invitation revoked: invitationId={} teamId={} by={}", invitationId, teamId, callerId);
  }

  @Override
  public void removeMember(final UUID teamId, final UUID targetUserId, final UUID callerId) {
    requireTeam(teamId);
    final var callerMember = requireActiveMember(teamId, callerId);

    if (callerMember.getRole() != TeamRole.OWNER && callerMember.getRole() != TeamRole.ADMIN) {
      throw new AccessDeniedException("Only OWNER or ADMIN can remove members");
    }

    final var targetMember = teamMemberMapper.findActiveByTeamIdAndUserId(teamId, targetUserId)
        .orElseThrow(() -> new MembershipNotFoundException("Member not found in team: " + targetUserId));

    if (targetMember.getRole() == TeamRole.OWNER) {
      final long ownerCount = teamMemberMapper.countActiveOwnersByTeamId(teamId);
      if (ownerCount <= 1) {
        throw new AccessDeniedException("Cannot remove the last OWNER of a team");
      }
    }

    teamMemberMapper.updateRoleAndStatus(
        targetMember.getId(),
        targetMember.getRole().name(),
        TeamMemberStatus.REMOVED.name(),
        LocalDateTime.now());

    log.info("Team member removed: teamId={} userId={} by={}", teamId, targetUserId, callerId);
  }

  @Override
  public void updateMemberRole(
      final UUID teamId,
      final UUID targetUserId,
      final UUID callerId,
      final TeamDtos.UpdateMemberRoleRequest request) {

    requireTeam(teamId);
    final var callerMember = requireActiveMember(teamId, callerId);

    if (callerMember.getRole() != TeamRole.OWNER && callerMember.getRole() != TeamRole.ADMIN) {
      throw new AccessDeniedException("Only OWNER or ADMIN can change member roles");
    }

    final var targetMember = teamMemberMapper.findActiveByTeamIdAndUserId(teamId, targetUserId)
        .orElseThrow(() -> new MembershipNotFoundException("Member not found in team: " + targetUserId));

    final var newRole = TeamRole.valueOf(request.role());

    // Prevent demoting the last OWNER
    if (targetMember.getRole() == TeamRole.OWNER && newRole != TeamRole.OWNER) {
      final long ownerCount = teamMemberMapper.countActiveOwnersByTeamId(teamId);
      if (ownerCount <= 1) {
        throw new AccessDeniedException("Cannot demote the last OWNER of a team");
      }
    }

    // ADMIN cannot promote to OWNER
    if (callerMember.getRole() == TeamRole.ADMIN && newRole == TeamRole.OWNER) {
      throw new AccessDeniedException("ADMIN cannot promote a member to OWNER");
    }

    teamMemberMapper.updateRoleAndStatus(
        targetMember.getId(),
        newRole.name(),
        TeamMemberStatus.ACTIVE.name(),
        LocalDateTime.now());

    log.info("Team member role updated: teamId={} userId={} newRole={} by={}", teamId, targetUserId, newRole, callerId);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private Team requireTeam(final UUID teamId) {
    return teamMapper.findById(teamId).orElseThrow(TeamNotFoundException::new);
  }

  private TeamMember requireActiveMember(final UUID teamId, final UUID userId) {
    return teamMemberMapper.findActiveByTeamIdAndUserId(teamId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not an active member of this team"));
  }

  private TeamInvitation requirePendingInvitation(final String token) {
    final var invitation = teamInvitationMapper.findByToken(token)
        .orElseThrow(TeamInvitationNotFoundException::new);
    if (!invitation.isPending()) {
      throw new TeamInvitationNotFoundException();
    }
    return invitation;
  }

  private void sendInvitationNotification(final TeamInvitation invitation, final Team team, final UUID inviterId) {
    try {
      final var inviter = userMapper.findById(inviterId).orElse(null);
      final String inviterName = inviter != null
          ? inviter.getFirstName() + " " + inviter.getLastName()
          : team.getName();
      final String acceptUrl = notificationProps.baseUrl() + "/team-invite/" + invitation.getToken();
      final var event = new NotificationEvent(
          invitation.getInvitedEmail(),
          notificationProps.defaultLocale(),
          NotificationEventType.INVITATION,
          Map.of(
              "inviterName", inviterName,
              "tenantName", team.getName(),
              "authority", invitation.getRole().name(),
              "acceptUrl", acceptUrl,
              "expiresAt", invitation.getExpiresAt().toString()),
          Instant.now());
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish team invitation notification for invitationId={}", invitation.getId(), e);
    }
  }

  private TeamDtos.TeamResponse toTeamResponse(final Team team) {
    return new TeamDtos.TeamResponse(
        team.getId(), team.getName(), team.getSlug(), team.getStatus().name(),
        team.getCreatedByUserId(), team.getCreatedAt(), team.getUpdatedAt());
  }

  private TeamDtos.TeamMemberResponse toMemberResponse(final TeamMemberWithUser m) {
    return new TeamDtos.TeamMemberResponse(
        m.getId(), m.getTeamId(), m.getUserId(),
        m.getUserEmail(), m.getUserFirstName(), m.getUserLastName(),
        m.getRole().name(), m.getStatus().name(), m.getCreatedAt());
  }

  private TeamDtos.TeamInvitationResponse toInvitationResponse(final TeamInvitation inv, final String teamName) {
    return new TeamDtos.TeamInvitationResponse(
        inv.getId(), inv.getTeamId(), teamName, inv.getInvitedEmail(),
        inv.getRole().name(), inv.getStatus().name(),
        inv.getExpiresAt(),
        inv.getCreatedAt() != null ? inv.getCreatedAt().toInstant(ZoneOffset.UTC) : null);
  }
}
