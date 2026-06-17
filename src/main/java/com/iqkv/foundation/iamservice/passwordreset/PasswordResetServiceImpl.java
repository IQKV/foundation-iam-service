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

package com.iqkv.foundation.iamservice.passwordreset;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.iqkv.foundation.iamservice.infrastructure.config.AuthConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.config.NotificationConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.NotificationEventType;
import com.iqkv.foundation.iamservice.infrastructure.messaging.PasswordEvent;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.shared.exception.InvalidPasswordException;
import com.iqkv.foundation.iamservice.shared.exception.PasswordResetRateLimitException;
import com.iqkv.foundation.iamservice.shared.exception.PasswordResetTokenNotFoundException;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final MessageSource messageSource;
  private final IamServiceMetrics metrics;

  public PasswordResetServiceImpl(
      final UserMapper userMapper,
      final PasswordResetTokenMapper passwordResetTokenMapper,
      final PasswordEncoder passwordEncoder,
      final MessagingService messagingService,
      final AuthConfigurationProperties authProps,
      final NotificationConfigurationProperties notificationProps,
      final MessageSource messageSource,
      final IamServiceMetrics metrics) {
    this.userMapper = userMapper;
    this.passwordResetTokenMapper = passwordResetTokenMapper;
    this.passwordEncoder = passwordEncoder;
    this.messagingService = messagingService;
    this.authProps = authProps;
    this.notificationProps = notificationProps;
    this.messageSource = messageSource;
    this.metrics = metrics;
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
    metrics.recordUserLifecycleEvent("password_reset_initiated");

    // Audit event — fire-and-forget, published before sending the email notification
    try {
      final var pwEvent = new PasswordEvent(
          user.getId(), user.getEmail(), null,
          PasswordEvent.EventType.PASSWORD_RESET_INITIATED,
          null, Instant.now());
      messagingService.publishPasswordEvent(pwEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_RESET_INITIATED audit event for userId={}", user.getId(), e);
    }

    final String resetUrl = (notificationProps.baseUrl() != null ? notificationProps.baseUrl() : "") + "/reset-password?token=" + tokenValue;
    final var payload = new java.util.HashMap<String, Object>();
    payload.put("resetUrl", resetUrl);
    payload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    payload.put("token", tokenValue);

    final var event = new NotificationEvent(
        email,
        notificationProps.defaultLocale(),
        NotificationEventType.PASSWORD_RESET_INITIATE,
        payload,
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
      throw new InvalidPasswordException(
          messageSource.getMessage("error.password-reset.invalid-password", null, Locale.ENGLISH));
    }

    final String hash = passwordEncoder.encode(newPassword);
    userMapper.updatePassword(prt.getUserId(), hash, Instant.now());
    passwordResetTokenMapper.deleteByToken(token);
    userMapper.updateLastGlobalSignoutAt(prt.getUserId(), Instant.now());
    metrics.recordUserLifecycleEvent("password_reset_completed");

    final var user = userMapper.findById(prt.getUserId()).orElse(null);

    // Audit event — fire-and-forget
    try {
      final var pwEvent = new PasswordEvent(
          prt.getUserId(),
          user != null ? user.getEmail() : null,
          null,
          PasswordEvent.EventType.PASSWORD_RESET_COMPLETED,
          prt.getUserId(), Instant.now());
      messagingService.publishPasswordEvent(pwEvent);
    } catch (final Exception e) {
      log.warn("Failed to publish PASSWORD_RESET_COMPLETED audit event for userId={}", prt.getUserId(), e);
    }

    if (user != null) {
      final var confirmedPayload = new java.util.HashMap<String, Object>();
      confirmedPayload.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");

      final var event = new NotificationEvent(
          user.getEmail(),
          notificationProps.defaultLocale(),
          NotificationEventType.PASSWORD_RESET_CONFIRMED,
          confirmedPayload,
          Instant.now());
      try {
        messagingService.publishNotification(event);
      } catch (final Exception e) {
        log.warn("Failed to publish PASSWORD_RESET_CONFIRMED notification for user {}", user.getId(), e);
      }
    }
  }
}
