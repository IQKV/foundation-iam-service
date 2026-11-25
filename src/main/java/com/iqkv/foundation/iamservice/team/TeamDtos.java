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

package com.iqkv.foundation.iamservice.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public final class TeamDtos {

  private TeamDtos() {
  }

  // --- Requests ---

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateTeamRequest(
      @NotBlank(message = "Name is required")
      @Size(max = 100, message = "Name must not exceed 100 characters")
      String name,
      @Size(max = 50, message = "Slug must not exceed 50 characters")
      @Pattern(regexp = "^[a-z0-9-]*$", message = "Slug must contain only lowercase letters, digits, and hyphens")
      String slug
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateTeamRequest(
      @Size(max = 100, message = "Name must not exceed 100 characters")
      String name,
      @Pattern(regexp = "ACTIVE|ARCHIVED", message = "Status must be ACTIVE or ARCHIVED")
      String status
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SendTeamInvitationRequest(
      @NotBlank(message = "Email is required")
      @Email(message = "Must be a valid email address")
      @Size(max = 255, message = "Email must not exceed 255 characters")
      String email,
      @Pattern(regexp = "ADMIN|MEMBER", message = "Role must be ADMIN or MEMBER")
      String role
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UpdateMemberRoleRequest(
      @NotBlank(message = "Role is required")
      @Pattern(regexp = "OWNER|ADMIN|MEMBER", message = "Role must be OWNER, ADMIN, or MEMBER")
      String role
  ) {
  }

  // --- Responses ---

  public record TeamResponse(
      UUID id,
      String name,
      String slug,
      String status,
      UUID createdByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
  }

  public record TeamMemberResponse(
      UUID id,
      UUID teamId,
      UUID userId,
      String userEmail,
      String userFirstName,
      String userLastName,
      String role,
      String status,
      LocalDateTime createdAt
  ) {
  }

  public record TeamWithMembersResponse(
      UUID id,
      String name,
      String slug,
      String status,
      UUID createdByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      List<TeamMemberResponse> members
  ) {
  }

  public record TeamInvitationResponse(
      UUID invitationId,
      UUID teamId,
      String teamName,
      String email,
      String role,
      String status,
      Instant expiresAt,
      Instant createdAt
  ) {
  }

  public record TeamInvitationPreviewResponse(
      UUID invitationId,
      String teamName,
      String email,
      String role,
      Instant expiresAt,
      boolean requiresSignup
  ) {
  }

  public record AcceptTeamInvitationResponse(
      UUID teamId,
      String teamName
  ) {
  }
}
