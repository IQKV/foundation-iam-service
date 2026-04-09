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

package com.iqkv.iamservice.email;

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

import com.iqkv.iamservice.authentication.AuthenticationService;
import com.iqkv.iamservice.authentication.dto.AuthenticationDtos;

@RestController
@RequestMapping("/api/v1/iam/users/email")
@Tag(name = "Email Verification", description = "Email address verification and resend operations")
public class EmailVerificationRestResource {

  private final AuthenticationService authenticationService;

  public EmailVerificationRestResource(final AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/verify")
  @Operation(summary = "Verify email address",
      description = "Consumes a one-time 64-char hex token sent to the user's email and marks the address as verified.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Email verified"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired token")
  })
  public ResponseEntity<Void> verify(
      @Valid @RequestBody final AuthenticationDtos.VerifyEmailRequest request) {
    authenticationService.verifyEmail(request.token());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/resend-verification")
  @Operation(summary = "Resend email verification",
      description = "Generates a new verification token and sends it to the given email address. "
          + "Rate-limited to prevent abuse.")
  @ApiResponses({
      @ApiResponse(responseCode = "202", description = "Verification email queued"),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  })
  public ResponseEntity<Void> resend(
      @Valid @RequestBody final AuthenticationDtos.ResendVerificationRequest request) {
    authenticationService.resendVerification(request.email());
    return ResponseEntity.accepted().build();
  }
}
