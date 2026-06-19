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

import com.iqkv.foundation.iamservice.shared.exception.PlanFeatureNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Guard component for plan feature entitlement checks.
 *
 * <p>Acts as the single authoritative gate for "does this plan include feature X?" decisions
 * within the IAM service. All callers pass the {@code planCode} extracted from the caller's
 * JWT ({@code plan_code} claim); this guard resolves the feature set from the local
 * {@link PlanCatalogCache} — no remote call is made at check time.
 *
 * <p>A feature is considered <em>enabled</em> when its entry exists in the plan's features
 * map <strong>and</strong> its {@code value} is {@code "true"} (case-insensitive).
 * Missing entries and {@code "false"} values are both treated as disabled.
 *
 * <p>Usage example:
 * <pre>{@code
 * // In a controller, with plan_code sourced from the caller's JWT:
 * planFeatureGuard.require(planCode, PlanFeatureGuard.ADVANCED_ANALYTICS);
 * // Proceeds if enabled; throws PlanFeatureNotAvailableException otherwise.
 * }</pre>
 */
@Component
public class PlanFeatureGuard {

  private static final Logger log = LoggerFactory.getLogger(PlanFeatureGuard.class);

  /**
   * Feature code for the Advanced Analytics dashboard feature.
   */
  public static final String ADVANCED_ANALYTICS = "advanced_analytics";
  /**
   * Feature code for the Priority Support feature.
   */
  public static final String PRIORITY_SUPPORT = "priority_support";

  private final PlanCatalogCache planCatalogCache;

  public PlanFeatureGuard(final PlanCatalogCache planCatalogCache) {
    this.planCatalogCache = planCatalogCache;
  }

  /**
   * Returns {@code true} if the given plan includes {@code featureCode} with value {@code "true"}.
   *
   * @param planCode    the caller's active plan code (e.g. {@code "pro-monthly"}),
   *                    typically extracted from the {@code plan_code} JWT claim;
   *                    {@code null} or blank is treated as an unknown plan (returns {@code false})
   * @param featureCode the feature to check (e.g. {@code "advanced_analytics"})
   * @return {@code true} if the feature is enabled for this plan, {@code false} otherwise
   */
  public boolean hasFeature(final String planCode, final String featureCode) {
    final PlanFeatures features = planCatalogCache.forPlan(planCode);
    final PlanFeature entry = features.features().get(featureCode);
    return entry != null && "true".equalsIgnoreCase(entry.value());
  }

  /**
   * Asserts that the given plan includes {@code featureCode}.
   * Throws {@link PlanFeatureNotAvailableException} if the feature is disabled or absent.
   *
   * <p>Intended to be called at HTTP boundary (controller layer) before delegating to
   * the service, with the {@code planCode} sourced from the caller's JWT {@code plan_code} claim.
   *
   * @param planCode    the caller's active plan code; {@code null} or blank resolves to
   *                    {@link PlanFeatures#NONE} (all features disabled)
   * @param featureCode the feature that must be enabled (e.g. {@code "advanced_analytics"})
   * @throws PlanFeatureNotAvailableException if the feature is not enabled for this plan
   */
  public void require(final String planCode, final String featureCode) {
    if (!hasFeature(planCode, featureCode)) {
      log.debug("Plan feature check failed: planCode={}, featureCode={}", planCode, featureCode);
      throw new PlanFeatureNotAvailableException(featureCode);
    }
  }
}
