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

package dev.iqkv.iamservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for IAM service operations.
 *
 * <p>Counters:
 * <ul>
 *   <li>{@code auth.success} (tags: tenantId)</li>
 *   <li>{@code auth.failure} (tags: tenantId, reason)</li>
 *   <li>{@code tenant.created}</li>
 * </ul>
 *
 * <p>Timers:
 * <ul>
 *   <li>{@code auth.duration} (tags: tenantId)</li>
 * </ul>
 */
@Component
public class UserServiceMetrics {

  private final MeterRegistry meterRegistry;

  public UserServiceMetrics(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    // Pre-register the tenant.created counter (no dynamic tags)
    Counter.builder("tenant.created")
        .description("Number of tenants created")
        .register(meterRegistry);
  }

  /**
   * Records a successful authentication for the given tenant.
   */
  public void recordAuthSuccess(final String tenantId) {
    Counter.builder("auth.success")
        .description("Number of successful authentications")
        .tag("tenantId", tenantId)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Records a failed authentication for the given tenant and reason.
   *
   * @param tenantId the tenant identifier
   * @param reason   a short label describing the failure (e.g. "bad_credentials", "locked")
   */
  public void recordAuthFailure(final String tenantId, final String reason) {
    Counter.builder("auth.failure")
        .description("Number of failed authentications")
        .tag("tenantId", tenantId)
        .tag("reason", reason)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Records a tenant creation event.
   */
  public void recordTenantCreated() {
    meterRegistry.counter("tenant.created").increment();
  }

  /**
   * Returns a {@link Timer} for measuring authentication duration for the given tenant.
   * Callers should use {@code timer.record(() -> ...)} or {@code Timer.Sample}.
   */
  public Timer authDurationTimer(final String tenantId) {
    return Timer.builder("auth.duration")
        .description("Authentication request duration")
        .tag("tenantId", tenantId)
        .register(meterRegistry);
  }
}
