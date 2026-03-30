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

package com.iqscaffold.iam.passwordreset;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqscaffold.iam.passwordreset.dto.PasswordResetDtos;

@RestController
@RequestMapping("/api/v1/iam/users/password")
public class PasswordResetRestResource {

  private final PasswordResetService passwordResetService;

  public PasswordResetRestResource(final PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/forgot")
  public ResponseEntity<Void> forgot(@Valid @RequestBody final PasswordResetDtos.ForgotPasswordRequest request) {
    passwordResetService.initiate(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/reset")
  public ResponseEntity<Void> reset(@Valid @RequestBody final PasswordResetDtos.ResetPasswordRequest request) {
    passwordResetService.complete(request.token(), request.newPassword());
    return ResponseEntity.noContent().build();
  }
}
