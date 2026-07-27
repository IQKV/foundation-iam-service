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

package com.iqkv.foundation.iamservice.locale.dto;

import com.iqkv.foundation.iamservice.locale.Locale;

public final class LocaleDtoMapper {

  private LocaleDtoMapper() {
  }

  public static LocaleDtos.LocaleResponse toResponse(final Locale locale) {
    return new LocaleDtos.LocaleResponse(
        locale.getCode(),
        locale.getName(),
        locale.getNativeName(),
        locale.getIsActive(),
        locale.getIsDefault()
    );
  }
}
