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

/**
 * Strategy interface for mode-specific tenant bootstrap actions performed at application startup.
 *
 * <p>Implementations are selected via {@code @ConditionalOnProperty} based on
 * {@code iqkv.platform.rollout-mode}:
 * <ul>
 *   <li>{@link MultiTenantBootstrapStrategy} — no-op for {@code MULTI_TENANT} mode</li>
 *   <li>{@link SingleTenantBootstrapStrategy} — provisions the default tenant for {@code SINGLE_TENANT} mode</li>
 * </ul>
 */
public interface TenantBootstrapStrategy {

  /**
   * Performs startup bootstrap actions for the active rollout mode.
   * Called once during application startup via {@link org.springframework.boot.ApplicationRunner}.
   */
  void bootstrapOnStartup();
}
