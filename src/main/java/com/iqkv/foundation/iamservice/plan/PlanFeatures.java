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

package com.iqkv.foundation.iamservice.plan;

/**
 * Local copy of the plan feature set as returned by the billing service internal plans endpoint.
 *
 * <p>Intentionally a plain record — no shared library dependency on billing service.
 * Unknown JSON fields are ignored by the Jackson deserializer.
 */
public record PlanFeatures(
    boolean prioritySupport,
    int maxUsers,
    int maxProjects
) {

  /** Safe fallback used when the plan code is unknown or the cache is empty. */
  public static final PlanFeatures NONE = new PlanFeatures(false, 1, 1);
}
