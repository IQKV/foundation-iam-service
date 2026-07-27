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

package com.iqkv.foundation.iamservice.locale;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Locale Tests")
class LocaleTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    Locale locale = new Locale();
    String code = "en-US";
    String name = "English";
    String nativeName = "English";
    Boolean isActive = true;
    Boolean isDefault = true;
    LocalDateTime createdAt = LocalDateTime.now();

    locale.setCode(code);
    locale.setName(name);
    locale.setNativeName(nativeName);
    locale.setIsActive(isActive);
    locale.setIsDefault(isDefault);
    locale.setCreatedAt(createdAt);

    assertThat(locale.getCode()).isEqualTo(code);
    assertThat(locale.getName()).isEqualTo(name);
    assertThat(locale.getNativeName()).isEqualTo(nativeName);
    assertThat(locale.getIsActive()).isEqualTo(isActive);
    assertThat(locale.getIsDefault()).isEqualTo(isDefault);
    assertThat(locale.getCreatedAt()).isEqualTo(createdAt);
  }
}
