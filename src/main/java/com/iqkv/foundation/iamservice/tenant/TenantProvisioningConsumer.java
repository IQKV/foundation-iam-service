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

package com.iqkv.foundation.iamservice.tenant;

import java.time.LocalDateTime;

import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.messaging.TenantEvent;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.tenancy.TenantLiquibaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class TenantProvisioningConsumer {

  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningConsumer.class);

  private final TenantLiquibaseRunner tenantLiquibaseRunner;
  private final TenantMapper tenantMapper;
  private final MessagingService messagingService;
  private final IamServiceMetrics metrics;

  public TenantProvisioningConsumer(final TenantLiquibaseRunner tenantLiquibaseRunner,
                                    final TenantMapper tenantMapper,
                                    final MessagingService messagingService,
                                    final IamServiceMetrics metrics) {
    this.tenantLiquibaseRunner = tenantLiquibaseRunner;
    this.tenantMapper = tenantMapper;
    this.messagingService = messagingService;
    this.metrics = metrics;
  }

  @RabbitListener(queues = RabbitMQConfig.TENANT_PROVISIONING_QUEUE)
  public void handleTenantProvisioning(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Received tenant provisioning event: tenantKey={}", tenantKey);

    metrics.tenantProvisioningTimer().record(() -> {
      try {
        tenantLiquibaseRunner.runMigrationsForTenant(tenantKey);
        tenantMapper.updateStatus(tenantKey, TenantStatus.ACTIVE.name(), LocalDateTime.now());
        messagingService.publishTenantProvisioned(tenantKey);
        metrics.recordTenantProvisioning("success");
        log.info("Tenant provisioning succeeded: tenantKey={}", tenantKey);
      } catch (final Exception e) {
        log.error("Tenant provisioning failed: tenantKey={}", tenantKey, e);
        tenantMapper.updateStatus(tenantKey, TenantStatus.PROVISIONING_FAILED.name(), LocalDateTime.now());
        messagingService.publishTenantProvisioningFailed(tenantKey);
        metrics.recordTenantProvisioning("failure");
        // Do NOT rethrow — message is not requeued; failed state is observable via status endpoint
      }
    });
  }
}
