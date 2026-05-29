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

package com.iqkv.foundation.iamservice.ban;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.ban.dto.BanDtos;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.platformauthority.PlatformAuthorityMapper;
import com.iqkv.foundation.iamservice.shared.exception.UserManagementException;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import com.iqkv.foundation.iamservice.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BanServiceImpl implements BanService {

  private static final Logger log = LoggerFactory.getLogger(BanServiceImpl.class);
  private static final String PLATFORM_ADMIN_AUTHORITY = "PLATFORM_ADMIN";

  private final BanMapper banMapper;
  private final UserMapper userMapper;
  private final PlatformAuthorityMapper platformAuthorityMapper;
  private final MessagingService messagingService;
  private final NotificationConfigurationProperties notificationProps;

  public BanServiceImpl(final BanMapper banMapper,
                       final UserMapper userMapper,
                       final PlatformAuthorityMapper platformAuthorityMapper,
                       final MessagingService messagingService,
                       final NotificationConfigurationProperties notificationProps) {
    this.banMapper = banMapper;
    this.userMapper = userMapper;
    this.platformAuthorityMapper = platformAuthorityMapper;
    this.messagingService = messagingService;
    this.notificationProps = notificationProps;
  }

  @Override
  public BanDtos.BanResponse banUserPlatform(UUID userId, UUID initiatorId, BanDtos.CreateBanRequest request) {
    User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    // Prevent self-ban
    if (userId.equals(initiatorId)) {
      throw new UserManagementException("Cannot ban yourself");
    }

    // Prevent banning other platform admins
    List<String> targetUserAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);
    if (targetUserAuthorities.contains(PLATFORM_ADMIN_AUTHORITY)) {
      throw new UserManagementException("Cannot ban another platform admin");
    }

    // Remove any existing platform ban
    banMapper.deleteByUserIdAndTypeAndTenantKey(userId, BanType.PLATFORM, null);

    Ban ban = new Ban();
    ban.setId(UUID.randomUUID());
    ban.setUserId(userId);
    ban.setInitiatorId(initiatorId);
    ban.setType(BanType.PLATFORM);
    ban.setTenantKey(null);
    ban.setReason(request.reason());
    ban.setExpiresAt(request.expiresAt());
    ban.setCreatedAt(LocalDateTime.now());
    ban.setUpdatedAt(LocalDateTime.now());
    ban.setCreatedBy(initiatorId.toString());
    ban.setUpdatedBy(initiatorId.toString());

    banMapper.insert(ban);

    // Invalidate all user sessions
    userMapper.updateLastGlobalSignoutAt(userId, Instant.now());

    // Send notification (only USER_BANNED)
    sendBanNotification(user, ban);

    log.info("User {} banned globally by {}", userId, initiatorId);
    return toResponse(ban);
  }

  @Override
  public void unbanUserPlatform(UUID userId, UUID initiatorId) {
    User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    banMapper.deleteByUserIdAndTypeAndTenantKey(userId, BanType.PLATFORM, null);
    log.info("User {} unbanned globally by {}", userId, initiatorId);
    // Unban notification reserved but not implemented
  }

  @Override
  public BanDtos.BanResponse banUserTenant(UUID userId, String tenantKey, UUID initiatorId, BanDtos.CreateBanRequest request) {
    User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    // Prevent self-ban
    if (userId.equals(initiatorId)) {
      throw new UserManagementException("Cannot ban yourself");
    }

    // Check if target user is a platform admin (cannot ban platform admins even at tenant level)
    List<String> targetUserAuthorities = platformAuthorityMapper.findAuthorityValuesByUserId(userId);
    if (targetUserAuthorities.contains(PLATFORM_ADMIN_AUTHORITY)) {
      throw new UserManagementException("Cannot ban a platform admin");
    }

    // Remove any existing tenant ban
    banMapper.deleteByUserIdAndTypeAndTenantKey(userId, BanType.TENANT, tenantKey);

    Ban ban = new Ban();
    ban.setId(UUID.randomUUID());
    ban.setUserId(userId);
    ban.setInitiatorId(initiatorId);
    ban.setType(BanType.TENANT);
    ban.setTenantKey(tenantKey);
    ban.setReason(request.reason());
    ban.setExpiresAt(request.expiresAt());
    ban.setCreatedAt(LocalDateTime.now());
    ban.setUpdatedAt(LocalDateTime.now());
    ban.setCreatedBy(initiatorId.toString());
    ban.setUpdatedBy(initiatorId.toString());

    banMapper.insert(ban);

    // Invalidate all user sessions (for simplicity, since they're banned from this tenant)
    userMapper.updateLastGlobalSignoutAt(userId, Instant.now());

    // Send notification (only USER_BANNED)
    sendBanNotification(user, ban);

    log.info("User {} banned from tenant {} by {}", userId, tenantKey, initiatorId);
    return toResponse(ban);
  }

  @Override
  public void unbanUserTenant(UUID userId, String tenantKey, UUID initiatorId) {
    User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    banMapper.deleteByUserIdAndTypeAndTenantKey(userId, BanType.TENANT, tenantKey);
    log.info("User {} unbanned from tenant {} by {}", userId, tenantKey, initiatorId);
    // Unban notification reserved but not implemented
  }

  @Override
  public boolean isUserBanned(UUID userId, String tenantKey) {
    Optional<Ban> activeBan = banMapper.findActiveBan(userId, tenantKey);
    return activeBan.isPresent();
  }

  private BanDtos.BanResponse toResponse(Ban ban) {
    return new BanDtos.BanResponse(
        ban.getId(),
        ban.getUserId(),
        ban.getInitiatorId(),
        ban.getType().name(),
        ban.getTenantKey(),
        ban.getReason(),
        ban.getExpiresAt(),
        ban.getCreatedAt(),
        ban.getUpdatedAt());
  }

  private void sendBanNotification(User user, Ban ban) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
      payload.put("banType", ban.getType().name());
      if (ban.getTenantKey() != null) {
        payload.put("tenantKey", ban.getTenantKey());
      }
      if (ban.getReason() != null) {
        payload.put("reason", ban.getReason());
      }
      if (ban.getExpiresAt() != null) {
        payload.put("expiresAt", ban.getExpiresAt());
      }

      NotificationEvent event = new NotificationEvent(
          user.getId(),
          user.getEmail(),
          user.getLocale() != null ? user.getLocale() : (notificationProps.defaultLocale() != null ? notificationProps.defaultLocale() : "en"),
          NotificationEventType.USER_BANNED,
          payload,
          Instant.now());
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish USER_BANNED notification for user {}", user.getId(), e);
    }
  }
}
