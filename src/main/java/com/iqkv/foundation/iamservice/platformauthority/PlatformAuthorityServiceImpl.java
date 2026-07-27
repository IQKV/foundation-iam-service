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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAuthorityServiceImpl implements PlatformAuthorityService {

  private final PlatformAuthorityMapper platformAuthorityMapper;
  private final UserService userService;

  public PlatformAuthorityServiceImpl(final PlatformAuthorityMapper platformAuthorityMapper, final UserService userService) {
    this.platformAuthorityMapper = platformAuthorityMapper;
    this.userService = userService;
  }

  @Override
  @Transactional
  public void updateUserAuthorities(UUID userId, List<String> authorities, String updatedBy) {
    // Verify user exists; throws UserNotFoundException if not
    userService.getUserById(userId);

    // Get current authorities
    List<String> currentAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);

    // Determine authorities to remove
    for (final String currentAuth : currentAuthorities) {
      if (!authorities.contains(currentAuth)) {
        platformAuthorityMapper.deleteByUserIdAndRole(userId, currentAuth);
      }
    }

    // Determine authorities to add
    for (final String newAuth : authorities) {
      if (!currentAuthorities.contains(newAuth)) {
        PlatformAuthority authority = new PlatformAuthority();
        authority.setId(UUID.randomUUID());
        authority.setUserId(userId);
        authority.setRole(newAuth); // using setRole since the mapper and entity expect that
        authority.setGrantedAt(Instant.now());
        authority.setGrantedBy(updatedBy);
        platformAuthorityMapper.insert(authority);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getUserAuthorities(UUID userId) {
    // Verify user exists; throws UserNotFoundException if not
    userService.getUserById(userId);
    return platformAuthorityMapper.findAuthorityValuesByUserId(userId);
  }
}
