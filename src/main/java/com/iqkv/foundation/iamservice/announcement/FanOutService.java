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

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.notification.UserNotification;
import com.iqkv.foundation.iamservice.notification.UserNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FanOutService {

  private final SiteAnnouncementMapper announcementMapper;
  private final UserNotificationMapper notificationMapper;

  public FanOutService(final SiteAnnouncementMapper announcementMapper,
                       final UserNotificationMapper notificationMapper) {
    this.announcementMapper = announcementMapper;
    this.notificationMapper = notificationMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatus(final UUID announcementId, final SiteAnnouncementStatus status) {
    announcementMapper.findById(announcementId).ifPresent(announcement -> {
      announcement.setStatus(status);
      announcementMapper.update(announcement);
    });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveBatch(final List<UserNotification> batch) {
    if (!batch.isEmpty()) {
      notificationMapper.insertBatch(batch);
    }
  }
}
