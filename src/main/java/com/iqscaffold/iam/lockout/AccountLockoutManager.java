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

package com.iqscaffold.iam.lockout;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iqscaffold.iam.infrastructure.config.AuthConfigurationProperties;

@Component
public class AccountLockoutManager {

  private final FailedLoginAttemptMapper mapper;
  private final int threshold;
  private final java.time.Duration lockoutDuration;

  public AccountLockoutManager(
      final FailedLoginAttemptMapper mapper,
      final AuthConfigurationProperties authProps) {
    this.mapper = mapper;
    this.threshold = authProps.security().rateLimiting().loginAttempts();
    this.lockoutDuration = authProps.security().rateLimiting().lockoutDuration();
  }

  public void recordFailedAttempt(final String email) {
    var attempt = new FailedLoginAttempt();
    attempt.setId(UUID.randomUUID());
    attempt.setEmail(email);
    attempt.setAttemptedAt(Instant.now());
    mapper.insert(attempt);
  }

  public boolean isLocked(final String email) {
    var since = Instant.now().minus(lockoutDuration);
    var count = mapper.countByEmailAndAttemptedAtAfter(email, since);
    return count >= threshold;
  }

  public void reset(final String email) {
    mapper.deleteByEmail(email);
  }
}
