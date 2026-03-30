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

package com.iqscaffold.iam.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

  private UserDtos() {}

  public record RegisterUserRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128)
      @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
          message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
      String password,
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName,
      @NotBlank @Size(min = 1, max = 100) String tenantName) {}

  public record UpdateUserRequest(
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName) {}

  /**
   * @deprecated Use {@link UpdateUserRequest} instead.
   */
  @Deprecated
  public record UpdateProfileRequest(
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName) {}

  public record UserResponse(
      UUID id,
      String email,
      String firstName,
      String lastName,
      String status,
      boolean emailVerified,
      LocalDateTime createdAt) {}

  public record SignupResponse(
      UUID userId,
      String email,
      String tenantKey,
      String tenantStatus) {}
}
