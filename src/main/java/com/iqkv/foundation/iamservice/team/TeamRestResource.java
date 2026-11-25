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

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Lightweight collaboration groups within the platform workspace")
public class TeamRestResource {

  private final TeamService teamService;

  public TeamRestResource(final TeamService teamService) {
    this.teamService = teamService;
  }

  // -------------------------------------------------------------------------
  // Team CRUD
  // -------------------------------------------------------------------------

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Create a team",
      description = "Creates a new team and automatically adds the caller as OWNER.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Team created"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<TeamDtos.TeamResponse> createTeam(
      @Valid @RequestBody final TeamDtos.CreateTeamRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final var response = teamService.createTeam(callerId, request);
    return ResponseEntity.created(URI.create("/api/v1/teams/" + response.id())).body(response);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "List my teams",
      description = "Returns all teams the authenticated user is an active member of.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Team list returned"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<List<TeamDtos.TeamResponse>> listMyTeams(
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.listMyTeams(callerId));
  }

  @GetMapping("/{teamId}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get team with members",
      description = "Returns team details and the full active member list. Caller must be an active team member.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Team returned"),
      @ApiResponse(responseCode = "403", description = "Not a team member"),
      @ApiResponse(responseCode = "404", description = "Team not found")
  })
  public ResponseEntity<TeamDtos.TeamWithMembersResponse> getTeam(
      @PathVariable final UUID teamId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.getTeam(teamId, callerId));
  }

  @PatchMapping("/{teamId}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update team",
      description = "Updates name or status. Requires OWNER or ADMIN role within the team.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Team updated"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "403", description = "Insufficient role"),
      @ApiResponse(responseCode = "404", description = "Team not found")
  })
  public ResponseEntity<TeamDtos.TeamResponse> updateTeam(
      @PathVariable final UUID teamId,
      @Valid @RequestBody final TeamDtos.UpdateTeamRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.updateTeam(teamId, callerId, request));
  }

  // -------------------------------------------------------------------------
  // Members
  // -------------------------------------------------------------------------

  @GetMapping("/{teamId}/members")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "List team members",
      description = "Returns all active members of the team. Caller must be an active member.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Member list returned"),
      @ApiResponse(responseCode = "403", description = "Not a team member"),
      @ApiResponse(responseCode = "404", description = "Team not found")
  })
  public ResponseEntity<List<TeamDtos.TeamMemberResponse>> listMembers(
      @PathVariable final UUID teamId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.listMembers(teamId, callerId));
  }

  @DeleteMapping("/{teamId}/members/{userId}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Remove a team member",
      description = "Marks the member as REMOVED. Requires OWNER or ADMIN role. Cannot remove the last OWNER.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Member removed"),
      @ApiResponse(responseCode = "403", description = "Insufficient role or last OWNER guard"),
      @ApiResponse(responseCode = "404", description = "Team or member not found")
  })
  public ResponseEntity<Void> removeMember(
      @PathVariable final UUID teamId,
      @PathVariable final UUID userId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    teamService.removeMember(teamId, userId, callerId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{teamId}/members/{userId}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update member role",
      description = "Changes the role of a team member. OWNER or ADMIN only. ADMIN cannot promote to OWNER. Cannot demote the last OWNER.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Role updated — returns updated member list"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "403", description = "Insufficient role or last-OWNER guard"),
      @ApiResponse(responseCode = "404", description = "Team or member not found")
  })
  public ResponseEntity<Void> updateMemberRole(
      @PathVariable final UUID teamId,
      @PathVariable final UUID userId,
      @Valid @RequestBody final TeamDtos.UpdateMemberRoleRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    teamService.updateMemberRole(teamId, userId, callerId, request);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Invitations (team-scoped management)
  // -------------------------------------------------------------------------

  @PostMapping("/{teamId}/invitations")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Send a team invitation",
      description = "Creates a PENDING invitation and sends a notification. Requires OWNER or ADMIN role.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Invitation created"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "403", description = "Insufficient role"),
      @ApiResponse(responseCode = "404", description = "Team not found"),
      @ApiResponse(responseCode = "409", description = "Pending invitation already exists or user already a member")
  })
  public ResponseEntity<TeamDtos.TeamInvitationResponse> sendInvitation(
      @PathVariable final UUID teamId,
      @Valid @RequestBody final TeamDtos.SendTeamInvitationRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final var response = teamService.sendInvitation(teamId, callerId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{teamId}/invitations")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "List pending team invitations",
      description = "Returns all PENDING invitations for the team. Caller must be an active member.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation list returned"),
      @ApiResponse(responseCode = "403", description = "Not a team member"),
      @ApiResponse(responseCode = "404", description = "Team not found")
  })
  public ResponseEntity<List<TeamDtos.TeamInvitationResponse>> listInvitations(
      @PathVariable final UUID teamId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.listInvitations(teamId, callerId));
  }

  @DeleteMapping("/{teamId}/invitations/{invitationId}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Revoke a team invitation",
      description = "Sets invitation status to REVOKED. Inviter, OWNER, or ADMIN may revoke.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Invitation revoked"),
      @ApiResponse(responseCode = "403", description = "Insufficient role"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer pending")
  })
  public ResponseEntity<Void> revokeInvitation(
      @PathVariable final UUID teamId,
      @PathVariable final UUID invitationId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    teamService.revokeInvitation(teamId, invitationId, callerId);
    return ResponseEntity.noContent().build();
  }
}
