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

package com.iqkv.iamservice.shared.util;

/** Service-wide string constants for the IAM service. */
public final class UserServiceConstants {

  // Authority constants
  public static final String AUTHORITY_TENANT_OWNER = "TENANT_OWNER";
  public static final String AUTHORITY_ADMIN = "ADMIN";
  public static final String AUTHORITY_MEMBER = "MEMBER";

  // API path constants
  public static final String API_BASE_PATH = "/api/v1/iam";
  public static final String AUTH_PATH = API_BASE_PATH + "/auth";
  public static final String USERS_PATH = API_BASE_PATH + "/users";
  public static final String TENANTS_PATH = API_BASE_PATH + "/tenants";

  // Header constants
  public static final String TENANT_ID_HEADER = "X-Tenant-ID";
  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  // MDC key constants
  public static final String MDC_CORRELATION_ID = "correlationId";
  public static final String MDC_TENANT_ID = "tenantId";

  private UserServiceConstants() {}
}
