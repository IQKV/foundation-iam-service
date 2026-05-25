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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.announcement.dto.SiteAnnouncementDtos;
import com.iqkv.foundation.iamservice.infrastructure.messaging.MessagingService;
import com.iqkv.foundation.iamservice.shared.exception.SiteAnnouncementNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteAnnouncementService Unit Tests")
class SiteAnnouncementServiceImplTest {

  @Mock
  private SiteAnnouncementMapper announcementMapper;

  @Mock
  private MessagingService messagingService;

  @InjectMocks
  private SiteAnnouncementServiceImpl announcementService;

  @Test
  @DisplayName("Should create announcement")
  void shouldCreateAnnouncement() {
    // Arrange
    final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request = new SiteAnnouncementDtos.CreateSiteAnnouncementRequest(
        "INFO",
        SiteAnnouncementStatus.DRAFT,
        List.of(new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest("en-US", "Title", "Message"))
    );

    // Act
    final SiteAnnouncementDtos.SiteAnnouncementResponse result = announcementService.create(request);

    // Assert
    assertThat(result.id()).isNotNull();
    assertThat(result.type()).isEqualTo("INFO");
    verify(announcementMapper).insert(any(SiteAnnouncement.class));
    verify(announcementMapper, times(1)).insertTranslation(any(SiteAnnouncementTranslation.class));
  }

  @Test
  @DisplayName("Should throw error when creating announcement without en-US translation")
  void shouldThrowErrorOnCreateWithoutEnUs() {
    // Arrange
    final SiteAnnouncementDtos.CreateSiteAnnouncementRequest request = new SiteAnnouncementDtos.CreateSiteAnnouncementRequest(
        "INFO",
        SiteAnnouncementStatus.DRAFT,
        List.of(new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest("ru-RU", "Title", "Message"))
    );

    // Act & Assert
    assertThatThrownBy(() -> announcementService.create(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("English (en-US) translation is mandatory");

    verify(announcementMapper, never()).insert(any());
  }

  @Test
  @DisplayName("Should update announcement")
  void shouldUpdateAnnouncement() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final SiteAnnouncement existing = new SiteAnnouncement();
    existing.setId(id);
    existing.setStatus(SiteAnnouncementStatus.DRAFT);

    when(announcementMapper.findById(id)).thenReturn(Optional.of(existing));

    final SiteAnnouncementDtos.UpdateSiteAnnouncementRequest request = new SiteAnnouncementDtos.UpdateSiteAnnouncementRequest(
        "WARNING",
        SiteAnnouncementStatus.DRAFT,
        List.of(new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest("en-US", "New Title", "New Message"))
    );

    // Act
    announcementService.update(id, request);

    // Assert
    verify(announcementMapper).update(any(SiteAnnouncement.class));
    verify(announcementMapper).deleteTranslations(id);
    verify(announcementMapper).insertTranslation(any(SiteAnnouncementTranslation.class));
  }

  @Test
  @DisplayName("Should throw error when updating published announcement")
  void shouldThrowErrorOnUpdatePublished() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final SiteAnnouncement existing = new SiteAnnouncement();
    existing.setId(id);
    existing.setStatus(SiteAnnouncementStatus.PUBLISHED);

    when(announcementMapper.findById(id)).thenReturn(Optional.of(existing));

    final SiteAnnouncementDtos.UpdateSiteAnnouncementRequest request = new SiteAnnouncementDtos.UpdateSiteAnnouncementRequest(
        "WARNING",
        SiteAnnouncementStatus.PUBLISHED,
        List.of(new SiteAnnouncementDtos.SiteAnnouncementTranslationRequest("en-US", "New Title", "New Message"))
    );

    // Act & Assert
    assertThatThrownBy(() -> announcementService.update(id, request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot modify a published or in-progress announcement");

    verify(announcementMapper, never()).update(any());
  }

  @Test
  @DisplayName("Should delete announcement")
  void shouldDeleteAnnouncement() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final SiteAnnouncement existing = new SiteAnnouncement();
    existing.setId(id);
    existing.setStatus(SiteAnnouncementStatus.DRAFT);

    when(announcementMapper.findById(id)).thenReturn(Optional.of(existing));

    // Act
    announcementService.delete(id);

    // Assert
    verify(announcementMapper).deleteTranslations(id);
    verify(announcementMapper).delete(id);
  }

  @Test
  @DisplayName("Should throw error when deleting publishing announcement")
  void shouldThrowErrorOnDeletePublishing() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final SiteAnnouncement existing = new SiteAnnouncement();
    existing.setId(id);
    existing.setStatus(SiteAnnouncementStatus.PUBLISHING);

    when(announcementMapper.findById(id)).thenReturn(Optional.of(existing));

    // Act & Assert
    assertThatThrownBy(() -> announcementService.delete(id))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot delete a published or in-progress announcement");

    verify(announcementMapper, never()).delete(any());
  }

  @Test
  @DisplayName("Should publish announcement")
  void shouldPublishAnnouncement() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final SiteAnnouncement announcement = new SiteAnnouncement();
    announcement.setId(id);
    announcement.setStatus(SiteAnnouncementStatus.DRAFT);

    when(announcementMapper.findById(id)).thenReturn(Optional.of(announcement));

    // Act
    announcementService.publish(id);

    // Assert
    assertThat(announcement.getStatus()).isEqualTo(SiteAnnouncementStatus.PENDING);
    verify(announcementMapper).update(announcement);
    verify(messagingService).publishAnnouncementPublish(any());
  }
}
