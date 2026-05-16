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

package com.iqkv.foundation.iamservice.platformadmin;

import java.util.UUID;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;

/**
 * Platform operator self-service account operations (no tenant context).
 */
public interface PlatformAdminAccountService {

  PlatformAdminDtos.AdminAccountResponse getAccount(UUID userId);

  PlatformAdminDtos.AdminAccountResponse updateAccount(
      UUID userId,
      PlatformAdminDtos.AdminUpdateAccountRequest request);

  /**
   * Changes the authenticated platform operator's own password.
   *
   * <p>Verifies {@code currentPassword} against the stored hash before accepting
   * {@code newPassword}. On success, all existing sessions for the user are
   * invalidated by updating {@code last_global_signout_at}.
   *
   * @param userId          UUID of the authenticated platform operator
   * @param currentPassword the operator's current plaintext password (for re-authentication)
   * @param newPassword     the desired new password (must satisfy the platform password policy)
   * @throws org.springframework.security.authentication.BadCredentialsException if {@code currentPassword} is wrong
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException if the user does not exist
   * @throws com.iqkv.foundation.iamservice.shared.exception.NoPlatformAuthorityException if the user is not a platform operator
   */
  void changePassword(UUID userId, String currentPassword, String newPassword);
}
