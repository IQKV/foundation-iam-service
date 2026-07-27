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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SiteAnnouncement {

  private UUID id;
  private String type;
  private SiteAnnouncementStatus status;
  private LocalDateTime createdAt;
  private List<SiteAnnouncementTranslation> translations = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public SiteAnnouncementStatus getStatus() {
    return status;
  }

  public void setStatus(final SiteAnnouncementStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public List<SiteAnnouncementTranslation> getTranslations() {
    return translations;
  }

  public void setTranslations(final List<SiteAnnouncementTranslation> translations) {
    this.translations = translations;
  }
}
