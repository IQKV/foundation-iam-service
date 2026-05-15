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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformadmin.dto.PlatformAdminDtos;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper;
import com.iqkv.foundation.iamservice.shared.exception.NoPlatformAuthorityException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformAdminAccountServiceImpl implements PlatformAdminAccountService {

  private final UserMapper userMapper;
  private final PlatformAuthorityMapper platformAuthorityMapper;

  public PlatformAdminAccountServiceImpl(
      final UserMapper userMapper,
      final PlatformAuthorityMapper platformAuthorityMapper) {
    this.userMapper = userMapper;
    this.platformAuthorityMapper = platformAuthorityMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PlatformAdminDtos.AdminAccountResponse getAccount(final UUID userId) {
    final User user = requirePlatformOperator(userId);
    final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);
    return PlatformAdminDtoMapper.toAccountResponse(user, platformAuthorities);
  }

  @Override
  public PlatformAdminDtos.AdminAccountResponse updateAccount(
      final UUID userId,
      final PlatformAdminDtos.AdminUpdateAccountRequest request) {
    final User user = requirePlatformOperator(userId);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setUpdatedAt(LocalDateTime.now());
    user.setUpdatedBy(userId.toString());
    userMapper.update(user);
    final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);
    return PlatformAdminDtoMapper.toAccountResponse(user, platformAuthorities);
  }

  private User requirePlatformOperator(final UUID userId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    final List<String> platformAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);
    if (platformAuthorities.isEmpty()) {
      throw new NoPlatformAuthorityException();
    }
    return user;
  }
}
