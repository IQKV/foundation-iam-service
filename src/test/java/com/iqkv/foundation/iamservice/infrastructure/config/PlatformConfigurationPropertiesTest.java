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

@DisplayName("PlatformConfigurationProperties Tests")
class PlatformConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create PlatformConfigurationProperties with SINGLE_TENANT mode")
  void shouldCreateSingleTenantConfiguration() {
    var config = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);

    assertThat(config.rolloutMode()).isEqualTo(RolloutMode.SINGLE_TENANT);
    assertThat(config.getRolloutModeValue()).isEqualTo("SINGLE_TENANT");
  }

  @Test
  @DisplayName("Should create PlatformConfigurationProperties with MULTI_TENANT mode")
  void shouldCreateMultiTenantConfiguration() {
    var config = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);

    assertThat(config.rolloutMode()).isEqualTo(RolloutMode.MULTI_TENANT);
    assertThat(config.getRolloutModeValue()).isEqualTo("MULTI_TENANT");
  }
}
