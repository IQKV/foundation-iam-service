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

package com.iqscaffold.servicename;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Modulith tests for verifying module structure and boundaries. This test ensures that the application follows proper modular architecture principles.
 */
class ModulithTest {

  private final ApplicationModules modules = ApplicationModules.of(ServicenameApplication.class);

  @Test
  void verifiesModularStructure() {
    // Verify that the application modules are properly structured
    modules.verify();
  }

  @Test
  void createModuleDocumentation() throws Exception {
    // Generate module documentation
    new Documenter(modules).writeDocumentation().writeIndividualModulesAsPlantUml();
  }

  @Test
  void writeModulithDocumentation() throws Exception {
    // Generate comprehensive modulith documentation
    new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
  }
}
