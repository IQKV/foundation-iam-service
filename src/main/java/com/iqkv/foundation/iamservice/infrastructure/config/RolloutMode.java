package com.iqkv.foundation.iamservice.infrastructure.config;

/**
 * Defines the platform-wide operational mode.
 * This mode must be consistent across all core services (IAM, Billing, Gateway).
 */
public enum RolloutMode {
  /**
   * Multi-tenant mode: each user signup creates a new tenant.
   * Users are granted TENANT_OWNER authority.
   * Subscriptions are scoped to tenants.
   */
  MULTI_TENANT,

  /**
   * Single-tenant mode: all users join a pre-provisioned default tenant.
   * Users are granted MEMBER authority.
   * Subscriptions are scoped to individual users.
   */
  SINGLE_TENANT
}
