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

import java.util.List;
import java.util.UUID;

public interface TeamService {

  TeamDtos.TeamResponse createTeam(UUID callerId, TeamDtos.CreateTeamRequest request);

  List<TeamDtos.TeamResponse> listMyTeams(UUID callerId);

  TeamDtos.TeamWithMembersResponse getTeam(UUID teamId, UUID callerId);

  TeamDtos.TeamResponse updateTeam(UUID teamId, UUID callerId, TeamDtos.UpdateTeamRequest request);

  List<TeamDtos.TeamMemberResponse> listMembers(UUID teamId, UUID callerId);

  TeamDtos.TeamInvitationResponse sendInvitation(UUID teamId, UUID callerId, TeamDtos.SendTeamInvitationRequest request);

  List<TeamDtos.TeamInvitationResponse> listInvitations(UUID teamId, UUID callerId);

  TeamDtos.TeamInvitationPreviewResponse previewInvitation(String token);

  TeamDtos.AcceptTeamInvitationResponse acceptInvitation(String token, UUID callerId);

  void revokeInvitation(UUID teamId, UUID invitationId, UUID callerId);

  void removeMember(UUID teamId, UUID targetUserId, UUID callerId);

  void updateMemberRole(UUID teamId, UUID targetUserId, UUID callerId, TeamDtos.UpdateMemberRoleRequest request);
}
