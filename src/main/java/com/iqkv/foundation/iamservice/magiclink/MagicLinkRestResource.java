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

package com.iqkv.foundation.iamservice.magiclink;

import jakarta.validation.Valid;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.magiclink.dto.MagicLinkDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/auth/magic-link")
@Tag(name = "Magic Link", description = "Initiate, resend, and exchange magic links for passwordless authentication")
public class MagicLinkRestResource {

  private final MagicLinkService magicLinkService;

  public MagicLinkRestResource(final MagicLinkService magicLinkService) {
    this.magicLinkService = magicLinkService;
  }

  @PostMapping("/initiate")
  @Operation(summary = "Initiate magic link authentication",
             description = "Sends a magic link to the provided email address. "
                           + "Always returns 204 to avoid email enumeration. Rate-limited per email address.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Magic link initiated (email sent if user exists)"),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  })
  public ResponseEntity<Void> initiate(
      @Valid @RequestBody final MagicLinkDtos.InitiateMagicLinkRequest request) {
    magicLinkService.initiate(request.email(), request.tenantKey());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/resend")
  @Operation(summary = "Resend magic link authentication",
             description = "Resends a magic link to the provided email address if one exists. "
                           + "Always returns 204 to avoid email enumeration. Rate-limited per email address.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Magic link resent (email sent if user exists and token exists)"),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  })
  public ResponseEntity<Void> resend(
      @Valid @RequestBody final MagicLinkDtos.InitiateMagicLinkRequest request) {
    magicLinkService.resend(request.email(), request.tenantKey());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/exchange")
  @Operation(summary = "Exchange magic link token for JWT tokens",
             description = "Consumes a magic link token and returns access and refresh tokens if valid.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token exchange successful"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired token")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> exchange(
      @Valid @RequestBody final MagicLinkDtos.ExchangeMagicLinkRequest request) {
    return ResponseEntity.ok(magicLinkService.exchange(request.token()));
  }
}
