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

package com.iqkv.iamservice.tenant.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import com.iqkv.iamservice.tenant.TenantStatus;

public final class TenantDtos {

  private TenantDtos() {}

  public record UpdateTenantStatusRequest(@NotNull TenantStatus status) {}

  public record TenantResponse(
      String tenantKey,
      String name,
      TenantStatus status,
      LocalDateTime createdAt) {}
}
