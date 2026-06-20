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
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.infrastructure.metrics.IamServiceMetrics;
import com.iqkv.foundation.iamservice.membership.MembershipStatus;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthority;
import com.iqkv.foundation.iamservice.membership.TenantMemberAuthorityMapper;
import com.iqkv.foundation.iamservice.membership.TenantMembership;
import com.iqkv.foundation.iamservice.membership.TenantMembershipMapper;
import com.iqkv.foundation.iamservice.plan.PlanCatalogCache;
import com.iqkv.foundation.iamservice.shared.exception.InvalidTenantStateException;
import com.iqkv.foundation.iamservice.shared.exception.PlanMemberQuotaException;
import com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException;
import com.iqkv.foundation.iamservice.shared.exception.TenantNotFoundException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.shared.util.UserServiceConstants;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtoMapper;
import com.iqkv.foundation.iamservice.tenant.dto.TenantDtos;
import com.iqkv.foundation.iamservice.user.AccountStatus;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final TenantMembershipMapper membershipMapper;
  private final TenantMemberAuthorityMapper authorityMapper;
  private final MessagingService messagingService;
  private final UserMapper userMapper;
  private final IamServiceMetrics metrics;
  private final PlanCatalogCache planCatalogCache;

  public TenantServiceImpl(final TenantMapper tenantMapper,
                           final TenantMembershipMapper membershipMapper,
                           final TenantMemberAuthorityMapper authorityMapper,
                           final MessagingService messagingService,
                           final UserMapper userMapper,
                           final IamServiceMetrics metrics,
                           final PlanCatalogCache planCatalogCache) {
    this.tenantMapper = tenantMapper;
    this.membershipMapper = membershipMapper;
    this.authorityMapper = authorityMapper;
    this.messagingService = messagingService;
    this.userMapper = userMapper;
    this.metrics = metrics;
    this.planCatalogCache = planCatalogCache;
  }

  @Override
  public void updateActivePlanCode(final String tenantKey, final String planCode) {
    tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));
    tenantMapper.updateActivePlanCode(tenantKey, planCode);
    log.info("Active plan code updated: tenantKey={}, planCode={}", tenantKey, planCode);
  }

  // ─── Self-service ──────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public String getProvisioningStatus(final String tenantKey) {
    return tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey))
        .getStatus()
        .name();
  }

  @Override
  public Tenant createTenant(final String tenantName, final UUID ownerUserId) {
    if (tenantMapper.existsByName(tenantName)) {
      throw new TenantAlreadyExistsException("Tenant name already taken");
    }

    // Step 1: Validate owner user exists
    final User owner = userMapper.findById(ownerUserId)
        .orElseThrow(() -> new UserNotFoundException("Owner user not found"));

    final String tenantKey = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NANOID_ALPHABET, NANOID_SIZE);

    // Step 2: Create tenant
    final Tenant tenant = new Tenant();
    tenant.setId(UUID.randomUUID());
    tenant.setTenantKey(tenantKey);
    tenant.setName(tenantName);
    tenant.setStatus(TenantStatus.PROVISIONING);
    tenant.setCreatedAt(LocalDateTime.now());
    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setCreatedBy(ownerUserId.toString());
    tenant.setUpdatedBy(ownerUserId.toString());
    tenant.setIsInternal(false);

    tenantMapper.insertIfAbsent(tenant);
    metrics.recordTenantProvisioning("initiated");

    // Step 3: Quota check: enforce maxUsers from the active plan (0 = unlimited)
    final int maxUsers = planCatalogCache.forPlan(tenant.getActivePlanCode()).maxUsers();
    if (maxUsers > 0) {
      final long current = membershipMapper.countByTenantKey(tenantKey);
      if (current >= maxUsers) {
        throw new PlanMemberQuotaException(current, maxUsers);
      }
    }

    // Step 4: Create owner membership with TENANT_OWNER authority
    final var membership = new TenantMembership();
    membership.setId(UUID.randomUUID());
    membership.setUserId(ownerUserId);
    membership.setTenantKey(tenantKey);
    membership.setStatus(MembershipStatus.ACTIVE);
    membership.setCreatedAt(LocalDateTime.now());
    membership.setUpdatedAt(LocalDateTime.now());
    membership.setCreatedBy(ownerUserId.toString());
    membership.setUpdatedBy(ownerUserId.toString());
    membershipMapper.insert(membership);

    final var authority = new TenantMemberAuthority();
    authority.setId(UUID.randomUUID());
    authority.setMembershipId(membership.getId());
    authority.setAuthority(UserServiceConstants.AUTHORITY_TENANT_OWNER);
    authorityMapper.insert(authority);

    // Step 5: Publish tenant created event
    messagingService.publishTenantCreated(tenantKey, tenantName, owner.getEmail(), owner.getFirstName());

    log.info("Tenant created (provisioning initiated): tenantKey={}, ownerUserId={}", tenantKey, ownerUserId);
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

  // ─── Platform admin ────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public TenantDtos.PagedTenantMemberResponse listMembersByTenantKey(final String tenantKey,
                                                                     final TenantDtos.TenantMemberListQuery query) {
    tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));

    final String safeSortBy = java.util.Set.of("email", "firstName", "lastName", "updatedAt", "createdAt")
        .contains(query.sortBy()) ? query.sortBy() : "createdAt";
    final String safeSortDir = "asc".equalsIgnoreCase(query.sortDir()) ? "asc" : "desc";
    final String safeSearch = (query.search() != null && !query.search().isBlank())
        ? query.search().strip() : null;
    final String safeStatus = Arrays.stream(AccountStatus.values())
        .map(Enum::name)
        .filter(name -> name.equalsIgnoreCase(query.status()))
        .findFirst()
        .orElse(null);

    final int offset = query.page() * query.size();
    final var members = userMapper.findMembersByTenantKeyScoped(
            tenantKey, query.size(), offset, safeSortBy, safeSortDir, safeSearch, safeStatus)
        .stream()
        .map(TenantDtoMapper::toTenantMemberResponse)
        .toList();
    final long total = userMapper.countMembersByTenantKey(tenantKey, safeSearch, safeStatus);
    final int totalPages = (int) Math.ceil((double) total / query.size());
    return new TenantDtos.PagedTenantMemberResponse(members, query.page(), query.size(), total, totalPages);
  }

  @Override
  @Transactional(readOnly = true)
  public TenantDtos.TenantMemberCountResponse countMembersByTenantKey(final String tenantKey) {
    tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));
    return new TenantDtos.TenantMemberCountResponse(tenantKey, membershipMapper.countByTenantKey(tenantKey));
  }

  @Override
  @Transactional(readOnly = true)
  public TenantDtos.TenantCountResponse countTenants() {
    return new TenantDtos.TenantCountResponse(tenantMapper.countAll(null, null));
  }

  @Override
  @Transactional(readOnly = true)
  public TenantDtos.PagedTenantResponse listTenants(final TenantDtos.TenantListQuery query) {
    final String safeSortBy = Set.of("name", "tenantKey", "updatedAt", "createdAt")
        .contains(query.sortBy()) ? query.sortBy() : "createdAt";
    final String safeSortDir = "asc".equalsIgnoreCase(query.sortDir()) ? "asc" : "desc";

    final String safeSearch = (query.search() != null && !query.search().isBlank())
        ? query.search().strip() : null;
    final String safeStatus = Arrays.stream(TenantStatus.values())
        .map(Enum::name)
        .filter(name -> name.equalsIgnoreCase(query.status()))
        .findFirst()
        .orElse(null);

    final int offset = query.page() * query.size();
    final var tenants = tenantMapper.findAll(query.size(), offset, safeSortBy, safeSortDir, safeSearch, safeStatus)
        .stream()
        .map(TenantDtoMapper::toAdminResponse)
        .toList();
    final long total = tenantMapper.countAll(safeSearch, safeStatus);
    final int totalPages = (int) Math.ceil((double) total / query.size());
    return new TenantDtos.PagedTenantResponse(tenants, query.page(), query.size(), total, totalPages);
  }

  @Override
  public TenantDtos.AdminTenantResponse updateTenant(final String tenantKey,
                                                     final TenantDtos.UpdateTenantRequest request) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));

    if (!tenant.getName().equals(request.name()) && tenantMapper.existsByName(request.name())) {
      throw new TenantAlreadyExistsException("Tenant name already taken: " + request.name());
    }

    tenant.setName(request.name());
    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setUpdatedBy("system");
    tenantMapper.update(tenant);

    log.info("Tenant renamed by admin: tenantKey={}, name={}", tenantKey, request.name());
    return TenantDtoMapper.toAdminResponse(tenant);
  }

  @Override
  public TenantDtos.AdminTenantResponse patchTenant(final String tenantKey,
                                                    final TenantDtos.AdminUpdateTenantRequest request) {
    final Tenant tenant = tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));

    if (request.name() != null) {
      if (!tenant.getName().equals(request.name()) && tenantMapper.existsByName(request.name())) {
        throw new TenantAlreadyExistsException("Tenant name already taken: " + request.name());
      }
      tenant.setName(request.name());
    }

    if (request.status() != null && request.status() != tenant.getStatus()) {
      final String transition = tenant.getStatus() + "->" + request.status();
      if (!ALLOWED_TRANSITIONS.contains(transition)) {
        throw new InvalidTenantStateException("Invalid status transition: " + transition);
      }
      tenant.setStatus(request.status());

      if (request.status() == TenantStatus.SUSPENDED) {
        messagingService.publishTenantSuspended(tenantKey);
      } else if (request.status() == TenantStatus.DELETED) {
        messagingService.publishTenantDeleted(tenantKey);
      }
    }

    tenant.setUpdatedAt(LocalDateTime.now());
    tenant.setUpdatedBy("system");
    tenantMapper.update(tenant);

    log.info("Tenant patched by admin: tenantKey={}", tenantKey);
    return TenantDtoMapper.toAdminResponse(tenant);
  }

  @Override
  public void deleteTenant(final String tenantKey) {
    tenantMapper.findByTenantKey(tenantKey)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantKey));
    tenantMapper.deleteByTenantKey(tenantKey);
    messagingService.publishTenantDeleted(tenantKey);
    log.info("Tenant deleted by admin: tenantKey={}", tenantKey);
  }
}
