/*
 * Copyright 2026 iQKV Foundation Team.
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
import java.util.UUID;
import java.util.regex.Pattern;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.iqkv.foundation.iamservice.infrastructure.config.TenancyConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link DefaultTenantResolver}.
 *
 * <p>Resolves the default tenant key using the following priority chain:
 * <ol>
 *   <li>{@code iqkv.tenancy.default-tenant-key} if configured and valid NanoID format</li>
 *   <li>Database row where {@code is_default = true}</li>
 *   <li>Generate a new NanoID and insert a new tenant row</li>
 * </ol>
 *
 * <p>Thread-safety is ensured by the database partial unique index on {@code is_default = true},
 * combined with the {@code INSERT ... ON CONFLICT DO NOTHING} pattern in
 * {@link TenantMapper#insertIfAbsentDefault(Tenant)}.
 */
@Component
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "SINGLE_TENANT")
public class DefaultTenantResolverImpl implements DefaultTenantResolver {

  private static final Logger log = LoggerFactory.getLogger(DefaultTenantResolverImpl.class);

  private static final char[] NANOID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int NANOID_SIZE = 8;
  private static final Pattern NANOID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");

  private static final String TENANT_MODE_ORIGIN_SINGLE_BOOTSTRAP = "SINGLE_BOOTSTRAP";
  private static final String DEFAULT_TENANT_NAME = "Acme Corp.";

  private final TenantMapper tenantMapper;
  private final TenancyConfigurationProperties tenancyConfig;

  public DefaultTenantResolverImpl(final TenantMapper tenantMapper,
                                   final TenancyConfigurationProperties tenancyConfig) {
    this.tenantMapper = tenantMapper;
    this.tenancyConfig = tenancyConfig;
  }

  @Override
  @Transactional
  public String resolveDefaultTenantKey() {
    // Priority 1: configured default-tenant-key
    final String configuredKey = tenancyConfig.defaultTenantKey();
    if (configuredKey != null && !configuredKey.isBlank()) {
      if (!isValidNanoId(configuredKey)) {
        throw new DefaultTenantResolutionException(
            "Configured iqkv.tenancy.default-tenant-key '" + configuredKey
            + "' is not a valid NanoID (must be 8 chars, alphabet [a-z0-9]).");
      }
      log.debug("Default tenant key resolved from configuration: {}", configuredKey);
      ensureTenantExists(configuredKey);
      return configuredKey;
    }

    // Priority 2: existing default tenant in database
    final var existing = tenantMapper.findDefaultTenant();
    if (existing.isPresent()) {
      final String key = existing.get().getTenantKey();
      log.debug("Default tenant key resolved from database: {}", key);
      return key;
    }

    // Priority 3: generate new NanoID and insert
    return createNewDefaultTenant();
  }

  /**
   * Ensures a tenant row exists for the given key, creating it as the default tenant if absent.
   */
  private void ensureTenantExists(final String tenantKey) {
    final var existing = tenantMapper.findByTenantKey(tenantKey);
    if (existing.isEmpty()) {
      log.info("Configured default tenant key '{}' not found in database; creating it.", tenantKey);
      insertDefaultTenant(tenantKey);
    } else if (!Boolean.TRUE.equals(existing.get().getIsDefault())) {
      log.info("Tenant '{}' exists but is not marked as default; marking it now.", tenantKey);
      tenantMapper.markDefaultTenant(tenantKey);
    }
  }

  /**
   * Generates a new NanoID, inserts a default tenant row, and returns the key.
   * Uses {@code INSERT ... ON CONFLICT DO NOTHING} for thread-safety; if a concurrent
   * replica wins the race, we fall back to reading the winner's row.
   */
  private String createNewDefaultTenant() {
    final String newKey = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NANOID_ALPHABET, NANOID_SIZE);
    log.info("No default tenant found; generating new default tenant with key={}", newKey);
    insertDefaultTenant(newKey);

    // After the insert (which may have been a no-op due to conflict), read the actual winner
    return tenantMapper.findDefaultTenant()
        .map(Tenant::getTenantKey)
        .orElseThrow(() -> new DefaultTenantResolutionException(
            "Failed to resolve default tenant after insert attempt."));
  }

  private void insertDefaultTenant(final String tenantKey) {
    final var tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantKey(tenantKey);
    tenant.setName(resolveTenantName());
    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setIsDefault(true);
    tenant.setTenantModeOrigin(TENANT_MODE_ORIGIN_SINGLE_BOOTSTRAP);
    tenant.setCreatedAt(LocalDateTime.now());
    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setCreatedBy("system");
    tenant.setUpdatedBy("system");
    tenantMapper.insertIfAbsentDefault(tenant);
  }

  private String resolveTenantName() {
    final String configuredName = tenancyConfig.defaultTenantName();
    return (configuredName != null && !configuredName.isBlank()) ? configuredName : DEFAULT_TENANT_NAME;
  }

  private boolean isValidNanoId(final String value) {
    return NANOID_PATTERN.matcher(value).matches();
  }
}
