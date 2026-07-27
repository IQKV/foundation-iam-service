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

package com.iqkv.foundation.iamservice.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SiteAnnouncement Tests")
class SiteAnnouncementTest {

  @Test
  @DisplayName("Should get and set all fields")
  void shouldGetAndSetAllFields() {
    SiteAnnouncement announcement = new SiteAnnouncement();
    UUID id = UUID.randomUUID();
    String type = "INFO";
    SiteAnnouncementStatus status = SiteAnnouncementStatus.PUBLISHED;
    LocalDateTime createdAt = LocalDateTime.now();
    List<SiteAnnouncementTranslation> translations = new ArrayList<>();

    announcement.setId(id);
    announcement.setType(type);
    announcement.setStatus(status);
    announcement.setCreatedAt(createdAt);
    announcement.setTranslations(translations);

    assertThat(announcement.getId()).isEqualTo(id);
    assertThat(announcement.getType()).isEqualTo(type);
    assertThat(announcement.getStatus()).isEqualTo(status);
    assertThat(announcement.getCreatedAt()).isEqualTo(createdAt);
    assertThat(announcement.getTranslations()).isEqualTo(translations);
  }
}
