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

import java.util.List;
import java.util.UUID;

public interface InvitationService {

  /**
   * Creates and sends an invitation email.
   *
   * @param tenantKey  target tenant
   * @param inviterId  UUID of the user sending the invite (must be TENANT_OWNER or ADMIN)
   * @param request    email + role
   * @return the created invitation summary
   */
  InvitationDtos.InvitationResponse sendInvitation(
      String tenantKey,
      UUID inviterId,
      InvitationDtos.SendInvitationRequest request);

  /**
   * Returns the invitation preview for the accept page.
   * Returns empty if the token is not found, expired, or already used — callers get 404.
   */
  InvitationDtos.InvitationPreviewResponse previewInvitation(String token);

  /**
   * Accepts an invitation.
   * Creates the user account if one does not exist, then creates the membership.
   *
   * @param token   the invitation token from the email link
   * @param request firstName/lastName/password (firstName+lastName required for new users)
   * @return a token pair scoped to the invited tenant
   */
  InvitationDtos.AcceptInvitationResponse acceptInvitation(
      String token,
      InvitationDtos.AcceptInvitationRequest request);

  /**
   * Revokes a PENDING invitation.
   * Only the inviter, a TENANT_OWNER, or an ADMIN of the tenant may revoke.
   *
   * @param tenantKey    tenant the invitation belongs to
   * @param invitationId invitation to revoke
   * @param requesterId  UUID of the user requesting revocation
   */
  void revokeInvitation(String tenantKey, UUID invitationId, UUID requesterId);

  /**
   * Lists all PENDING invitations for a tenant.
   * Caller must be TENANT_OWNER or ADMIN.
   */
  List<InvitationDtos.InvitationResponse> listInvitations(String tenantKey);
}
