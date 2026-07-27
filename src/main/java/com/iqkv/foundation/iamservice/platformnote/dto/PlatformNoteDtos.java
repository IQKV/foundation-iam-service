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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformnote.PlatformNoteSeverity;
import com.iqkv.foundation.iamservice.platformnote.PlatformNoteStatus;

public final class PlatformNoteDtos {

  private PlatformNoteDtos() {
  }

  // ─── Requests ─────────────────────────────────────────────────────────────

  public record CreatePlatformNoteRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 5000) String body,
      @NotNull PlatformNoteSeverity severity) {
  }

  public record UpdatePlatformNoteRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 5000) String body,
      @NotNull PlatformNoteSeverity severity,
      @NotNull PlatformNoteStatus status) {
  }

  public record PatchPlatformNoteRequest(
      @Size(max = 200) String title,
      @Size(max = 5000) String body,
      PlatformNoteSeverity severity,
      PlatformNoteStatus status) {
  }

  // ─── Query ────────────────────────────────────────────────────────────────

  public record PlatformNoteListQuery(
      Integer page,
      Integer size,
      String search,
      PlatformNoteSeverity severity,
      PlatformNoteStatus status,
      String sortBy,
      String sortDir) {
  }

  // ─── Responses ────────────────────────────────────────────────────────────

  public record PlatformNoteResponse(
      UUID id,
      String title,
      String body,
      PlatformNoteSeverity severity,
      PlatformNoteStatus status,
      UUID createdBy,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  public record PagedPlatformNoteResponse(
      List<PlatformNoteResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages) {
  }

  public record PlatformNoteCountResponse(long total) {
  }
}
