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

package com.iqkv.foundation.iamservice.platformauthority;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing platform-level user authorities.
 */
public interface PlatformAuthorityService {

  /**
   * Replaces all platform authorities for a given user.
   *
   * @param userId      the user ID
   * @param authorities the list of authorities to grant
   * @param updatedBy   the actor performing the update
   */
  void updateUserAuthorities(UUID userId, List<String> authorities, String updatedBy);

  /**
   * Retrieves all platform authorities assigned to a given user.
   *
   * @param userId the user ID
   * @return list of authorities
   */
  List<String> getUserAuthorities(UUID userId);
}
