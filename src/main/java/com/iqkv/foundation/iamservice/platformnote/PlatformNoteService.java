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

package com.iqkv.foundation.iamservice.platformnote;

import java.util.UUID;

import com.iqkv.foundation.iamservice.platformnote.dto.PlatformNoteDtos;

public interface PlatformNoteService {

  PlatformNoteDtos.PlatformNoteResponse create(UUID actorId, PlatformNoteDtos.CreatePlatformNoteRequest request);

  PlatformNoteDtos.PlatformNoteResponse update(UUID id, PlatformNoteDtos.UpdatePlatformNoteRequest request);

  PlatformNoteDtos.PlatformNoteResponse patch(UUID id, PlatformNoteDtos.PatchPlatformNoteRequest request);

  void delete(UUID id);

  PlatformNoteDtos.PlatformNoteResponse getById(UUID id);

  PlatformNoteDtos.PagedPlatformNoteResponse list(PlatformNoteDtos.PlatformNoteListQuery query);

  PlatformNoteDtos.PlatformNoteCountResponse count(PlatformNoteDtos.PlatformNoteListQuery query);
}
