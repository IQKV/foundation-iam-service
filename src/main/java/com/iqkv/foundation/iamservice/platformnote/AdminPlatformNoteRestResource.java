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

package com.iqkv.foundation.iamservice.platformnote;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformnote.dto.PlatformNoteDtos;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Platform Health Notes — operator-only scratch-pad for tracking incidents,
 * maintenance windows, and operational observations.
 *
 * <p>Notes are strictly internal: they are never surfaced to tenants or end-users.
 * This endpoint exists to demonstrate how a SaaS addon can wire real CRUD
 * operations to a secured backend while following the same patterns used by
 * the rest of the IAM service (security, validation, DTO mapping, pagination).
 *
 * <p>All operations require the {@code PLATFORM_ADMIN} authority.
 * No {@code X-Tenant-ID} header is consumed — notes are platform-scoped.
 */
@RestController
@RequestMapping("/api/v1/iam/admin/platform-notes")
@Tag(name = "Platform Health Notes",
     description = "Operator note-pad for tracking incidents and platform health observations — "
                   + "requires PLATFORM_ADMIN authority. Notes are internal and never tenant-visible.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Validated
public class AdminPlatformNoteRestResource {

  private final PlatformNoteService noteService;

  public AdminPlatformNoteRestResource(final PlatformNoteService noteService) {
    this.noteService = noteService;
  }

  // ── List / Count ──────────────────────────────────────────────────────────

  @GetMapping
  @Operation(summary = "List platform notes",
             description = "Returns a paginated, sorted, and optionally filtered list of platform health notes. "
                           + "Supports free-text search across title and body.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of notes returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PagedPlatformNoteResponse> listNotes(
      @ModelAttribute PlatformNoteDtos.PlatformNoteListQuery query) {
    return ResponseEntity.ok(noteService.list(query));
  }

  @GetMapping("/count")
  @Operation(summary = "Count platform notes",
             description = "Returns the total number of platform notes matching the supplied filters. "
                           + "Accepts the same filter parameters as the list endpoint.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Count returned",
                   content = @Content(schema = @Schema(implementation = PlatformNoteDtos.PlatformNoteCountResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PlatformNoteCountResponse> countNotes(
      @ModelAttribute PlatformNoteDtos.PlatformNoteListQuery query) {
    return ResponseEntity.ok(noteService.count(query));
  }

  // ── Single-resource reads ─────────────────────────────────────────────────

  @GetMapping("/{id}")
  @Operation(summary = "Get platform note by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Note found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Note not found", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PlatformNoteResponse> getNote(
      @Parameter(description = "Note UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(noteService.getById(id));
  }

  // ── Create ────────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(summary = "Create platform note",
             description = "Creates a new platform health note with status OPEN. "
                           + "The authenticated operator is recorded as the author.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Note created",
                   headers = @Header(name = "Location", description = "URL of the created note",
                                     schema = @Schema(type = "string", format = "uri"))),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PlatformNoteResponse> createNote(
      @Valid @RequestBody PlatformNoteDtos.CreatePlatformNoteRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    final UUID actorId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
    final PlatformNoteDtos.PlatformNoteResponse created = noteService.create(actorId, request);
    final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  // ── Full update ───────────────────────────────────────────────────────────

  @PutMapping("/{id}")
  @Operation(summary = "Replace platform note",
             description = "Full replacement of title, body, severity, and status. All fields are required.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Note updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Note not found", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PlatformNoteResponse> updateNote(
      @Parameter(description = "Note UUID") @PathVariable UUID id,
      @Valid @RequestBody PlatformNoteDtos.UpdatePlatformNoteRequest request) {
    return ResponseEntity.ok(noteService.update(id, request));
  }

  // ── Partial update ────────────────────────────────────────────────────────

  @PatchMapping("/{id}")
  @Operation(summary = "Partially update platform note",
             description = "Updates only the supplied fields. Omitted fields are left unchanged. "
                           + "Useful for quick status transitions (e.g. OPEN → RESOLVED).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Note patched"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Note not found", content = @Content)
  })
  public ResponseEntity<PlatformNoteDtos.PlatformNoteResponse> patchNote(
      @Parameter(description = "Note UUID") @PathVariable UUID id,
      @RequestBody PlatformNoteDtos.PatchPlatformNoteRequest request) {
    return ResponseEntity.ok(noteService.patch(id, request));
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete platform note",
             description = "Permanently removes the note. "
                           + "Consider patching status to ARCHIVED first if you want to retain it for auditing.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Note deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @ApiResponse(responseCode = "403", description = "Access denied — PLATFORM_ADMIN required", content = @Content),
      @ApiResponse(responseCode = "404", description = "Note not found", content = @Content)
  })
  public ResponseEntity<Void> deleteNote(
      @Parameter(description = "Note UUID") @PathVariable UUID id) {
    noteService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
