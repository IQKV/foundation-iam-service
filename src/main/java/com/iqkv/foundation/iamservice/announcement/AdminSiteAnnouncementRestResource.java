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

package com.iqkv.foundation.iamservice.announcement;

import java.util.UUID;

import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/admin/announcements")
@Tag(name = "Admin Announcement Management", description = "Platform admin endpoints for managing site-wide announcements")
@SecurityRequirement(name = "bearerAuth")
public class AdminSiteAnnouncementRestResource {

  private final SiteAnnouncementService announcementService;

  public AdminSiteAnnouncementRestResource(final SiteAnnouncementService announcementService) {
    this.announcementService = announcementService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(summary = "Create announcement", description = "Creates a new site-wide announcement with multi-lingual translations.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Announcement created successfully"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  public ResponseEntity<SiteAnnouncementDtos.SiteAnnouncementResponse> create(
      @Valid @RequestBody final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(summary = "Update announcement", description = "Updates an existing site-wide announcement and its translations.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Announcement updated successfully"),
      @ApiResponse(responseCode = "404", description = "Announcement not found")
  })
  public ResponseEntity<SiteAnnouncementDtos.SiteAnnouncementResponse> update(
      @PathVariable final UUID id,
      @Valid @RequestBody final SiteAnnouncementDtos.UpdateSiteAnnouncementRequest request) {
    return ResponseEntity.ok(announcementService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(summary = "Delete announcement", description = "Deletes a site-wide announcement.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Announcement deleted successfully")
  })
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    announcementService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(summary = "Get announcement by ID", description = "Retrieves detailed information about a specific announcement.")
  public ResponseEntity<SiteAnnouncementDtos.SiteAnnouncementResponse> getById(@PathVariable final UUID id) {
    return ResponseEntity.ok(announcementService.getById(id));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @Operation(summary = "List all announcements", description = "Retrieves a paginated list of all announcements.")
  public ResponseEntity<SiteAnnouncementDtos.SiteAnnouncementListResponse> getAll(
      @RequestParam(defaultValue = "20") final int limit,
      @RequestParam(defaultValue = "0") final int offset) {
    return ResponseEntity.ok(announcementService.getAll(limit, offset));
  }
}
