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

import java.util.Collections;

import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserWithOrganizations;

public final class UserDtoMapper {

  private UserDtoMapper() {
  }

  /**
   * Maps a plain {@link User} entity to a {@link UserDtos.UserResponse}.
   * Used for single-user lookups (GET by ID, create, update) where
   * organization membership is not fetched.
   */
  public static UserDtos.UserResponse toResponse(final User user) {
    return new UserDtos.UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getStatus() != null ? user.getStatus().name() : null,
        user.isEmailVerified(),
        user.getLocale(),
        Collections.emptyList(),
        Collections.emptyList(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  /**
   * Maps a {@link UserWithOrganizations} projection to a {@link UserDtos.UserResponse}.
   * Used by the admin list and detail queries where tenant names and membership
   * authorities are aggregated in SQL.
   */
  public static UserDtos.UserResponse toResponse(final UserWithOrganizations projection) {
    return new UserDtos.UserResponse(
        projection.getId(),
        projection.getEmail(),
        projection.getFirstName(),
        projection.getLastName(),
        projection.getStatus() != null ? projection.getStatus().name() : null,
        projection.isEmailVerified(),
        projection.getLocale(),
        projection.getOrganizations() != null ? projection.getOrganizations() : Collections.emptyList(),
        projection.getMembershipAuthorities() != null ? projection.getMembershipAuthorities() : Collections.emptyList(),
        projection.getCreatedAt(),
        projection.getUpdatedAt());
  }

  public static UserDtos.SignupResponse toSignupResponse(final User user, final Tenant tenant) {
    return new UserDtos.SignupResponse(
        user.getId(),
        user.getEmail(),
        tenant.getTenantKey(),
        tenant.getStatus() != null ? tenant.getStatus().name() : null);
  }
}
