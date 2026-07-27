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

package com.iqkv.foundation.iamservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RolloutMode Tests")
class RolloutModeTest {

  @Test
  @DisplayName("Should have SINGLE_TENANT mode")
  void shouldHaveSingleTenantMode() {
    var mode = RolloutMode.SINGLE_TENANT;

    assertThat(mode).isNotNull();
    assertThat(mode.name()).isEqualTo("SINGLE_TENANT");
  }

  @Test
  @DisplayName("Should have MULTI_TENANT mode")
  void shouldHaveMultiTenantMode() {
    var mode = RolloutMode.MULTI_TENANT;

    assertThat(mode).isNotNull();
    assertThat(mode.name()).isEqualTo("MULTI_TENANT");
  }

  @Test
  @DisplayName("Should have exactly two modes")
  void shouldHaveExactlyTwoModes() {
    var modes = RolloutMode.values();

    assertThat(modes).hasSize(2);
    assertThat(modes).containsExactlyInAnyOrder(RolloutMode.SINGLE_TENANT, RolloutMode.MULTI_TENANT);
  }

  @Test
  @DisplayName("Should convert from string")
  void shouldConvertFromString() {
    var singleTenant = RolloutMode.valueOf("SINGLE_TENANT");
    var multiTenant = RolloutMode.valueOf("MULTI_TENANT");

    assertThat(singleTenant).isEqualTo(RolloutMode.SINGLE_TENANT);
    assertThat(multiTenant).isEqualTo(RolloutMode.MULTI_TENANT);
  }
}
