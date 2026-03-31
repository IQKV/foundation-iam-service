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

package com.iqscaffold.iam.tenant;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.iqscaffold.iam.infrastructure.config.TenancyConfigurationProperties;

@Component
public class StuckTenantReaperJob {

  private static final Logger log = LoggerFactory.getLogger(StuckTenantReaperJob.class);

  private final TenantMapper tenantMapper;
  private final TenancyConfigurationProperties tenancyProps;

  public StuckTenantReaperJob(final TenantMapper tenantMapper,
                               final TenancyConfigurationProperties tenancyProps) {
    this.tenantMapper = tenantMapper;
    this.tenancyProps = tenancyProps;
  }

  @Scheduled(cron = "0 */5 * * * *")
  @SchedulerLock(name = "StuckTenantReaperJob.reapStuckTenants",
      lockAtMostFor = "PT4M", lockAtLeastFor = "PT1M")
  public void reapStuckTenants() {
    final Instant cutoff = Instant.now().minus(tenancyProps.provisioningTimeout());
    final List<Tenant> stuckTenants = tenantMapper.findStuckProvisioning(cutoff);

    if (stuckTenants.isEmpty()) {
      return;
    }

    for (final Tenant tenant : stuckTenants) {
      tenantMapper.updateStatus(tenant.getTenantKey(), TenantStatus.PROVISIONING_FAILED.name(), LocalDateTime.now());
      log.error("Marked stuck tenant as PROVISIONING_FAILED: tenantKey={}, createdAt={}",
          tenant.getTenantKey(), tenant.getCreatedAt());
    }

    log.warn("Reaped {} stuck tenant(s) that exceeded provisioning timeout of {}",
        stuckTenants.size(), tenancyProps.provisioningTimeout());
  }
}
