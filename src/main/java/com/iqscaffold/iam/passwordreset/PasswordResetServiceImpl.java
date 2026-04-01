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

package com.iqscaffold.iam.passwordreset;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iqscaffold.iam.infrastructure.config.AuthConfigurationProperties;
import com.iqscaffold.iam.infrastructure.config.NotificationConfigurationProperties;
import com.iqscaffold.iam.infrastructure.messaging.MessagingService;
import com.iqscaffold.iam.infrastructure.messaging.NotificationEvent;
import com.iqscaffold.iam.infrastructure.messaging.NotificationEventType;
import com.iqscaffold.iam.shared.exception.PasswordResetRateLimitException;
import com.iqscaffold.iam.shared.exception.PasswordResetTokenNotFoundException;
import com.iqscaffold.iam.user.UserMapper;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Pattern PASSWORD_PATTERN =
      Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$");

  private final UserMapper userMapper;
  private final PasswordResetTokenMapper passwordResetTokenMapper;
  private final PasswordEncoder passwordEncoder;
  private final MessagingService messagingService;
  private final AuthConfigurationProperties authProps;
  private final NotificationConfigurationProperties notificationProps;

  public PasswordResetServiceImpl(
      final UserMapper userMapper,
      final PasswordResetTokenMapper passwordResetTokenMapper,
      final PasswordEncoder passwordEncoder,
      final MessagingService messagingService,
      final AuthConfigurationProperties authProps,
      final NotificationConfigurationProperties notificationProps) {
    this.userMapper = userMapper;
    this.passwordResetTokenMapper = passwordResetTokenMapper;
    this.passwordEncoder = passwordEncoder;
    this.messagingService = messagingService;
    this.authProps = authProps;
    this.notificationProps = notificationProps;
  }

  @Override
  public void initiate(final String email) {
    final var userOpt = userMapper.findByEmail(email);
    if (userOpt.isEmpty()) {
      return; // prevent enumeration
    }

    final var user = userOpt.get();
    final Instant windowStart = Instant.now().minus(authProps.passwordReset().rateLimitWindow());
    final int count = passwordResetTokenMapper.countByUserIdAndCreatedAtAfter(user.getId(), windowStart);
    if (count >= authProps.passwordReset().rateLimitMaxRequests()) {
      throw new PasswordResetRateLimitException(authProps.passwordReset().rateLimitWindow().toSeconds());
    }

    passwordResetTokenMapper.deleteByUserId(user.getId());

    final byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    final String tokenValue = HexFormat.of().formatHex(bytes);

    final var prt = new PasswordResetToken();
    prt.setId(UUID.randomUUID());
    prt.setUserId(user.getId());
    prt.setToken(tokenValue);
    prt.setExpiresAt(Instant.now().plus(authProps.passwordReset().tokenTtl()));
    prt.setCreatedAt(Instant.now());
    passwordResetTokenMapper.insert(prt);

    final String resetUrl = notificationProps.baseUrl() + "/reset-password?token=" + tokenValue;
    final var event = new NotificationEvent(
        email,
        notificationProps.defaultLocale(),
        NotificationEventType.PASSWORD_RESET_INITIATE,
        Map.of("resetUrl", resetUrl, "firstName", user.getFirstName(), "token", tokenValue),
        Instant.now());
    try {
      messagingService.publishNotification(event);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_RESET_INITIATE notification for user {}", user.getId(), e);
    }
  }

  @Override
  public void complete(final String token, final String newPassword) {
    final var prt = passwordResetTokenMapper.findByToken(token)
        .orElseThrow(PasswordResetTokenNotFoundException::new);

    if (prt.getExpiresAt().isBefore(Instant.now())) {
      throw new PasswordResetTokenNotFoundException();
    }

    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128
        || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
      throw new IllegalArgumentException(
          "Password must be 8-128 characters and contain uppercase, lowercase, digit, and special character");
    }

    final String hash = passwordEncoder.encode(newPassword);
    userMapper.updatePassword(prt.getUserId(), hash, Instant.now());
    passwordResetTokenMapper.deleteByToken(token);
    userMapper.updateLastGlobalSignoutAt(prt.getUserId(), Instant.now());

    final var user = userMapper.findById(prt.getUserId()).orElse(null);
    if (user != null) {
      final var event = new NotificationEvent(
          user.getEmail(),
          notificationProps.defaultLocale(),
          NotificationEventType.PASSWORD_RESET_CONFIRMED,
          Map.of("firstName", user.getFirstName()),
          Instant.now());
      try {
        messagingService.publishNotification(event);
      } catch (final Exception e) {
        log.warn("Failed to publish PASSWORD_RESET_CONFIRMED notification for user {}", user.getId(), e);
      }
    }
  }
}
