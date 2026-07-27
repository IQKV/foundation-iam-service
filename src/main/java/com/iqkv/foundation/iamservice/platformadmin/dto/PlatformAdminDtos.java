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

package com.iqkv.foundation.iamservice.platformadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the platform operator account API ({@code /auth/admin/me}).
 */
public final class PlatformAdminDtos {

  private PlatformAdminDtos() {
  }

  /**
   * Profile of the authenticated platform operator.
   *
   * <p>Intentionally excludes tenant membership fields — platform admins operate outside
   * any organization context.
   */
  public record AdminAccountResponse(
      UUID userId,
      String email,
      String firstName,
      String lastName,
      String status,
      boolean emailVerified,
      List<String> platformAuthorities,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  /**
   * Body for {@code PATCH /auth/admin/me} — name fields only.
   */
  public record AdminUpdateAccountRequest(
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName) {
  }

  /**
   * Body for {@code PATCH /auth/admin/me/password} — self-service password change.
   *
   * <p>Requires the caller to supply their current password for re-authentication
   * before the new password is accepted.
   */
  public record AdminChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 128)
      @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
          message = "{validation.password.pattern}")
      String newPassword) {
  }
}
