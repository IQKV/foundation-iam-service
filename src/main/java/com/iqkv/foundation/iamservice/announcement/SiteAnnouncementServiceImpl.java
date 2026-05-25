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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtoMapper;
import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtos;
import com.iqkv.foundation.iamservice.shared.exception.SiteAnnouncementNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteAnnouncementServiceImpl implements SiteAnnouncementService {

  private final SiteAnnouncementMapper announcementMapper;

  public SiteAnnouncementServiceImpl(final SiteAnnouncementMapper announcementMapper) {
    this.announcementMapper = announcementMapper;
  }

  @Override
  @Transactional
  public SiteAnnouncementDtos.SiteAnnouncementResponse create(final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request) {
    final SiteAnnouncement announcement = SiteAnnouncementDtoMapper.toEntity(request);
    announcement.setId(UUID.randomUUID());
    announcement.setCreatedAt(LocalDateTime.now());

    announcementMapper.insert(announcement);

    for (final SiteAnnouncementTranslation translation : announcement.getTranslations()) {
      translation.setAnnouncementId(announcement.getId());
      announcementMapper.insertTranslation(translation);
    }

    return SiteAnnouncementDtoMapper.toResponse(announcement);
  }

  @Override
  @Transactional
  public SiteAnnouncementDtos.SiteAnnouncementResponse update(final UUID id, final SiteAnnouncementDtos.UpdateSiteAnnouncementRequest request) {
    final SiteAnnouncement announcement = announcementMapper.findById(id)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));

    announcement.setType(request.type());
    announcement.setStatus(request.status());
    announcementMapper.update(announcement);

    announcementMapper.deleteTranslations(id);
    for (final SiteAnnouncementDtos.SiteAnnouncementTranslationRequest tr : request.translations()) {
      final SiteAnnouncementTranslation translation = new SiteAnnouncementTranslation();
      translation.setAnnouncementId(id);
      translation.setLocale(tr.locale());
      translation.setTitle(tr.title());
      translation.setMessage(tr.message());
      announcementMapper.insertTranslation(translation);
    }

    return getById(id);
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    announcementMapper.deleteTranslations(id);
    announcementMapper.delete(id);
  }

  @Override
  @Transactional(readOnly = true)
  public SiteAnnouncementDtos.SiteAnnouncementResponse getById(final UUID id) {
    return announcementMapper.findById(id)
        .map(SiteAnnouncementDtoMapper::toResponse)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));
  }

  @Override
  @Transactional(readOnly = true)
  public SiteAnnouncementDtos.SiteAnnouncementListResponse getAll(final int limit, final int offset) {
    final List<SiteAnnouncement> announcements = announcementMapper.findAll(limit, offset);
    final long totalElements = announcementMapper.countAll();

    final List<SiteAnnouncementDtos.SiteAnnouncementResponse> items = announcements.stream()
        .map(SiteAnnouncementDtoMapper::toResponse)
        .toList();

    return new SiteAnnouncementDtos.SiteAnnouncementListResponse(items, totalElements);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SiteAnnouncementDtos.SiteAnnouncementResponse> getActiveByLocale(final String locale) {
    return announcementMapper.findActiveByLocale(locale)
        .stream()
        .map(SiteAnnouncementDtoMapper::toResponse)
        .toList();
  }
}
