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

import java.util.List;

import com.iqkv.foundation.iamservice.announcement.SiteAnnouncement;
import com.iqkv.foundation.iamservice.announcement.SiteAnnouncementTranslation;

public final class SiteAnnouncementDtoMapper {

  private SiteAnnouncementDtoMapper() {
  }

  public static SiteAnnouncementDtos.SiteAnnouncementResponse toResponse(final SiteAnnouncement announcement) {
    final List<SiteAnnouncementDtos.SiteAnnouncementTranslationResponse> translationResponses = announcement.getTranslations()
        .stream()
        .map(t -> new SiteAnnouncementDtos.SiteAnnouncementTranslationResponse(
            t.getLocale(),
            t.getTitle(),
            t.getMessage()
        ))
        .toList();

    return new SiteAnnouncementDtos.SiteAnnouncementResponse(
        announcement.getId(),
        announcement.getType(),
        announcement.getStatus(),
        announcement.getCreatedAt(),
        translationResponses
    );
  }

  public static SiteAnnouncement toEntity(final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request) {
    final SiteAnnouncement announcement = new SiteAnnouncement();
    announcement.setType(request.type());
    announcement.setStatus(request.status());
    announcement.setTranslations(request.translations().stream()
        .map(t -> {
          final SiteAnnouncementTranslation translation = new SiteAnnouncementTranslation();
          translation.setLocale(t.locale());
          translation.setTitle(t.title());
          translation.setMessage(t.message());
          return translation;
        })
        .toList());
    return announcement;
  }
}
