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

import java.util.Collections;
import java.util.Map;

/**
 * Local copy of the plan feature set as returned by the billing service
 * {@code GET /api/v1/billing/internal/plans} endpoint.
 *
 * <p>Intentionally a plain record — no shared library dependency on billing service.
 * Unknown JSON fields are ignored by the Jackson deserializer.
 *
 * <h3>Design — middle path</h3>
 * <ul>
 *   <li><b>Typed quota fields</b> ({@code maxUsers}, {@code maxProjects}) — kept as named
 *       {@code int} fields for compile-time safety. IAM enforces them at write time
 *       (signup, invitation accept).</li>
 *   <li><b>Open feature map</b> ({@link #features}) — extensible
 *       {@code Map<String, PlanFeature>} keyed by feature code. O(1) lookup,
 *       no duplicate codes, insertion order preserved. Adding a new feature requires
 *       only a YAML change in the billing service — no recompilation here.</li>
 *   <li><b>{@code pricingModel}</b> — optional string carrying the billing pricing mode
 *       ({@code "FLAT"} or {@code "PER_SEAT"}). {@code null} means the billing service
 *       has not yet been updated and the plan should be treated as {@code FLAT}.
 *       Seat-enforcement logic uses this field to decide whether to apply the
 *       purchased-seat count or the plan ceiling as the effective quota limit.</li>
 * </ul>
 *
 * <p>Add typed fields only for quotas this service actually enforces at write time.
 * Everything else belongs in the {@code features} map.
 *
 * <p>{@code maxUsers} and {@code maxProjects} use {@code 0} to mean "unlimited".
 */
public record PlanEntitlement(
    int maxUsers,
    int maxProjects,
    Map<String, PlanFeature> features,
    String pricingModel
) {

  /**
   * Safe fallback used when the plan code is unknown or the cache is empty.
   * Most restrictive quotas, no display features, no pricing model override.
   */
  public static final PlanEntitlement NONE = new PlanEntitlement(1, 1, Collections.emptyMap(), null);

  public PlanEntitlement {
    features = features != null ? Collections.unmodifiableMap(features) : Collections.emptyMap();
  }

  /**
   * Returns {@code true} if the feature map contains an entry for the given code
   * whose value is {@code "true"} (case-insensitive). O(1) lookup.
   *
   * <p>Use this for display-only boolean features. For quota enforcement use the
   * typed fields {@link #maxUsers()} and {@link #maxProjects()} directly.
   *
   * @param code the feature code (e.g. {@code "priority_support"})
   */
  public boolean has(final String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    final PlanFeature feature = features.get(code);
    return feature != null && "true".equalsIgnoreCase(feature.value());
  }

  /**
   * Returns {@code true} if the plan uses per-seat pricing.
   * Falls back to {@code false} (flat) when {@code pricingModel} is absent — safe for
   * existing plans that pre-date the per-seat billing feature.
   */
  public boolean isPerSeat() {
    return "PER_SEAT".equalsIgnoreCase(pricingModel);
  }
}
