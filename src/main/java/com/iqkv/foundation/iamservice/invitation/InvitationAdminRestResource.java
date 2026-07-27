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
import java.util.UUID;

import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/iam/admin/invitations")
@Tag(name = "Invitation Admin",
     description = "Platform operator operations for tenant invitations — requires PLATFORM_ADMIN authority")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class InvitationAdminRestResource {

  private final InvitationService invitationService;

  public InvitationAdminRestResource(final InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @GetMapping
  @Operation(summary = "List invitations",
             description = "Returns a paginated, sorted, and optionally filtered list of invitations across all tenants.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of invitations returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<InvitationDtos.PagedInvitationAdminResponse> listInvitations(
      @ModelAttribute @Valid InvitationDtos.InvitationListQuery query) {
    return ResponseEntity.ok(invitationService.listInvitationsAdmin(query));
  }

  @GetMapping("/count")
  @Operation(summary = "Count invitations",
             description = "Returns the total number of invitations matching the optional filters.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation count returned",
                   content = @Content(schema = @Schema(implementation = InvitationDtos.InvitationCountResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<InvitationDtos.InvitationCountResponse> countInvitations(
      @ModelAttribute @Valid InvitationDtos.InvitationListQuery query) {
    return ResponseEntity.ok(invitationService.countInvitationsAdmin(query));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get invitation by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Invitation found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content)
  })
  public ResponseEntity<InvitationDtos.AdminInvitationResponse> getInvitation(
      @Parameter(description = "Invitation UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(invitationService.getInvitationById(id));
  }

  @PostMapping
  @Operation(summary = "Propose invitation",
             description = "Creates a PENDING invitation for any active tenant and sends the invitation email. "
                           + "The authenticated platform admin is recorded as the inviter.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Invitation created and email sent",
                   headers = @Header(name = "Location", description = "URL of the created invitation",
                                     schema = @Schema(type = "string", format = "uri"))),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Tenant not found or inactive", content = @Content),
      @ApiResponse(responseCode = "409", description = "Pending invitation already exists or user is already a member",
                   content = @Content)
  })
  public ResponseEntity<InvitationDtos.AdminInvitationResponse> proposeInvitation(
      @Valid @RequestBody InvitationDtos.AdminProposeInvitationRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID proposerId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final var created = invitationService.proposeInvitation(proposerId, request);
    final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.invitationId())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke invitation",
             description = "Sets a PENDING invitation status to REVOKED.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Invitation revoked"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Invitation not found or no longer pending", content = @Content)
  })
  public ResponseEntity<Void> revokeInvitation(
      @Parameter(description = "Invitation UUID") @PathVariable UUID id) {
    invitationService.revokeInvitationById(id);
    return ResponseEntity.noContent().build();
  }
}
