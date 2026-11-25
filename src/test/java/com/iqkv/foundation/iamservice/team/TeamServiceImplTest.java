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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.infrastructure.config.InvitationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.shared.exception.InvitationAlreadyPendingException;
import com.iqkv.foundation.iamservice.shared.exception.TeamInvitationNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.TeamNotFoundException;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService Unit Tests")
class TeamServiceImplTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID MEMBER_ID = UUID.randomUUID();
  private static final UUID OTHER_USER_ID = UUID.randomUUID();

  @Mock
  private TeamMapper teamMapper;
  @Mock
  private TeamMemberMapper teamMemberMapper;
  @Mock
  private TeamInvitationMapper teamInvitationMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private MessagingService messagingService;
  @Mock
  private InvitationConfigurationProperties invitationProps;
  @Mock
  private NotificationConfigurationProperties notificationProps;

  private TeamServiceImpl teamService;

  @BeforeEach
  void setUp() {
    teamService = new TeamServiceImpl(
        teamMapper,
        teamMemberMapper,
        teamInvitationMapper,
        userMapper,
        messagingService,
        invitationProps,
        notificationProps
    );

    lenient().when(invitationProps.tokenTtl()).thenReturn(Duration.ofHours(72));
    lenient().when(notificationProps.baseUrl()).thenReturn("https://app.example.com");
    lenient().when(notificationProps.defaultLocale()).thenReturn("en");
  }

  // -------------------------------------------------------------------------
  // createTeam
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should persist team and owner member, and return correct name")
  void shouldCreateTeamWithOwnerMember() {
    // Arrange
    final var request = new TeamDtos.CreateTeamRequest("My Team", "my-team");

    // Act
    final var result = teamService.createTeam(OWNER_ID, request);

    // Assert
    assertThat(result.name()).isEqualTo("My Team");
    assertThat(result.status()).isEqualTo("ACTIVE");
    assertThat(result.createdByUserId()).isEqualTo(OWNER_ID);
    verify(teamMapper).insert(any(Team.class));
    verify(teamMemberMapper).insert(any(TeamMember.class));
  }

  @Test
  @DisplayName("Should set slug to null when not provided")
  void shouldSetSlugToNullWhenNotProvided() {
    // Arrange
    final var request = new TeamDtos.CreateTeamRequest("My Team", null);

    // Act
    final var result = teamService.createTeam(OWNER_ID, request);

    // Assert
    assertThat(result.slug()).isNull();
  }

  // -------------------------------------------------------------------------
  // listMyTeams
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should return mapped responses for all teams the caller belongs to")
  void shouldListMyTeams() {
    // Arrange
    final var team1 = buildTeam(TeamStatus.ACTIVE);
    final var team2 = buildTeam(TeamStatus.ARCHIVED);
    team2.setName("Other Team");
    when(teamMapper.findByMemberUserId(OWNER_ID)).thenReturn(List.of(team1, team2));

    // Act
    final var result = teamService.listMyTeams(OWNER_ID);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("Test Team");
    assertThat(result.get(1).name()).isEqualTo("Other Team");
  }

  // -------------------------------------------------------------------------
  // getTeam
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when caller is not an active member")
  void shouldThrowWhenCallerIsNotActiveMember() {
    // Arrange
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> teamService.getTeam(TEAM_ID, OTHER_USER_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should return team with members when caller is an active member")
  void shouldReturnTeamWithMembersForActiveMember() {
    // Arrange
    final var ownerMember = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    final var ownerWithUser = buildMemberWithUser(OWNER_ID, TeamRole.OWNER);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(ownerMember));
    when(teamMemberMapper.findActiveByTeamId(TEAM_ID)).thenReturn(List.of(ownerWithUser));

    // Act
    final var result = teamService.getTeam(TEAM_ID, OWNER_ID);

    // Assert
    assertThat(result.id()).isEqualTo(TEAM_ID);
    assertThat(result.name()).isEqualTo("Test Team");
    assertThat(result.members()).hasSize(1);
    assertThat(result.members().get(0).userId()).isEqualTo(OWNER_ID);
  }

  // -------------------------------------------------------------------------
  // updateTeam
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when MEMBER tries to update team")
  void shouldThrowWhenMemberTriesToUpdateTeam() {
    // Arrange
    final var memberRole = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(memberRole));
    final var request = new TeamDtos.UpdateTeamRequest("New Name", null);

    // Act & Assert
    assertThatThrownBy(() -> teamService.updateTeam(TEAM_ID, MEMBER_ID, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("OWNER or ADMIN");
  }

  @Test
  @DisplayName("Should allow OWNER to update team name and status")
  void shouldAllowOwnerToUpdateTeam() {
    // Arrange
    final var ownerMember = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(ownerMember));
    final var request = new TeamDtos.UpdateTeamRequest("Renamed Team", "ARCHIVED");

    // Act
    final var result = teamService.updateTeam(TEAM_ID, OWNER_ID, request);

    // Assert
    assertThat(result.name()).isEqualTo("Renamed Team");
    assertThat(result.status()).isEqualTo("ARCHIVED");
    verify(teamMapper).update(any(Team.class));
  }

  // -------------------------------------------------------------------------
  // sendInvitation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when MEMBER tries to send invitation")
  void shouldThrowWhenMemberTriesToSendInvitation() {
    // Arrange
    final var memberRole = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(memberRole));
    final var request = new TeamDtos.SendTeamInvitationRequest("invitee@example.com", "MEMBER");

    // Act & Assert
    assertThatThrownBy(() -> teamService.sendInvitation(TEAM_ID, MEMBER_ID, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("OWNER or ADMIN");
  }

  @Test
  @DisplayName("Should throw AccessDeniedException when inviting to an archived team")
  void shouldThrowWhenSendingInvitationToArchivedTeam() {
    // Arrange
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ARCHIVED)));
    final var request = new TeamDtos.SendTeamInvitationRequest("invitee@example.com", "MEMBER");

    // Act & Assert
    assertThatThrownBy(() -> teamService.sendInvitation(TEAM_ID, OWNER_ID, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("archived");
  }

  @Test
  @DisplayName("Should throw InvitationAlreadyPendingException when pending invitation already exists")
  void shouldThrowWhenPendingInvitationAlreadyExists() {
    // Arrange
    final var ownerMember = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(ownerMember));
    when(teamInvitationMapper.existsPendingForTeamAndEmail(TEAM_ID, "invitee@example.com")).thenReturn(true);
    final var request = new TeamDtos.SendTeamInvitationRequest("invitee@example.com", "MEMBER");

    // Act & Assert
    assertThatThrownBy(() -> teamService.sendInvitation(TEAM_ID, OWNER_ID, request))
        .isInstanceOf(InvitationAlreadyPendingException.class);
  }

  @Test
  @DisplayName("Should persist invitation and attempt notification on happy path")
  void shouldSendInvitationSuccessfully() {
    // Arrange
    final var ownerMember = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(ownerMember));
    when(teamInvitationMapper.existsPendingForTeamAndEmail(TEAM_ID, "invitee@example.com")).thenReturn(false);
    when(userMapper.findByEmail("invitee@example.com")).thenReturn(Optional.empty());
    when(teamInvitationMapper.insertIfNotExists(any(TeamInvitation.class))).thenReturn(1);
    when(userMapper.findById(OWNER_ID)).thenReturn(Optional.empty()); // graceful fallback for notification
    final var request = new TeamDtos.SendTeamInvitationRequest("invitee@example.com", "MEMBER");

    // Act
    final var result = teamService.sendInvitation(TEAM_ID, OWNER_ID, request);

    // Assert
    assertThat(result.email()).isEqualTo("invitee@example.com");
    assertThat(result.role()).isEqualTo("MEMBER");
    assertThat(result.status()).isEqualTo("PENDING");
    verify(teamInvitationMapper).insertIfNotExists(any(TeamInvitation.class));
    verify(messagingService).publishNotification(any());
  }

  // -------------------------------------------------------------------------
  // acceptInvitation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when invitation email does not match caller")
  void shouldThrowWhenInvitationEmailDoesNotMatchCaller() {
    // Arrange
    final var invitation = buildInvitation(TeamInvitationStatus.PENDING);
    final var wrongUser = new User();
    wrongUser.setId(OTHER_USER_ID);
    wrongUser.setEmail("wrong@example.com");
    when(teamInvitationMapper.findByToken("abc123")).thenReturn(Optional.of(invitation));
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(userMapper.findById(OTHER_USER_ID)).thenReturn(Optional.of(wrongUser));

    // Act & Assert
    assertThatThrownBy(() -> teamService.acceptInvitation("abc123", OTHER_USER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("email does not match");
  }

  @Test
  @DisplayName("Should create member and mark invitation accepted on happy path")
  void shouldAcceptInvitationSuccessfully() {
    // Arrange
    final var invitation = buildInvitation(TeamInvitationStatus.PENDING);
    final var invitee = new User();
    invitee.setId(MEMBER_ID);
    invitee.setEmail("invitee@example.com");
    when(teamInvitationMapper.findByToken("abc123")).thenReturn(Optional.of(invitation));
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(userMapper.findById(MEMBER_ID)).thenReturn(Optional.of(invitee));
    when(teamMemberMapper.existsActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(false);

    // Act
    final var result = teamService.acceptInvitation("abc123", MEMBER_ID);

    // Assert
    assertThat(result.teamId()).isEqualTo(TEAM_ID);
    assertThat(result.teamName()).isEqualTo("Test Team");
    verify(teamMemberMapper).insert(any(TeamMember.class));
    verify(teamInvitationMapper).markAccepted(any(), any(Instant.class), any(LocalDateTime.class));
  }

  // -------------------------------------------------------------------------
  // removeMember
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when MEMBER tries to remove another member")
  void shouldThrowWhenMemberTriesToRemoveMember() {
    // Arrange
    final var callerMember = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(callerMember));

    // Act & Assert
    assertThatThrownBy(() -> teamService.removeMember(TEAM_ID, OTHER_USER_ID, MEMBER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("OWNER or ADMIN");
  }

  @Test
  @DisplayName("Should throw AccessDeniedException when removing the last OWNER")
  void shouldThrowWhenRemovingLastOwner() {
    // Arrange
    final var callerOwner = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    final var targetOwner = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(callerOwner));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(targetOwner));
    when(teamMemberMapper.countActiveOwnersByTeamId(TEAM_ID)).thenReturn(1L);

    // Act & Assert
    assertThatThrownBy(() -> teamService.removeMember(TEAM_ID, OWNER_ID, OWNER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("last OWNER");
  }

  @Test
  @DisplayName("Should allow OWNER to remove a MEMBER")
  void shouldAllowOwnerToRemoveMember() {
    // Arrange
    final var callerOwner = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    final var targetMember = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID)).thenReturn(Optional.of(callerOwner));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(targetMember));

    // Act
    teamService.removeMember(TEAM_ID, MEMBER_ID, OWNER_ID);

    // Assert
    verify(teamMemberMapper).updateRoleAndStatus(
        any(UUID.class),
        any(String.class),
        any(String.class),
        any(LocalDateTime.class));
  }

  // -------------------------------------------------------------------------
  // updateMemberRole
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when ADMIN tries to promote a member to OWNER")
  void shouldThrowWhenAdminPromotesToOwner() {
    // Arrange
    final var callerAdmin = buildMember(OTHER_USER_ID, TeamRole.ADMIN, TeamMemberStatus.ACTIVE);
    final var targetMember = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(Optional.of(callerAdmin));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(targetMember));
    final var request = new TeamDtos.UpdateMemberRoleRequest("OWNER");

    // Act & Assert
    assertThatThrownBy(() -> teamService.updateMemberRole(TEAM_ID, MEMBER_ID, OTHER_USER_ID, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("ADMIN cannot promote");
  }

  @Test
  @DisplayName("Should throw AccessDeniedException when demoting the last OWNER")
  void shouldThrowWhenDemotingLastOwner() {
    // Arrange
    final var callerOwner = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    final var targetOwner = buildMember(OWNER_ID, TeamRole.OWNER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    // first call returns caller, second returns target — both are the same OWNER in this scenario
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, OWNER_ID))
        .thenReturn(Optional.of(callerOwner))
        .thenReturn(Optional.of(targetOwner));
    when(teamMemberMapper.countActiveOwnersByTeamId(TEAM_ID)).thenReturn(1L);
    final var request = new TeamDtos.UpdateMemberRoleRequest("MEMBER");

    // Act & Assert
    assertThatThrownBy(() -> teamService.updateMemberRole(TEAM_ID, OWNER_ID, OWNER_ID, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("last OWNER");
  }

  // -------------------------------------------------------------------------
  // revokeInvitation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw AccessDeniedException when MEMBER who is not the inviter tries to revoke")
  void shouldThrowWhenNonInviterMemberRevokes() {
    // Arrange
    final var invitation = buildInvitation(TeamInvitationStatus.PENDING);
    // invitation.invitedBy = OWNER_ID, caller = MEMBER_ID
    final var callerMember = buildMember(MEMBER_ID, TeamRole.MEMBER, TeamMemberStatus.ACTIVE);
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamInvitationMapper.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    when(teamMemberMapper.findActiveByTeamIdAndUserId(TEAM_ID, MEMBER_ID)).thenReturn(Optional.of(callerMember));

    // Act & Assert
    assertThatThrownBy(() -> teamService.revokeInvitation(TEAM_ID, invitation.getId(), MEMBER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Insufficient role");
  }

  @Test
  @DisplayName("Should allow the inviter to revoke their own invitation")
  void shouldAllowInviterToRevokeOwnInvitation() {
    // Arrange
    final var invitation = buildInvitation(TeamInvitationStatus.PENDING);
    // invitation.invitedBy = OWNER_ID, caller = OWNER_ID
    when(teamMapper.findById(TEAM_ID)).thenReturn(Optional.of(buildTeam(TeamStatus.ACTIVE)));
    when(teamInvitationMapper.findById(invitation.getId())).thenReturn(Optional.of(invitation));

    // Act
    teamService.revokeInvitation(TEAM_ID, invitation.getId(), OWNER_ID);

    // Assert
    verify(teamInvitationMapper).updateStatus(
        any(UUID.class),
        any(String.class),
        any(LocalDateTime.class));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Team buildTeam(final TeamStatus status) {
    final var team = new Team();
    team.setId(TEAM_ID);
    team.setName("Test Team");
    team.setStatus(status);
    team.setCreatedByUserId(OWNER_ID);
    team.setCreatedAt(LocalDateTime.now());
    team.setUpdatedAt(LocalDateTime.now());
    return team;
  }

  private TeamMember buildMember(final UUID userId, final TeamRole role, final TeamMemberStatus status) {
    final var m = new TeamMember();
    m.setId(UUID.randomUUID());
    m.setTeamId(TEAM_ID);
    m.setUserId(userId);
    m.setRole(role);
    m.setStatus(status);
    m.setCreatedAt(LocalDateTime.now());
    m.setUpdatedAt(LocalDateTime.now());
    return m;
  }

  private TeamMemberWithUser buildMemberWithUser(final UUID userId, final TeamRole role) {
    final var m = new TeamMemberWithUser();
    m.setId(UUID.randomUUID());
    m.setTeamId(TEAM_ID);
    m.setUserId(userId);
    m.setRole(role);
    m.setStatus(TeamMemberStatus.ACTIVE);
    m.setUserEmail("user@example.com");
    m.setUserFirstName("First");
    m.setUserLastName("Last");
    m.setCreatedAt(LocalDateTime.now());
    m.setUpdatedAt(LocalDateTime.now());
    return m;
  }

  private TeamInvitation buildInvitation(final TeamInvitationStatus status) {
    final var inv = new TeamInvitation();
    inv.setId(UUID.randomUUID());
    inv.setTeamId(TEAM_ID);
    inv.setInvitedEmail("invitee@example.com");
    inv.setInvitedBy(OWNER_ID);
    inv.setRole(TeamRole.MEMBER);
    inv.setToken("abc123");
    inv.setStatus(status);
    inv.setExpiresAt(status == TeamInvitationStatus.PENDING
        ? Instant.now().plus(Duration.ofHours(24))
        : Instant.now().minus(Duration.ofHours(1)));
    inv.setCreatedAt(LocalDateTime.now());
    inv.setUpdatedAt(LocalDateTime.now());
    return inv;
  }
}
