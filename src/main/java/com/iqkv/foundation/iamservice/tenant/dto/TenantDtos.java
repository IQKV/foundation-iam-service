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

package com.iqkv.foundation.iamservice.tenant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.tenant.TenantStatus;

public final class TenantDtos {

  private TenantDtos() {
  }

  // ─── Self-service DTOs (used by TenantRestResource) ───────────────────────

  public record CreateTenantRequest(
      @NotBlank @Size(max = 100) String name
  ) {
  }

  public record UpdateTenantStatusRequest(@NotNull TenantStatus status) {
  }

  /**
   * Slim response used by the self-service tenant endpoint.
   */
  public record TenantResponse(
      String tenantKey,
      String name,
      TenantStatus status,
      LocalDateTime createdAt
  ) {
  }

  // ─── Admin DTOs (used by TenantAdminRestResource) ─────────────────────────

  /**
   * Request body for full tenant rename (PUT semantics).
   * Only {@code name} is replaced; status and other fields are unchanged.
   */
  public record UpdateTenantRequest(
      @NotBlank @Size(max = 100) String name) {
  }

  /**
   * Request body for partial tenant update (PATCH semantics).
   * Any field may be {@code null} to indicate no change.
   */
  public record AdminUpdateTenantRequest(
      @Size(max = 100) String name,
      TenantStatus status) {
  }

  public record AdminUpdateMemberAuthoritiesRequest(
      List<String> authorities) {
  }

  public record MemberAuthoritiesResponse(
      UUID userId,
      String tenantKey,
      List<String> authorities) {
  }

  /**
   * Single member row returned by the tenant-scoped member list.
   *
   * <p>{@code tenantAuthorities} contains only the authorities this user holds
   * within the specific tenant being queried — not across all tenants.
   * {@code organizations} lists all tenant names the user belongs to (cross-tenant context).
   */
  public record TenantMemberResponse(
      UUID id,
      String email,
      String firstName,
      String lastName,
      String status,
      boolean emailVerified,
      List<String> tenantAuthorities,
      List<String> organizations,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  /**
   * Paginated list of tenant members returned by the admin tenant member list endpoint.
   */
  public record PagedTenantMemberResponse(
      List<TenantMemberResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  /**
   * Rich tenant response returned by admin endpoints.
   * Includes all fields from the {@code Tenant} entity.
   */
  public record AdminTenantResponse(
      UUID id,
      String tenantKey,
      String name,
      TenantStatus status,
      Boolean isDefault,
      String tenantModeOrigin,
      String createdBy,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  /**
   * Total tenant count returned by the admin count endpoint.
   */
  public record TenantCountResponse(long total) {
  }

  /**
   * Member count for a specific tenant.
   */
  public record TenantMemberCountResponse(String tenantKey, long total) {
  }

  /**
   * Query parameters for the tenant member list endpoint.
   *
   * @param page    zero-based page index (default 0)
   * @param size    page size 1–100 (default 20)
   * @param sortBy  sort field: email | firstName | lastName | updatedAt | createdAt
   * @param sortDir sort direction: asc | desc
   * @param search  free-text search on email, first name, last name (case-insensitive)
   * @param status  exact account status filter: ACTIVE | LOCKED | SUSPENDED | DELETED
   */
  public record TenantMemberListQuery(
      @Min(0) Integer page,
      @Min(1) @Max(100) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status) {

    public TenantMemberListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                                 final String search, final String status) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "createdAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
    }
  }

  /**
   * Paginated list of tenants returned by the admin list endpoint.
   */
  public record PagedTenantResponse(
      List<AdminTenantResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  /**
   * Query parameters for the admin tenant list endpoint.
   *
   * <p>Bound from HTTP query string via {@code @ModelAttribute} in the controller.
   * All filter/sort fields are optional — absent values fall back to safe defaults
   * in the service layer.
   *
   * @param page    zero-based page index (default 0)
   * @param size    page size 1–100 (default 20)
   * @param sortBy  sort field: name | tenantKey | createdAt | updatedAt
   * @param sortDir sort direction: asc | desc
   * @param search  free-text search on name and tenantKey (case-insensitive)
   * @param status  exact status filter: PROVISIONING | ACTIVE | SUSPENDED | DELETED | PROVISIONING_FAILED
   */
  public record TenantListQuery(
      @Min(0) Integer page,
      @Min(1) @Max(100) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status) {

    public TenantListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                           final String search, final String status) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "createdAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
    }
  }
}
