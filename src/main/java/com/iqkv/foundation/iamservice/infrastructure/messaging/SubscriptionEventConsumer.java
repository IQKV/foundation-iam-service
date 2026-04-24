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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.iqkv.foundation.iamservice.infrastructure.config.RabbitMQConfig;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException;
import com.iqkv.foundation.iamservice.tenant.Tenant;
import com.iqkv.foundation.iamservice.tenant.TenantService;
import com.iqkv.foundation.iamservice.tenant.TenantStatus;

@Component
@ConditionalOnProperty(name = "iqkv.messaging.rabbitmq.enabled", havingValue = "true")
public class SubscriptionEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);

  private final TenantService tenantService;

  public SubscriptionEventConsumer(final TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @RabbitListener(queues = RabbitMQConfig.SUBSCRIPTION_EVENTS_QUEUE)
  public void handleSubscriptionCancelled(final SubscriptionEvent event) {
    final String tenantKey = event.getTenantKey();
    log.info("Received subscription.cancelled event: tenantKey={}, externalSubscriptionId={}",
        tenantKey, event.getExternalSubscriptionId());

    // 1. Look up tenant via tenantService.getTenantByKey(tenantKey)
    //    If not found, log error and throw (→ DLQ)
    final Tenant tenant;
    try {
      tenant = tenantService.getTenantByKey(tenantKey);
    } catch (final TenantNotFoundException e) {
      log.error("Tenant not found for subscription.cancelled event: tenantKey={}", tenantKey, e);
      throw e; // → DLQ after retry exhaustion
    }

    // 2. If tenant already SUSPENDED, log warning and return (idempotent ack)
    if (tenant.getStatus() == TenantStatus.SUSPENDED) {
      log.warn("Tenant already SUSPENDED, skipping status update: tenantKey={}", tenantKey);
      return;
    }

    // 3. Otherwise call tenantService.updateTenantStatus(tenantKey, SUSPENDED)
    tenantService.updateTenantStatus(tenantKey, TenantStatus.SUSPENDED);
    log.info("Tenant suspended due to subscription cancellation: tenantKey={}", tenantKey);
  }
}
