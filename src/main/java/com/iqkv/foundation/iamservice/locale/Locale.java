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

import java.time.LocalDateTime;

public class Locale {

  private String code;
  private String name;
  private String nativeName;
  private Boolean isActive;
  private Boolean isDefault;
  private LocalDateTime createdAt;

  public String getCode() {
    return code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getNativeName() {
    return nativeName;
  }

  public void setNativeName(final String nativeName) {
    this.nativeName = nativeName;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(final Boolean isActive) {
    this.isActive = isActive;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(final Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
