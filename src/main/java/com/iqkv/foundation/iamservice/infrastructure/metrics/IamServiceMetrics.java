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

package com.iqkv.foundation.iamservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for IAM service operations.
 */
@Component
public class IamServiceMetrics {

  private final MeterRegistry meterRegistry;

  public IamServiceMetrics(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Records an authentication outcome.
   *
   * @param tenantId the tenant identifier (can be null for platform admin)
   * @param type     the type of authentication (e.g. "login", "refresh", "admin_login")
   * @param status   the status of authentication ("success", "failure")
   * @param reason   the reason for failure (e.g. "bad_credentials", "locked", "inactive", "suspended")
   */
  public void recordAuthOutcome(final String tenantId, final String type, final String status, final String reason) {
    Counter.builder("iam.auth.outcome")
        .description("Number of authentication outcomes")
        .tag("tenantId", tenantId != null ? tenantId : "platform")
        .tag("type", type)
        .tag("status", status)
        .tag("reason", reason != null ? reason : "none")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Returns a {@link Timer} for measuring authentication duration.
   */
  public Timer authDurationTimer(final String tenantId, final String type) {
    return Timer.builder("iam.auth.duration")
        .description("Authentication request duration")
        .tag("tenantId", tenantId != null ? tenantId : "platform")
        .tag("type", type)
        .register(meterRegistry);
  }

  /**
   * Records a security event.
   *
   * @param tenantId the tenant identifier
   * @param event    the type of security event (e.g. "account_locked", "token_revoked", "validation_failure")
   */
  public void recordSecurityEvent(final String tenantId, final String event) {
    Counter.builder("iam.security.event")
        .description("Number of security events")
        .tag("tenantId", tenantId != null ? tenantId : "platform")
        .tag("event", event)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Records a user lifecycle event.
   *
   * @param event the type of event (e.g. "registered", "verified", "password_reset_initiated", "password_reset_completed")
   */
  public void recordUserLifecycleEvent(final String event) {
    Counter.builder("iam.user.lifecycle")
        .description("Number of user lifecycle events")
        .tag("event", event)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Records a tenant provisioning outcome.
   *
   * @param status the status of provisioning ("success", "failure")
   */
  public void recordTenantProvisioning(final String status) {
    Counter.builder("iam.tenant.provisioning")
        .description("Number of tenant provisioning outcomes")
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Returns a {@link Timer} for measuring tenant provisioning duration.
   */
  public Timer tenantProvisioningTimer() {
    return Timer.builder("iam.tenant.provisioning.duration")
        .description("Tenant provisioning duration")
        .register(meterRegistry);
  }

  /**
   * Records a messaging operation outcome.
   *
   * @param routingKey the routing key used
   * @param status     the status of the operation ("success", "failure")
   */
  public void recordMessagingOutcome(final String routingKey, final String status) {
    Counter.builder("iam.messaging.publish")
        .description("Number of messaging publish outcomes")
        .tag("routingKey", routingKey)
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }
}
