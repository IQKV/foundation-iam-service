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

package com.iqkv.foundation.iamservice.locale;

import java.util.List;

import com.iqkv.foundation.iamservice.locale.dto.LocaleDtoMapper;
import com.iqkv.foundation.iamservice.locale.dto.LocaleDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/locales")
@Tag(name = "Locale Management", description = "Global locale and language settings")
public class LocaleRestResource {

  private final LocaleService localeService;

  public LocaleRestResource(final LocaleService localeService) {
    this.localeService = localeService;
  }

  @GetMapping
  @Operation(summary = "Get active locales", description = "Retrieves a list of all active locales supported by the platform.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of active locales retrieved successfully")
  })
  public ResponseEntity<List<LocaleDtos.LocaleResponse>> getAllActiveLocales() {
    final List<LocaleDtos.LocaleResponse> locales = localeService.getAllActiveLocales()
        .stream()
        .map(LocaleDtoMapper::toResponse)
        .toList();
    return ResponseEntity.ok(locales);
  }
}
