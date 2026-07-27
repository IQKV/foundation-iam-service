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

package com.iqkv.foundation.iamservice.announcement.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.announcement.SiteAnnouncementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SiteAnnouncementDtos Tests")
class SiteAnnouncementDtosTest {

  @Test
  @DisplayName("Should create SiteAnnouncementTranslationRequest")
  void shouldCreateSiteAnnouncementTranslationRequest() {
    var request = new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest(
        "en-US",
        "Welcome",
        "Hello world"
    );

    assertThat(request.locale()).isEqualTo("en-US");
    assertThat(request.title()).isEqualTo("Welcome");
    assertThat(request.message()).isEqualTo("Hello world");
  }

  @Test
  @DisplayName("Should create CreateSiteAnnouncementRequest")
  void shouldCreateCreateSiteAnnouncementRequest() {
    var translation = new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest(
        "en-US",
        "Welcome",
        "Hello world"
    );
    var request = new SiteAnnouncementDtos.CreateSiteAnnouncementRequest(
        "INFO",
        SiteAnnouncementStatus.PUBLISHED,
        List.of(translation)
    );

    assertThat(request.type()).isEqualTo("INFO");
    assertThat(request.status()).isEqualTo(SiteAnnouncementStatus.PUBLISHED);
    assertThat(request.translations()).hasSize(1);
  }

  @Test
  @DisplayName("Should create UpdateSiteAnnouncementRequest")
  void shouldCreateUpdateSiteAnnouncementRequest() {
    var translation = new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest(
        "en-US",
        "Welcome Updated",
        "Hello world updated"
    );
    var request = new SiteAnnouncementDtos.UpdateSiteAnnouncementRequest(
        "WARNING",
        SiteAnnouncementStatus.DRAFT,
        List.of(translation)
    );

    assertThat(request.type()).isEqualTo("WARNING");
    assertThat(request.status()).isEqualTo(SiteAnnouncementStatus.DRAFT);
    assertThat(request.translations()).hasSize(1);
  }

  @Test
  @DisplayName("Should create SiteAnnouncementTranslationResponse")
  void shouldCreateSiteAnnouncementTranslationResponse() {
    var response = new SiteAnnouncementDtos.SiteAnnouncementTranslationResponse(
        "en-US",
        "Welcome",
        "Hello world"
    );

    assertThat(response.locale()).isEqualTo("en-US");
    assertThat(response.title()).isEqualTo("Welcome");
    assertThat(response.message()).isEqualTo("Hello world");
  }

  @Test
  @DisplayName("Should create SiteAnnouncementResponse")
  void shouldCreateSiteAnnouncementResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var translation = new SiteAnnouncementDtos.SiteAnnouncementTranslationResponse(
        "en-US",
        "Welcome",
        "Hello world"
    );
    var response = new SiteAnnouncementDtos.SiteAnnouncementResponse(
        id,
        "INFO",
        SiteAnnouncementStatus.PUBLISHED,
        createdAt,
        List.of(translation)
    );

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.type()).isEqualTo("INFO");
    assertThat(response.status()).isEqualTo(SiteAnnouncementStatus.PUBLISHED);
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.translations()).hasSize(1);
  }

  @Test
  @DisplayName("Should create SiteAnnouncementListResponse")
  void shouldCreateSiteAnnouncementListResponse() {
    var id = UUID.randomUUID();
    var createdAt = LocalDateTime.now();
    var translation = new SiteAnnouncementDtos.SiteAnnouncementTranslationResponse(
        "en-US",
        "Welcome",
        "Hello world"
    );
    var item = new SiteAnnouncementDtos.SiteAnnouncementResponse(
        id,
        "INFO",
        SiteAnnouncementStatus.PUBLISHED,
        createdAt,
        List.of(translation)
    );
    var response = new SiteAnnouncementDtos.SiteAnnouncementListResponse(
        List.of(item),
        1L
    );

    assertThat(response.items()).hasSize(1);
    assertThat(response.totalElements()).isEqualTo(1L);
  }
}
