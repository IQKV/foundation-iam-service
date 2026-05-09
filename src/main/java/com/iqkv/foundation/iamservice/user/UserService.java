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

package com.iqkv.foundation.iamservice.user;

import java.util.UUID;

import com.iqkv.foundation.iamservice.user.dto.UserDtos;

/**
 * Domain service for user lifecycle management.
 *
 * <p>Covers two distinct surfaces:
 * <ul>
 *   <li><b>Self-service</b> — registration and profile operations initiated by the user
 *       themselves (e.g. {@link #registerUser}).</li>
 *   <li><b>Platform admin</b> — privileged CRUD operations performed by operators with
 *       {@code PLATFORM_ADMIN} authority (e.g. {@link #createUser}, {@link #patchUser},
 *       {@link #deleteUserById}).</li>
 * </ul>
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Persisting changes via {@code UserMapper}.</li>
 *   <li>Publishing domain events (user created, user removed) via {@code UserEventPublisher}.</li>
 *   <li>Triggering side-effects such as email verification token generation and
 *       notification dispatch on registration.</li>
 * </ul>
 */
public interface UserService {

  /**
   * Registers a new user through the self-service signup flow.
   *
   * <p>Delegates mode-specific logic (user upsert, tenant resolution or creation,
   * membership creation, authority grant) to the active {@code SignupStrategy}.
   * After the strategy completes, this method:
   * <ol>
   *   <li>Publishes a {@code user.created} domain event.</li>
   *   <li>Generates a 32-byte hex email verification token (TTL: 24 h) and persists it.</li>
   *   <li>Dispatches a {@code VERIFY_EMAIL} notification via the messaging service.</li>
   * </ol>
   *
   * @param request validated registration payload containing email, password, name, and
   *                optional tenant name
   * @return a {@link UserDtos.SignupResponse} with the new user ID, email, tenant key,
   *         and initial tenant status
   */
  UserDtos.SignupResponse registerUser(UserDtos.RegisterUserRequest request);

  /**
   * Retrieves a single user by their UUID.
   *
   * @param userId the UUID of the user to fetch
   * @return the user's public profile as a {@link UserDtos.UserResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException
   *         if no user exists with the given ID
   */
  UserDtos.UserResponse getUserById(UUID userId);

  /**
   * Returns a paginated, sorted list of all users across all tenants.
   *
   * <p>Intended for platform admin use only. The {@code sortBy} and {@code sortDir}
   * values are validated against an allowlist in the implementation before being
   * forwarded to the SQL layer — invalid values silently fall back to
   * {@code createdAt DESC} rather than throwing.
   *
   * <p>Valid {@code sortBy} values: {@code email}, {@code firstName}, {@code lastName},
   * {@code updatedAt}, {@code createdAt}.
   *
   * @param page    zero-based page index
   * @param size    number of records per page (1–100)
   * @param sortBy  field name to sort by; falls back to {@code createdAt} if unrecognised
   * @param sortDir {@code "asc"} or {@code "desc"} (case-insensitive); defaults to
   *                {@code "desc"} for any other value
   * @return a {@link UserDtos.PagedUserResponse} containing the content slice and
   *         pagination metadata
   */
  UserDtos.PagedUserResponse listUsers(int page, int size, String sortBy, String sortDir);

  /**
   * Creates a new user account via the platform admin flow.
   *
   * <p>A random UUID is used as the temporary password hash so the account is
   * immediately usable but the plaintext password is never known. The user must
   * reset their password on first login. {@code emailVerified} is set to
   * {@code false}; no verification email is sent by this method.
   *
   * @param request admin create payload containing email, first name, and last name
   * @return the newly created user as a {@link UserDtos.UserResponse}
   */
  UserDtos.UserResponse createUser(UserDtos.AdminCreateUserRequest request);

  /**
   * Fully replaces a user's name fields (PUT semantics).
   *
   * <p>Both {@code firstName} and {@code lastName} are required. Only those two
   * fields are written; email, status, and credentials are not affected.
   * {@code updatedAt} is set to {@code now()} and {@code updatedBy} is recorded
   * as the provided actor identifier.
   *
   * @param userId     UUID of the user to update
   * @param firstName  new first name (must not be blank)
   * @param lastName   new last name (must not be blank)
   * @param updatedBy  identifier of the actor performing the update (user ID or
   *                   {@code "system"} for admin operations)
   * @return the updated user as a {@link UserDtos.UserResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException
   *         if no user exists with the given ID
   */
  UserDtos.UserResponse updateUser(UUID userId, String firstName, String lastName, String updatedBy);

  /**
   * Partially updates a user's profile (PATCH semantics).
   *
   * <p>Only non-null fields in the request are applied; omitted fields are left
   * unchanged. Supports updating {@code firstName}, {@code lastName}, and
   * {@code status}. Email and credentials cannot be changed through this method.
   *
   * @param userId  UUID of the user to patch
   * @param request partial update payload; any field may be {@code null} to indicate
   *                no change
   * @return the updated user as a {@link UserDtos.UserResponse}
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException
   *         if no user exists with the given ID
   * @throws IllegalArgumentException if {@code status} is non-null but not a valid
   *         {@link AccountStatus} name
   */
  UserDtos.UserResponse patchUser(UUID userId, UserDtos.AdminUpdateUserRequest request);

  /**
   * Removes a user's membership from a specific tenant.
   *
   * <p>Deletes only the {@code TenantMembership} record for the given
   * {@code (userId, tenantKey)} pair. The user account itself is preserved.
   * A {@code user.removed} domain event is published after the membership is deleted.
   *
   * <p>Use {@link #deleteUserById} to permanently delete the user account and all
   * associated memberships across all tenants.
   *
   * @param userId    UUID of the user whose membership is being removed
   * @param tenantKey the 8-character NanoID identifying the tenant
   * @throws com.iqkv.foundation.iamservice.shared.exception.MembershipNotFoundException
   *         if the user is not a member of the specified tenant
   */
  void deleteUser(UUID userId, String tenantKey);

  /**
   * Permanently deletes a user account and all associated data (cascade).
   *
   * <p>Removes the {@code users} row directly. Cascading deletes defined at the
   * database level handle removal of memberships, authorities, tokens, and other
   * dependent records. This operation is irreversible.
   *
   * <p>Use {@link #deleteUser(UUID, String)} to remove only a tenant membership
   * while keeping the account intact.
   *
   * @param userId UUID of the user to delete
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException
   *         if no user exists with the given ID
   */
  void deleteUserById(UUID userId);
}
