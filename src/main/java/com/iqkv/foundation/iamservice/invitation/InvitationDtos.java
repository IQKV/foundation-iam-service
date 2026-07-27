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

package com.iqkv.foundation.iamservice.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTOs for the invitation vertical slice.
 * All records are immutable; validation annotations live on request types only.
 */
public final class InvitationDtos {

  private InvitationDtos() {
  }

  // -------------------------------------------------------------------------
  // Requests
  // -------------------------------------------------------------------------

  /**
   * Body for {@code POST /tenants/{tenantKey}/invitations}.
   *
   * <p>{@code authority} is optional — defaults to {@code MEMBER} when omitted.
   * Only {@code ADMIN} or {@code MEMBER} are grantable via invitation; {@code TENANT_OWNER} is not.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SendInvitationRequest(
      @NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
      @Pattern(regexp = "ADMIN|MEMBER", message = "Authority must be ADMIN or MEMBER") String authority
  ) {
  }

  /**
   * Body for {@code POST /invitations/{token}/accept}.
   *
   * <p>For a new user: firstName, lastName, and password are required.
   * For an existing user: only password is required (firstName and lastName are ignored).
   * The service determines which path to take based on whether the email already has an account.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AcceptInvitationRequest(
      @Size(max = 100, message = "First name must not exceed 100 characters") String firstName,
      @Size(max = 100, message = "Last name must not exceed 100 characters") String lastName,
      @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password
  ) {
  }

  /**
   * Body for {@code POST /admin/invitations} — platform operator proposes an invitation for any tenant.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AdminProposeInvitationRequest(
      @NotBlank(message = "Tenant key is required") @Size(min = 8, max = 21, message = "Tenant key must be between 8 and 21 characters") String tenantKey,
      @NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
      @Pattern(regexp = "ADMIN|MEMBER", message = "Authority must be ADMIN or MEMBER") String authority
  ) {
  }

  // -------------------------------------------------------------------------
  // Responses
  // -------------------------------------------------------------------------

  /**
   * Returned after a successful {@code POST /tenants/{tenantKey}/invitations}.
   */
  public record InvitationResponse(
      UUID invitationId,
      String tenantKey,
      String email,
      String authority,
      String status,
      Instant expiresAt,
      Instant createdAt
  ) {
  }

  /**
   * Returned by {@code GET /invitations/{token}}.
   * The UI uses {@code requiresSignup} to decide which form to render.
   */
  public record InvitationPreviewResponse(
      UUID invitationId,
      String tenantName,
      String email,
      String authority,
      Instant expiresAt,
      boolean requiresSignup
  ) {
  }

  /**
   * Returned after a successful {@code POST /invitations/{token}/accept}.
   */
  public record AcceptInvitationResponse(
      String accessToken,
      String refreshToken,
      String tenantKey
  ) {
  }

  // -------------------------------------------------------------------------
  // Admin DTOs (used by InvitationAdminRestResource)
  // -------------------------------------------------------------------------

  /**
   * Full invitation view for platform operators — includes inviter and lifecycle timestamps.
   */
  public record AdminInvitationResponse(
      UUID invitationId,
      String tenantKey,
      String email,
      String authority,
      String status,
      UUID invitedBy,
      Instant expiresAt,
      Instant acceptedAt,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  public record InvitationCountResponse(long total) {
  }

  public record PagedInvitationAdminResponse(
      List<AdminInvitationResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  /**
   * Query parameters for the admin invitation list endpoint.
   *
   * @param page      zero-based page index (default 0)
   * @param size      page size 1–200 (default 20)
   * @param sortBy    sort field: email | tenantKey | status | expiresAt | createdAt | updatedAt
   * @param sortDir   sort direction: asc | desc
   * @param search    free-text search on invited email (case-insensitive)
   * @param status    exact status filter: PENDING | ACCEPTED | REVOKED | EXPIRED
   * @param tenantKey exact tenant key filter
   */
  public record InvitationListQuery(
      @Min(0) Integer page,
      @Min(1) @Max(200) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status,
      String tenantKey) {

    public InvitationListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                               final String search, final String status, final String tenantKey) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "createdAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
      this.tenantKey = tenantKey;
    }
  }
}
