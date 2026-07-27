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

package com.iqkv.foundation.iamservice.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserNotificationMapper {

  void insert(UserNotification notification);

  void insertBatch(@Param("notifications") List<UserNotification> notifications);

  Optional<UserNotification> findById(@Param("id") UUID id);

  List<UserNotification> findAllByTargetUserId(@Param("targetUserId") UUID targetUserId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset,
                                               @Param("isRead") Boolean isRead);

  long countByTargetUserId(@Param("targetUserId") UUID targetUserId,
                           @Param("isRead") Boolean isRead);

  void markAsRead(@Param("id") UUID id, @Param("readAt") LocalDateTime readAt);

  void markAllAsRead(@Param("targetUserId") UUID targetUserId, @Param("readAt") LocalDateTime readAt);

  void deleteById(@Param("id") UUID id, @Param("targetUserId") UUID targetUserId);

  void deleteAllByTargetUserId(@Param("targetUserId") UUID targetUserId);

}
