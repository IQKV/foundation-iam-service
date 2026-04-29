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

package com.iqkv.foundation.iamservice.signup;

import com.iqkv.foundation.iamservice.user.dto.UserDtos;

/**
 * Strategy interface for mode-specific user registration logic.
 *
 * <p>Implementations are selected via {@code @ConditionalOnProperty} based on
 * {@code iqkv.platform.rollout-mode}:
 * <ul>
 *   <li>{@link MultiTenantSignupStrategy} — creates a new tenant per signup for {@code MULTI_TENANT} mode</li>
 *   <li>{@link SingleTenantSignupStrategy} — joins the default tenant for {@code SINGLE_TENANT} mode</li>
 * </ul>
 *
 * <p>Requirements: 5.1
 */
public interface SignupStrategy {

  /**
   * Registers a new user according to the active rollout mode.
   *
   * @param request the registration request
   * @return signup result containing user, tenant, membership, and granted authorities
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantAlreadyExistsException           if tenant name is taken (multi-tenant only)
   * @throws com.iqkv.foundation.iamservice.shared.exception.TenantMembershipAlreadyExistsException if user is already a member of the tenant
   */
  SignupResult execute(UserDtos.RegisterUserRequest request);
}
