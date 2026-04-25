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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * No-op {@link TenantBootstrapStrategy} for {@code MULTI_TENANT} mode.
 *
 * <p>In multi-tenant mode, tenants are created on demand during user signup,
 * so no default tenant bootstrap is required at startup.
 */
@Service
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "MULTI_TENANT")
@Order(Ordered.LOWEST_PRECEDENCE)
public class MultiTenantBootstrapStrategy implements TenantBootstrapStrategy, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(MultiTenantBootstrapStrategy.class);

  @Override
  public void run(final ApplicationArguments args) {
    bootstrapOnStartup();
  }

  @Override
  public void bootstrapOnStartup() {
    log.info("Multi-tenant mode: no default tenant bootstrap required.");
  }
}
