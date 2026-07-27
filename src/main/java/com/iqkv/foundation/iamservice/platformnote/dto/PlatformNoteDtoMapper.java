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

package com.iqkv.foundation.iamservice.platformnote.dto;

import com.iqkv.foundation.iamservice.platformnote.PlatformNote;
import com.iqkv.foundation.iamservice.platformnote.PlatformNoteStatus;

public final class PlatformNoteDtoMapper {

  private PlatformNoteDtoMapper() {
  }

  public static PlatformNote toEntity(final PlatformNoteDtos.CreatePlatformNoteRequest request) {
    final PlatformNote note = new PlatformNote();
    note.setTitle(request.title());
    note.setBody(request.body());
    note.setSeverity(request.severity());
    note.setStatus(PlatformNoteStatus.OPEN);
    return note;
  }

  public static PlatformNoteDtos.PlatformNoteResponse toResponse(final PlatformNote note) {
    return new PlatformNoteDtos.PlatformNoteResponse(
        note.getId(),
        note.getTitle(),
        note.getBody(),
        note.getSeverity(),
        note.getStatus(),
        note.getCreatedBy(),
        note.getCreatedAt(),
        note.getUpdatedAt()
    );
  }
}
