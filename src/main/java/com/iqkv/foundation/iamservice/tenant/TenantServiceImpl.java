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
import java.util.Set;
import java.util.UUID;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException;
import com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

  private static final Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);

  private static final char[] NANOID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int NANOID_SIZE = 8;

  // Allowed status transitions
  private static final Set<String> ALLOWED_TRANSITIONS = Set.of(
      TenantStatus.ACTIVE + "->" + TenantStatus.SUSPENDED,
      TenantStatus.SUSPENDED + "->" + TenantStatus.ACTIVE,
      TenantStatus.ACTIVE + "->" + TenantStatus.DELETED,
      TenantStatus.SUSPENDED + "->" + TenantStatus.DELETED,
      TenantStatus.PROVISIONING_FAILED + "->" + TenantStatus.DELETED
  );

  private final TenantMapper tenantMapper;
  private final MessagingService messagingService;

  public TenantServiceImpl(final TenantMapper tenantMapper, final MessagingService messagingService) {
    this.tenantMapper = tenantMapper;
    this.messagingService = messagingService;
  }

  @Override
  public Tenant createTenant(final String tenantName, final UUID ownerUserId) {
    if (tenantMapper.existsByName(tenantName)) {
      throw new TenantAlreadyExistsException("Tenant name already taken");
    }

    final String tenantKey = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NANOID_ALPHABET, NANOID_SIZE);

    final var tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantKey(tenantKey);
    tenant.setName(tenantName);
    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setCreatedAt(LocalDateTime.now());
    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setCreatedBy(ownerUserId.toString());
    tenant.setUpdatedBy(ownerUserId.toString());

    tenantMapper.insertIfAbsent(tenant);
    messagingService.publishTenantCreated(tenantKey, tenantName);

    log.info("Tenant created: tenantKey={}, name={}", tenantKey, tenantName);
    return tenant;
  }

  @Override
  @Transactional(readOnly = true)
  public Tenant getTenantByKey(final String tenantKey) {
    return tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));
  }

  @Override
  public Tenant updateTenantStatus(final String tenantKey, final TenantStatus newStatus) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));

    final String transition = tenant.getStatus() + "->" + newStatus;
    if (!ALLOWED_TRANSITIONS.contains(transition)) {
      throw new InvalidTenantStateException("Invalid status transition");
    }

    tenantMapper.updateStatus(tenantKey, newStatus.name(), LocalDateTime.now());
    tenant.setStatus(newStatus);
    tenant.setUpdatedAt(LocalDateTime.now());

    if (newStatus == TenantStatus.SUSPENDED) {
      messagingService.publishTenantSuspended(tenantKey);
    } else if (newStatus == TenantStatus.DELETED) {
      messagingService.publishTenantDeleted(tenantKey);
    }

    log.info("Tenant status updated: tenantKey={}, status={}", tenantKey, newStatus);
    return tenant;
  }

  @Override
  public Tenant retryProvisioning(final String tenantKey) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));

    if (tenant.getStatus() != TenantStatus.PROVISIONING_FAILED) {
      throw new InvalidTenantStateException("Tenant is not in PROVISIONING_FAILED state");
    }

    final OwnerInfo ownerInfo = tenantMapper.findOwnerByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Owner not found for tenant: " + tenantKey));

    tenantMapper.updateStatus(tenantKey, TenantStatus.PROVISIONING.name(), LocalDateTime.now());
    messagingService.publishTenantCreated(tenantKey, tenant.getName(), ownerInfo.email(), ownerInfo.firstName());

    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setUpdatedAt(LocalDateTime.now());

    log.info("Tenant provisioning retry initiated: tenantKey={}", tenantKey);
    return tenant;
  }
}
