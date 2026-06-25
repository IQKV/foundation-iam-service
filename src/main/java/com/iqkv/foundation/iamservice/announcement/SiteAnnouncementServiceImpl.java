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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtoMapper;
import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtos;
import com.iqkv.foundation.iamservice.infrastructure.messaging.AnnouncementPublishEvent;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.shared.exception.SiteAnnouncementNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteAnnouncementServiceImpl implements SiteAnnouncementService {

  private static final Logger log = LoggerFactory.getLogger(SiteAnnouncementServiceImpl.class);

  private final SiteAnnouncementMapper announcementMapper;
  private final MessagingService messagingService;

  public SiteAnnouncementServiceImpl(final SiteAnnouncementMapper announcementMapper,
                                     final MessagingService messagingService) {
    this.announcementMapper = announcementMapper;
    this.messagingService = messagingService;
  }

  @Override
  @Transactional
  public SiteAnnouncementDtos.SiteAnnouncementResponse create(final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request) {
    validateEnUsTranslation(request.translations());
    final SiteAnnouncement announcement = SiteAnnouncementDtoMapper.toEntity(request);
    announcement.setId(UUID.randomUUID());
    announcement.setCreatedAt(LocalDateTime.now());

    announcementMapper.insert(announcement);

    for (final SiteAnnouncementTranslation translation : announcement.getTranslations()) {
      translation.setAnnouncementId(announcement.getId());
      announcementMapper.insertTranslation(translation);
    }

    log.info("Site announcement created: announcementId={}", announcement.getId());
    return SiteAnnouncementDtoMapper.toResponse(announcement);
  }

  @Override
  @Transactional
  public SiteAnnouncementDtos.SiteAnnouncementResponse update(final UUID id, final SiteAnnouncementDtos.UpdateSiteAnnouncementRequest request) {
    final SiteAnnouncement announcement = announcementMapper.findById(id)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));

    if (announcement.getStatus() == SiteAnnouncementStatus.PUBLISHED
        || announcement.getStatus() == SiteAnnouncementStatus.PUBLISHING
        || announcement.getStatus() == SiteAnnouncementStatus.PENDING) {
      log.warn("Cannot modify announcement: announcementId={}, status={}", id, announcement.getStatus());
      throw new IllegalStateException("Cannot modify a published or in-progress announcement");
    }

    validateEnUsTranslation(request.translations());

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

    log.info("Site announcement updated: announcementId={}", id);
    return getById(id);
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    final SiteAnnouncement announcement = announcementMapper.findById(id)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));

    if (announcement.getStatus() == SiteAnnouncementStatus.PUBLISHED
        || announcement.getStatus() == SiteAnnouncementStatus.PUBLISHING
        || announcement.getStatus() == SiteAnnouncementStatus.PENDING) {
      log.warn("Cannot delete announcement: announcementId={}, status={}", id, announcement.getStatus());
      throw new IllegalStateException("Cannot delete a published or in-progress announcement");
    }

    announcementMapper.deleteTranslations(id);
    announcementMapper.delete(id);
    log.info("Site announcement deleted: announcementId={}", id);
  }

  private void validateEnUsTranslation(final List<? extends SiteAnnouncementDtos.SiteAnnouncementTranslationRequest> translations) {
    final boolean hasEnUs = translations.stream()
        .anyMatch(t -> "en-US".equalsIgnoreCase(t.locale()));
    if (!hasEnUs) {
      log.warn("Validation failed: missing en-US translation");
      throw new IllegalArgumentException("English (en-US) translation is mandatory");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SiteAnnouncementDtos.SiteAnnouncementResponse getById(final UUID id) {
    return announcementMapper.findById(id)
        .map(SiteAnnouncementDtoMapper::toResponse)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));
  }

  @Override
  @Transactional
  public void publish(final UUID id) {
    final SiteAnnouncement announcement = announcementMapper.findById(id)
        .orElseThrow(() -> new SiteAnnouncementNotFoundException(id));

    if (announcement.getStatus() != SiteAnnouncementStatus.DRAFT) {
      log.warn("Cannot publish non-draft announcement: announcementId={}, status={}", id, announcement.getStatus());
      throw new IllegalStateException("Only draft announcements can be published");
    }

    announcement.setStatus(SiteAnnouncementStatus.PENDING);
    announcementMapper.update(announcement);

    messagingService.publishAnnouncementPublish(new AnnouncementPublishEvent(id, Instant.now()));
    log.info("Site announcement publish initiated: announcementId={}", id);
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
