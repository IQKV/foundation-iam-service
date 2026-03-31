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

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.iqscaffold.iam.infrastructure.config.RabbitMQConfig;
import com.iqscaffold.iam.infrastructure.messaging.MessagingService;
import com.iqscaffold.iam.infrastructure.messaging.TenantEvent;
import com.iqscaffold.iam.tenancy.TenantLiquibaseRunner;

@Component
@ConditionalOnProperty(name = "iqscaffold.messaging.rabbitmq.enabled", havingValue = "true")
public class TenantProvisioningConsumer {

  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningConsumer.class);

  private final TenantLiquibaseRunner tenantLiquibaseRunner;
  private final TenantMapper tenantMapper;
  private final MessagingService messagingService;

  public TenantProvisioningConsumer(final TenantLiquibaseRunner tenantLiquibaseRunner,
                                    final TenantMapper tenantMapper,
                                    final MessagingService messagingService) {
    this.tenantLiquibaseRunner = tenantLiquibaseRunner;
    this.tenantMapper = tenantMapper;
    this.messagingService = messagingService;
  }

  @RabbitListener(queues = RabbitMQConfig.TENANT_PROVISIONING_QUEUE)
  public void handleTenantProvisioning(final TenantEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Received tenant provisioning event: tenantKey={}", tenantKey);

    try {
      tenantLiquibaseRunner.runMigrationsForTenant(tenantKey);
      tenantMapper.updateStatus(tenantKey, TenantStatus.ACTIVE.name(), LocalDateTime.now());
      messagingService.publishTenantUpdated(tenantKey);
      log.info("Tenant provisioning succeeded: tenantKey={}", tenantKey);
    } catch (final Exception e) {
      log.error("Tenant provisioning failed: tenantKey={}", tenantKey, e);
      tenantMapper.updateStatus(tenantKey, TenantStatus.PROVISIONING_FAILED.name(), LocalDateTime.now());
      // Do NOT rethrow — message is not requeued; failed state is observable via status endpoint
    }
  }
}
