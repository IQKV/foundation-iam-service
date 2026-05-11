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

package com.iqkv.foundation.iamservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class UserDtos {

  private UserDtos() {
  }

  public record RegisterUserRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128)
      @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
          message = "{validation.password.pattern}")
      String password,
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName,
      @Size(max = 100) String tenantName) {
  }

  public record UpdateUserRequest(
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName) {
  }

  public record UserResponse(
      UUID id,
      String email,
      String firstName,
      String lastName,
      String status,
      boolean emailVerified,
      List<String> organizations,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  public record SignupResponse(
      UUID userId,
      String email,
      String tenantKey,
      String tenantStatus) {
  }

  public record AdminCreateUserRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName) {
  }

  public record AdminUpdateUserRequest(
      @Size(max = 100) String firstName,
      @Size(max = 100) String lastName,
      String status) {
  }

  public record PagedUserResponse(
      java.util.List<UserResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  /**
   * Query parameters for the admin user list endpoint.
   *
   * <p>Bound from HTTP query string via {@code @ModelAttribute} in the controller.
   * All filter/sort fields are optional — absent values fall back to safe defaults
   * in the service layer.
   *
   * @param page    zero-based page index (default 0)
   * @param size    page size 1–100 (default 20)
   * @param sortBy  sort field: email | firstName | lastName | updatedAt | createdAt
   * @param sortDir sort direction: asc | desc
   * @param search  free-text search on email, first name, last name (case-insensitive)
   * @param status  exact account status filter: ACTIVE | LOCKED | SUSPENDED | DELETED
   */
  public record UserListQuery(
      @Min(0) int page,
      @Min(1) @Max(100) int size,
      String sortBy,
      String sortDir,
      String search,
      String status) {

    /** Canonical defaults applied when the controller binds an empty query string. */
    public UserListQuery() {
      this(0, 20, "createdAt", "desc", null, null);
    }
  }
}
