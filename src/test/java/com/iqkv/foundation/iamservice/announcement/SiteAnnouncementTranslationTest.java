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

package com.iqkv.foundation.iamservice.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SiteAnnouncementTranslation Tests")
class SiteAnnouncementTranslationTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    SiteAnnouncementTranslation translation = new SiteAnnouncementTranslation();
    UUID announcementId = UUID.randomUUID();
    String locale = "en-US";
    String title = "Welcome";
    String message = "Hello World";

    translation.setAnnouncementId(announcementId);
    translation.setLocale(locale);
    translation.setTitle(title);
    translation.setMessage(message);

    assertThat(translation.getAnnouncementId()).isEqualTo(announcementId);
    assertThat(translation.getLocale()).isEqualTo(locale);
    assertThat(translation.getTitle()).isEqualTo(title);
    assertThat(translation.getMessage()).isEqualTo(message);
  }
}
