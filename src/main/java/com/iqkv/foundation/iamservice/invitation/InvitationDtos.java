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

package com.iqkv.foundation.iamservice.invitation;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs for the invitation vertical slice.
 * All records are immutable; validation annotations live on request types only.
 */
public final class InvitationDtos {

  private InvitationDtos() {}

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
  ) {}

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
  ) {}

  // -------------------------------------------------------------------------
  // Responses
  // -------------------------------------------------------------------------

  /** Returned after a successful {@code POST /tenants/{tenantKey}/invitations}. */
  public record InvitationResponse(
      UUID invitationId,
      String tenantKey,
      String email,
      String authority,
      String status,
      Instant expiresAt,
      Instant createdAt
  ) {}

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
  ) {}

  /** Returned after a successful {@code POST /invitations/{token}/accept}. */
  public record AcceptInvitationResponse(
      String accessToken,
      String refreshToken,
      String tenantKey
  ) {}
}
