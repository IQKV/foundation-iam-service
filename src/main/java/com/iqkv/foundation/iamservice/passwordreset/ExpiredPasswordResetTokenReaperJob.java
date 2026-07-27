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

package com.iqkv.foundation.iamservice.passwordreset;

import java.time.Instant;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredPasswordResetTokenReaperJob {

  private static final Logger log = LoggerFactory.getLogger(ExpiredPasswordResetTokenReaperJob.class);

  private final PasswordResetTokenMapper passwordResetTokenMapper;

  public ExpiredPasswordResetTokenReaperJob(final PasswordResetTokenMapper passwordResetTokenMapper) {
    this.passwordResetTokenMapper = passwordResetTokenMapper;
  }

  @Scheduled(cron = "0 0 * * * *")
  @SchedulerLock(name = "ExpiredPasswordResetTokenReaperJob.cleanup",
                 lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")
  public void cleanup() {
    log.debug("Cleaning up expired password reset tokens");
    passwordResetTokenMapper.deleteByExpiresAtBefore(Instant.now());
  }
}
