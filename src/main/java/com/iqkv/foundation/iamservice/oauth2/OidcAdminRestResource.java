package com.iqkv.foundation.iamservice.oauth2;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.dto.OidcDtos;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/admin/oidc/users")
@Tag(name = "OIDC Admin", description = "Platform operator operations for linked OIDC identities")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class OidcAdminRestResource {

  private final OidcAdminService oidcAdminService;

  public OidcAdminRestResource(final OidcAdminService oidcAdminService) {
    this.oidcAdminService = oidcAdminService;
  }

  @GetMapping("/{userId}/identities")
  @Operation(summary = "List linked OIDC identities for a user")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Linked identities returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<List<OidcDtos.AdminLinkedIdentityResponse>> listUserIdentities(
      @Parameter(description = "User UUID") @PathVariable final UUID userId) {
    return ResponseEntity.ok(oidcAdminService.listUserIdentities(userId));
  }

  @DeleteMapping("/{userId}/identities/{identityId}")
  @Operation(summary = "Unmerge a linked OIDC identity from a user account",
             description = "Force-removes the selected external identity link from the target user. "
                           + "This admin remediation endpoint intentionally bypasses the self-service unlink guard.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Identity unmerged"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "User or identity not found", content = @Content)
  })
  public ResponseEntity<Void> unmergeIdentity(
      @Parameter(description = "User UUID") @PathVariable final UUID userId,
      @Parameter(description = "Linked identity UUID") @PathVariable final UUID identityId,
      @AuthenticationPrincipal final Jwt jwt) {
    final UUID actorUserId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    oidcAdminService.unmergeIdentity(userId, identityId, actorUserId);
    return ResponseEntity.noContent().build();
  }
}
