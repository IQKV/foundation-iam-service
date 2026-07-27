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

package com.iqkv.foundation.iamservice.invitation;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Invitations", description = "Send, preview, accept, revoke, and list tenant invitations")
public class InvitationRestResource {

  private final InvitationService invitationService;

  public InvitationRestResource(final InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  // -------------------------------------------------------------------------
  // Tenant-scoped management endpoints (require JWT + TENANT_OWNER or ADMIN)
  // -------------------------------------------------------------------------

  @PostMapping("/api/v1/iam/tenants/{tenantKey}/invitations")
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Send an invitation",
      description = "Creates a PENDING invitation and sends an email to the invitee. "
                    + "Requires a JWT scoped to {tenantKey} — the gateway resolves tenant context from the "
                    + "X-Tenant-ID request header, which must match the tenantKey path variable. "
                    + "Requires TENANT_OWNER or ADMIN authority within that tenant. "
                    + "Authority defaults to MEMBER when omitted. TENANT_OWNER is not grantable via invitation.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Invitation created and email sent"),
      @ApiResponse(responseCode = "400", description = "Invalid request body"),
      @ApiResponse(responseCode = "403", description = "Insufficient authority"),
      @ApiResponse(responseCode = "409", description = "Pending invitation already exists or user is already a member")
  })
  public ResponseEntity<InvitationDtos.InvitationResponse> sendInvitation(
      @PathVariable final String tenantKey,
      @Valid @RequestBody final InvitationDtos.SendInvitationRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final UUID inviterId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final InvitationDtos.InvitationResponse response =
        invitationService.sendInvitation(tenantKey, inviterId, request);
    return ResponseEntity
        .created(URI.create("/api/v1/iam/tenants/" + tenantKey + "/invitations/" + response.invitationId()))
        .body(response);
  }

  @GetMapping("/api/v1/iam/tenants/{tenantKey}/invitations")
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "List pending invitations",
      description = "Returns all PENDING invitations for the tenant. "
                    + "Requires a JWT scoped to {tenantKey} via the X-Tenant-ID header. "
                    + "Requires TENANT_OWNER or ADMIN authority within that tenant.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation list returned"),
      @ApiResponse(responseCode = "403", description = "Insufficient authority")
  })
  public ResponseEntity<List<InvitationDtos.InvitationResponse>> listInvitations(
      @PathVariable final String tenantKey,
      @AuthenticationPrincipal final Jwt jwt) {

    // Authority check is enforced in the service via MembershipService.resolveMembership
    // The JWT must carry a membership for this tenantKey — gateway propagates X-Tenant-ID.
    return ResponseEntity.ok(invitationService.listInvitations(tenantKey));
  }

  @DeleteMapping("/api/v1/iam/tenants/{tenantKey}/invitations/{invitationId}")
  @SecurityRequirement(name = "bearerAuth")
  @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'ADMIN')")
  @Operation(
      summary = "Revoke an invitation",
      description = "Sets the invitation status to REVOKED. "
                    + "Requires a JWT scoped to {tenantKey} via the X-Tenant-ID header. "
                    + "The original inviter, a TENANT_OWNER, or an ADMIN of that tenant may revoke.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Invitation revoked"),
      @ApiResponse(responseCode = "403", description = "Insufficient authority"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer pending")
  })
  public ResponseEntity<Void> revokeInvitation(
      @PathVariable final String tenantKey,
      @PathVariable final UUID invitationId,
      @AuthenticationPrincipal final Jwt jwt) {

    final UUID requesterId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    invitationService.revokeInvitation(tenantKey, invitationId, requesterId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Public endpoints (no JWT required — accept flow)
  // -------------------------------------------------------------------------

  @GetMapping("/api/v1/iam/invitations/{token}")
  @Operation(
      summary = "Preview an invitation",
      description = "Returns tenant name, invited email, authority, and expiry. "
                    + "The 'requiresSignup' flag tells the UI which form to render. "
                    + "Returns 404 for expired, revoked, or non-existent tokens.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation preview returned"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer valid")
  })
  public ResponseEntity<InvitationDtos.InvitationPreviewResponse> previewInvitation(
      @PathVariable final String token) {
    return ResponseEntity.ok(invitationService.previewInvitation(token));
  }

  @PostMapping("/api/v1/iam/invitations/{token}/accept")
  @Operation(
      summary = "Accept an invitation",
      description = "Accepts the invitation and returns a JWT token pair scoped to the invited tenant. "
                    + "For new users: firstName, lastName, and password are required. "
                    + "For existing users: only password is required (used to verify identity). "
                    + "Returns 404 for expired, revoked, or non-existent tokens.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation accepted, token pair returned"),
      @ApiResponse(responseCode = "400", description = "Invalid request body"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials (existing user path)"),
      @ApiResponse(responseCode = "403", description = "Account locked"),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer valid"),
      @ApiResponse(responseCode = "409", description = "User is already a member of this tenant")
  })
  public ResponseEntity<InvitationDtos.AcceptInvitationResponse> acceptInvitation(
      @PathVariable final String token,
      @Valid @RequestBody final InvitationDtos.AcceptInvitationRequest request) {
    return ResponseEntity.ok(invitationService.acceptInvitation(token, request));
  }
}
