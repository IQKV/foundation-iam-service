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

/**
 * Resolves the default tenant key for single-tenant mode.
 *
 * <p>The resolution follows a priority chain:
 * <ol>
 *   <li>{@code iqkv.tenancy.default-tenant-key} configuration property (if set and valid NanoID format)</li>
 *   <li>Database row where {@code is_default = true}</li>
 *   <li>Generate a new NanoID and insert a new tenant row with {@code is_default = true}</li>
 * </ol>
 */
public interface DefaultTenantResolver {

  /**
   * Resolves the default tenant key using the priority chain described above.
   *
   * @return the default tenant key (never null)
   * @throws DefaultTenantResolutionException if resolution fails
   */
  String resolveDefaultTenantKey();
}
