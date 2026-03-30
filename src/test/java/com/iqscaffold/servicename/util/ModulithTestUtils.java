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

package com.iqscaffold.servicename.util;

import java.util.stream.Stream;

import com.iqscaffold.servicename.ServicenameApplication;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Utility class for modulith testing operations. Provides common functionality needed across modulith tests.
 */
public final class ModulithTestUtils {

  private static final ApplicationModules MODULES = ApplicationModules.of(ServicenameApplication.class);

  private ModulithTestUtils() {
    // Utility class
  }

  /**
   * Gets all application modules.
   *
   * @return Stream of application modules
   */
  public static Stream<ApplicationModule> getAllModules() {
    return MODULES.stream();
  }

  /**
   * Gets a specific module by name.
   *
   * @param moduleName the name of the module
   * @return the application module
   */
  public static ApplicationModule getModule(String moduleName) {
    return MODULES.getModuleByName(moduleName).orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleName));
  }

  /**
   * Verifies that all modules are properly structured.
   */
  public static void verifyAllModules() {
    MODULES.verify();
  }

  /**
   * Gets the application modules instance.
   *
   * @return the application modules
   */
  public static ApplicationModules getApplicationModules() {
    return MODULES;
  }
}
