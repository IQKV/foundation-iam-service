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

import java.util.UUID;

import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/team-invitations")
@Tag(name = "Team Invitations", description = "Accept or preview team invitations by token")
public class TeamInvitationRestResource {

  private final TeamService teamService;

  public TeamInvitationRestResource(final TeamService teamService) {
    this.teamService = teamService;
  }

  @GetMapping("/{token}")
  @Operation(summary = "Preview a team invitation",
      description = "Returns team name, invited email, role, and expiry. Returns 404 for expired, revoked, or non-existent tokens.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation preview returned"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer valid")
  })
  public ResponseEntity<TeamDtos.TeamInvitationPreviewResponse> previewInvitation(
      @PathVariable final String token) {
    return ResponseEntity.ok(teamService.previewInvitation(token));
  }

  @PostMapping("/{token}/accept")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Accept a team invitation",
      description = "Accepts the invitation for the authenticated user. The caller's email must match the invited email. "
                    + "The user must already have an account (team invitations require an existing authenticated user).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation accepted"),
      @ApiResponse(responseCode = "403", description = "Email mismatch or already a member"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer valid"),
      @ApiResponse(responseCode = "409", description = "Already a member of this team")
  })
  public ResponseEntity<TeamDtos.AcceptTeamInvitationResponse> acceptInvitation(
      @PathVariable final String token,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID callerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    return ResponseEntity.ok(teamService.acceptInvitation(token, callerId));
  }
}
