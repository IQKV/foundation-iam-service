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
      @NotBlank @Size(max = 100) String lastName,
      // BCP 47 locale tag (e.g. "en-US"). Optional — omit to leave unchanged.
      @Size(max = 20) String locale) {
  }

  public record UserResponse(
      UUID id,
      String email,
      String firstName,
      String lastName,
      String status,
      boolean emailVerified,
      // BCP 47 locale tag (e.g. "en-US"). Null when not yet set.
      String locale,
      // Public URL of the user's avatar image. Null when no avatar has been uploaded.
      String avatarUrl,
      // First sign-in timestamp. Null if the user hasn't signed in yet.
      java.time.Instant firstSignInAt,
      // Whether the user has completed the onboarding process.
      boolean onboardingCompleted,
      List<String> organizations,
      List<String> membershipAuthorities,
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
      @NotBlank @Size(max = 100) String lastName,
      // BCP 47 locale tag (e.g. "en-US"). Optional — defaults to platform default when absent.
      @Size(max = 20) String locale) {
  }

  public record AdminUpdateUserRequest(
      @Size(max = 100) String firstName,
      @Size(max = 100) String lastName,
      String status,
      // BCP 47 locale tag (e.g. "en-US"). Optional — omit to leave unchanged.
      @Size(max = 20) String locale) {
  }

  public record AdminUpdateUserAuthoritiesRequest(
      List<String> authorities) {
  }

  /**
   * Body for {@code POST /users/me/password} — self-service password change.
   *
   * <p>Requires the caller to supply their current password for re-authentication
   * before the new password is accepted.
   */
  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 128)
      @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
          message = "{validation.password.pattern}")
      String newPassword) {
  }

  /**
   * Body for {@code PATCH /admin/users/{id}/password} — admin-forced password change.
   *
   * <p>No current password is required; the platform admin sets the new password directly.
   * All existing sessions for the target user are invalidated after the change.
   */
  public record AdminSetUserPasswordRequest(
      @NotBlank @Size(min = 8, max = 128)
      @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
          message = "{validation.password.pattern}")
      String newPassword) {
  }

  public record UserAuthoritiesResponse(
      UUID userId,
      List<String> authorities) {
  }

  public record UserMembershipResponse(
      String tenantKey,
      String tenantName,
      String status,
      List<String> authorities,
      boolean isPersonal) {
  }

  public record UserCountResponse(long total) {
  }

  /**
   * Returned by {@code POST /users/me/avatar}.
   *
   * <p>The client must PUT the file directly to {@code presignedUploadUrl} within
   * {@code expiresInMinutes} minutes, then call {@code POST /users/me/avatar/confirm}
   * with the resulting {@code objectKey} to persist the avatar URL on the profile.
   */
  public record AvatarUploadInitResponse(
      // Pre-signed S3/MinIO PUT URL — valid for expiresInMinutes minutes.
      String presignedUploadUrl,
      // Object key to pass back in the confirm call.
      String objectKey,
      int expiresInMinutes) {
  }

  /**
   * Body for {@code POST /users/me/avatar/confirm}.
   *
   * <p>After the client has successfully PUT the file to the pre-signed URL,
   * it calls this endpoint with the object key to persist the avatar URL.
   */
  public record AvatarConfirmRequest(
      @NotBlank String objectKey) {
  }

  /**
   * Returned after a successful avatar confirm — contains the public avatar URL.
   */
  public record AvatarResponse(String avatarUrl) {
  }

  public record PagedUserResponse(
      java.util.List<UserResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  /**
   * A single data point in a time-series signup series.
   *
   * @param period  ISO-8601 date string representing the start of the bucket
   *                (e.g. {@code "2026-06-01"} for a day bucket, {@code "2026-06"} for a month bucket)
   * @param signups number of new memberships created within this bucket
   */
  public record UserSignupSeriesPoint(String period, long signups) {
  }

  /**
   * Aggregated user statistics for a tenant, suitable for the owner/admin dashboard.
   *
   * <p>The {@code signupSeries} list contains one entry per calendar bucket
   * (day or month depending on the requested {@code granularity}) within the
   * queried range. Buckets with zero signups are included so the chart renders
   * a continuous axis.
   *
   * @param tenantKey          the 8-character NanoID identifying the tenant
   * @param totalMembers       total number of memberships ever created (all statuses)
   * @param activeMembers      members whose account status is {@code ACTIVE}
   * @param lockedMembers      members whose account status is {@code LOCKED}
   * @param suspendedMembers   members whose account status is {@code SUSPENDED}
   * @param emailVerifiedCount members with a verified email address
   * @param signupSeries       time-bucketed new-signup counts over the requested period
   * @param periodFrom         inclusive start of the queried period (ISO-8601 date)
   * @param periodTo           inclusive end of the queried period (ISO-8601 date)
   * @param granularity        time bucket size used: {@code "day"} or {@code "month"}
   */
  public record TenantUserStatsResponse(
      String tenantKey,
      long totalMembers,
      long activeMembers,
      long lockedMembers,
      long suspendedMembers,
      long emailVerifiedCount,
      java.util.List<UserSignupSeriesPoint> signupSeries,
      String periodFrom,
      String periodTo,
      String granularity) {
  }

  /**
   * Query parameters for the tenant user-stats endpoint.
   *
   * @param from        inclusive start date, ISO-8601 ({@code yyyy-MM-dd}); defaults to 30 days ago
   * @param to          inclusive end date, ISO-8601 ({@code yyyy-MM-dd}); defaults to today
   * @param granularity time bucket: {@code "day"} (default) or {@code "month"}
   */
  public record TenantUserStatsQuery(
      String from,
      String to,
      String granularity) {
  }

  /**
   * Query parameters for the admin user list endpoint.
   *
   * <p>Bound from HTTP query string via {@code @ModelAttribute} in the controller.
   * All filter/sort fields are optional — absent values fall back to safe defaults
   * in the service layer.
   *
   * @param page                  zero-based page index (default 0)
   * @param size                  page size 1–100 (default 20)
   * @param sortBy                sort field: email | firstName | lastName | updatedAt | createdAt
   * @param sortDir               sort direction: asc | desc
   * @param search                free-text search on email, first name, last name (case-insensitive)
   * @param status                exact account status filter: ACTIVE | LOCKED | SUSPENDED | DELETED
   * @param excludePlatformAdmins if true, omits users who hold the PLATFORM_ADMIN authority
   */
  public record UserListQuery(
      @Min(0) Integer page,
      @Min(1) @Max(100) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status,
      Boolean excludePlatformAdmins) {

    public UserListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                         final String search, final String status, final Boolean excludePlatformAdmins) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "createdAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
      this.excludePlatformAdmins = excludePlatformAdmins != null ? excludePlatformAdmins : false;
    }
  }
}
