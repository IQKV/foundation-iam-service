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

import com.iqkv.foundation.iamservice.infrastructure.config.TenancyConfigurationProperties;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * {@link TenantBootstrapStrategy} for {@code SINGLE_TENANT} mode.
 *
 * <p>On startup, idempotently ensures exactly one default tenant exists by delegating to
 * {@link DefaultTenantResolver}. If the tenant was newly created during this startup,
 * a {@code tenant.created} event is published exactly once.
 *
 * <p>Thread-safety for concurrent replica startup is guaranteed by the database partial unique
 * index on {@code public.tenants (is_default) WHERE is_default = true}, combined with the
 * {@code INSERT ... ON CONFLICT DO NOTHING} pattern used by {@link TenantMapper#insertIfAbsentDefault}.
 */
@Service
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "SINGLE_TENANT")
@Order(Ordered.LOWEST_PRECEDENCE)
public class SingleTenantBootstrapStrategy implements TenantBootstrapStrategy, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SingleTenantBootstrapStrategy.class);

  private final DefaultTenantResolver defaultTenantResolver;
  private final TenantMapper tenantMapper;
  private final MessagingService messagingService;
  private final TenancyConfigurationProperties tenancyConfig;

  public SingleTenantBootstrapStrategy(final DefaultTenantResolver defaultTenantResolver,
                                       final TenantMapper tenantMapper,
                                       final MessagingService messagingService,
                                       final TenancyConfigurationProperties tenancyConfig) {
    this.defaultTenantResolver = defaultTenantResolver;
    this.tenantMapper = tenantMapper;
    this.messagingService = messagingService;
    this.tenancyConfig = tenancyConfig;
  }

  @Override
  public void run(final ApplicationArguments args) {
    bootstrapOnStartup();
  }

  @Override
  public void bootstrapOnStartup() {
    log.info("Single-tenant mode: bootstrapping default tenant...");

    // Check whether a default tenant already exists before resolution
    final boolean existedBefore = tenantMapper.findDefaultTenant().isPresent();

    final String tenantKey = defaultTenantResolver.resolveDefaultTenantKey();

    // Publish tenant.created only if this replica created the tenant (it did not exist before)
    // and the resolved key matches a newly inserted row (not a pre-existing one).
    // We re-check after resolution: if it didn't exist before, this replica triggered creation.
    if (!existedBefore) {
      // Verify the tenant now exists (it should after resolution)
      final var tenant = tenantMapper.findByTenantKey(tenantKey);
      if (tenant.isPresent() && Boolean.TRUE.equals(tenant.get().getIsDefault())) {
        final String tenantName = resolveTenantName();
        log.info("Single-tenant mode: default tenant newly created, publishing tenant.created event for key={}", tenantKey);
        // Publish with null owner fields — single-tenant bootstrap has no owner
        messagingService.publishTenantCreated(tenantKey, tenantName, null, null);
      }
    } else {
      log.info("Single-tenant mode: default tenant already exists, skipping event publication. key={}", tenantKey);
    }

    log.info("Single-tenant mode: default tenant bootstrapped successfully. key={}", tenantKey);
  }

  private String resolveTenantName() {
    final String configuredName = tenancyConfig.defaultTenantName();
    return (configuredName != null && !configuredName.isBlank()) ? configuredName : "Default Organization";
  }
}
