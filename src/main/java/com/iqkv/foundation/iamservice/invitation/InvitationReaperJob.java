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

package com.iqkv.foundation.iamservice.invitation;

import java.time.Instant;
import java.time.LocalDateTime;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job that bulk-expires PENDING invitations whose TTL has passed.
 *
 * <p>Guarded by ShedLock so only one node runs the job in a clustered deployment.
 * Runs every hour; safe to run more frequently — the UPDATE is idempotent.
 */
@Component
public class InvitationReaperJob {

  private static final Logger log = LoggerFactory.getLogger(InvitationReaperJob.class);

  private final InvitationMapper invitationMapper;

  public InvitationReaperJob(final InvitationMapper invitationMapper) {
    this.invitationMapper = invitationMapper;
  }

  @Scheduled(fixedDelayString = "${iqkv.invitation.reaper-interval:PT1H}")
  @SchedulerLock(name = "invitation-reaper", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5M")
  @Transactional
  public void expireStaleInvitations() {
    final int expired = invitationMapper.expireStale(Instant.now(), LocalDateTime.now());
    if (expired > 0) {
      log.info("Invitation reaper: expired {} stale invitation(s)", expired);
    } else {
      log.debug("Invitation reaper: no stale invitations found");
    }
  }
}
