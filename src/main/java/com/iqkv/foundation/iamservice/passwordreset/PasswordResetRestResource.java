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

package com.iqkv.foundation.iamservice.passwordreset;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqkv.foundation.iamservice.passwordreset.dto.PasswordResetDtos;

@RestController
@RequestMapping("/api/v1/iam/users/password")
@Tag(name = "Password Reset", description = "Initiate and complete the password reset flow")
public class PasswordResetRestResource {

  private final PasswordResetService passwordResetService;

  public PasswordResetRestResource(final PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/forgot")
  @Operation(summary = "Initiate password reset",
      description = "Sends a password reset email if the address is registered. "
          + "Always returns 200 to avoid email enumeration. Rate-limited per email address.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Reset email sent (or silently ignored if email not found)"),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  })
  public ResponseEntity<Void> forgot(
      @Valid @RequestBody final PasswordResetDtos.ForgotPasswordRequest request) {
    passwordResetService.initiate(request.email());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset")
  @Operation(summary = "Complete password reset",
      description = "Consumes the single-use reset token and sets a new password. "
          + "The token expires after the configured TTL (default 1 hour).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Password updated"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired token")
  })
  public ResponseEntity<Void> reset(
      @Valid @RequestBody final PasswordResetDtos.ResetPasswordRequest request) {
    passwordResetService.complete(request.token(), request.newPassword());
    return ResponseEntity.ok().build();
  }
}
