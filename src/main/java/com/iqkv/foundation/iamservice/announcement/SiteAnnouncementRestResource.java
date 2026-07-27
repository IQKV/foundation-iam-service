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

package com.iqkv.foundation.iamservice.announcement;

import java.util.List;

import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/announcements")
@Tag(name = "Public Announcements", description = "Endpoints for retrieving active site-wide announcements")
public class SiteAnnouncementRestResource {

  private final SiteAnnouncementService announcementService;

  public SiteAnnouncementRestResource(final SiteAnnouncementService announcementService) {
    this.announcementService = announcementService;
  }

  @GetMapping
  @Operation(summary = "Get active announcements", description = "Retrieves a list of all active site-wide announcements for a specific locale.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active announcements retrieved successfully")
  })
  public ResponseEntity<List<SiteAnnouncementDtos.SiteAnnouncementResponse>> getActiveByLocale(
      @RequestParam(defaultValue = "en-US") final String locale) {
    return ResponseEntity.ok(announcementService.getActiveByLocale(locale));
  }
}
