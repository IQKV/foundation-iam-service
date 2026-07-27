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
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocaleService Unit Tests")
class LocaleServiceImplTest {

  @Mock
  private LocaleMapper localeMapper;

  @InjectMocks
  private LocaleServiceImpl localeService;

  @Test
  @DisplayName("Should return all active locales")
  void shouldReturnAllActiveLocales() {
    // Arrange
    final Locale en = new Locale();
    en.setCode("en-US");
    en.setName("English (US)");
    en.setIsActive(true);
    en.setIsDefault(true);

    final Locale ru = new Locale();
    ru.setCode("ru-RU");
    ru.setName("Russian");
    ru.setIsActive(true);
    ru.setIsDefault(false);

    when(localeMapper.findAllActive()).thenReturn(List.of(en, ru));

    // Act
    final List<Locale> result = localeService.getAllActiveLocales();

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCode()).isEqualTo("en-US");
    assertThat(result.get(1).getCode()).isEqualTo("ru-RU");
  }
}
