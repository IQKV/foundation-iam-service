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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantService;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes subscription lifecycle events published by the Billing service.
 *
 * <p>Handles three event types:
 * <ul>
 *   <li>{@code SUBSCRIPTION_CREATED} — caches the active plan code on the tenant so it
 *       can be stamped into JWT access tokens as the {@code plan_code} claim.</li>
 *   <li>{@code SUBSCRIPTION_UPDATED} — updates the cached plan code when the tenant
 *       upgrades or downgrades their subscription.</li>
 *   <li>{@code SUBSCRIPTION_CANCELLED} — suspends the tenant.</li>
 * </ul>
 *
 * <p>The queue binds to the {@code subscription.#} wildcard, so all three routing keys
 * ({@code subscription.created}, {@code subscription.updated}, {@code subscription.cancelled})
 * are delivered here.
 */
@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class SubscriptionEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);

  private final TenantService tenantService;

  public SubscriptionEventConsumer(final TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @RabbitListener(queues = RabbitMQConfig.SUBSCRIPTION_EVENTS_QUEUE)
  public void handleSubscriptionEvent(final SubscriptionEvent event) {
    if (event.getEventType() == null) {
      log.warn("Received subscription event with null eventType for tenantKey={}", event.getTenantKey());
      return;
    }
    switch (event.getEventType()) {
      case SUBSCRIPTION_CREATED -> handleSubscriptionActivated(event);
      case SUBSCRIPTION_UPDATED -> handleSubscriptionActivated(event);
      case SUBSCRIPTION_CANCELLED -> handleSubscriptionCancelled(event);
      default -> log.debug("Unhandled subscription event type: {}", event.getEventType());
    }
  }

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  /**
   * Caches the active plan code on the tenant when a subscription is created or updated.
   * The plan code is subsequently stamped into JWT access tokens via {@code JwtTokenGenerator}.
   */
  private void handleSubscriptionActivated(final SubscriptionEvent event) {
    final String tenantKey = event.getTenantKey();
    final String planCode = event.getPlanCode();

    if (tenantKey == null || tenantKey.isBlank()) {
      log.warn("Received {} event with missing tenantKey, skipping", event.getEventType());
      return;
    }

    if (planCode == null || planCode.isBlank()) {
      log.debug("Received {} event for tenantKey={} with null planCode, skipping plan cache update",
          event.getEventType(), tenantKey);
      return;
    }

    log.info("Received {} event: tenantKey={}, planCode={}, externalSubscriptionId={}",
        event.getEventType(), tenantKey, planCode, event.getExternalSubscriptionId());

    try {
      tenantService.updateActivePlanCode(tenantKey, planCode);
      log.info("Cached active plan code for tenantKey={}: planCode={}", tenantKey, planCode);
    } catch (final TenantNotFoundException e) {
      log.error("Tenant not found for {} event: tenantKey={}", event.getEventType(), tenantKey, e);
      throw e; // → DLQ after retry exhaustion
    }
  }

  /**
   * Suspends the tenant when their subscription is cancelled.
   */
  private void handleSubscriptionCancelled(final SubscriptionEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Received SUBSCRIPTION_CANCELLED event: tenantKey={}, externalSubscriptionId={}",
        tenantKey, event.getExternalSubscriptionId());

    final Tenant tenant;
    try {
      tenant = tenantService.getTenantByKey(tenantKey);
    } catch (final TenantNotFoundException e) {
      log.error("Tenant not found for subscription.cancelled event: tenantKey={}", tenantKey, e);
      throw e; // → DLQ after retry exhaustion
    }

    if (tenant.getStatus() == TenantStatus.SUSPENDED) {
      log.warn("Tenant already SUSPENDED, skipping status update: tenantKey={}", tenantKey);
      return;
    }

    tenantService.updateTenantStatus(tenantKey, TenantStatus.SUSPENDED);
    log.info("Tenant suspended due to subscription cancellation: tenantKey={}", tenantKey);
  }
}
