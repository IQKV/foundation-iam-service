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

package com.iqkv.foundation.iamservice.platformnote;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.iqkv.foundation.iamservice.platformnote.dto.PlatformNoteDtoMapper;
import com.iqkv.foundation.iamservice.platformnote.dto.PlatformNoteDtos;
import com.iqkv.foundation.iamservice.shared.exception.PlatformNoteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformNoteServiceImpl implements PlatformNoteService {

  private static final Logger log = LoggerFactory.getLogger(PlatformNoteServiceImpl.class);

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final String DEFAULT_SORT_BY = "created_at";
  private static final String DEFAULT_SORT_DIR = "desc";

  private final PlatformNoteMapper noteMapper;

  public PlatformNoteServiceImpl(final PlatformNoteMapper noteMapper) {
    this.noteMapper = noteMapper;
  }

  @Override
  @Transactional
  public PlatformNoteDtos.PlatformNoteResponse create(final UUID actorId,
                                                      final PlatformNoteDtos.CreatePlatformNoteRequest request) {
    final PlatformNote note = PlatformNoteDtoMapper.toEntity(request);
    note.setId(UUID.randomUUID());
    note.setCreatedBy(actorId);
    final LocalDateTime now = LocalDateTime.now();
    note.setCreatedAt(now);
    note.setUpdatedAt(now);

    noteMapper.insert(note);
    log.info("Platform note created: noteId={}, actorId={}", note.getId(), actorId);
    return PlatformNoteDtoMapper.toResponse(note);
  }

  @Override
  @Transactional
  public PlatformNoteDtos.PlatformNoteResponse update(final UUID id,
                                                      final PlatformNoteDtos.UpdatePlatformNoteRequest request) {
    final PlatformNote note = noteMapper.findById(id)
        .orElseThrow(() -> new PlatformNoteNotFoundException(id));

    note.setTitle(request.title());
    note.setBody(request.body());
    note.setSeverity(request.severity());
    note.setStatus(request.status());
    note.setUpdatedAt(LocalDateTime.now());

    noteMapper.update(note);
    log.info("Platform note updated: noteId={}", id);
    return PlatformNoteDtoMapper.toResponse(note);
  }

  @Override
  @Transactional
  public PlatformNoteDtos.PlatformNoteResponse patch(final UUID id,
                                                     final PlatformNoteDtos.PatchPlatformNoteRequest request) {
    final PlatformNote note = noteMapper.findById(id)
        .orElseThrow(() -> new PlatformNoteNotFoundException(id));

    if (request.title() != null) {
      note.setTitle(request.title());
    }
    if (request.body() != null) {
      note.setBody(request.body());
    }
    if (request.severity() != null) {
      note.setSeverity(request.severity());
    }
    if (request.status() != null) {
      note.setStatus(request.status());
    }
    note.setUpdatedAt(LocalDateTime.now());

    noteMapper.update(note);
    log.info("Platform note patched: noteId={}", id);
    return PlatformNoteDtoMapper.toResponse(note);
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    noteMapper.findById(id).orElseThrow(() -> new PlatformNoteNotFoundException(id));
    noteMapper.delete(id);
    log.info("Platform note deleted: noteId={}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public PlatformNoteDtos.PlatformNoteResponse getById(final UUID id) {
    return noteMapper.findById(id)
        .map(PlatformNoteDtoMapper::toResponse)
        .orElseThrow(() -> new PlatformNoteNotFoundException(id));
  }

  @Override
  @Transactional(readOnly = true)
  public PlatformNoteDtos.PagedPlatformNoteResponse list(final PlatformNoteDtos.PlatformNoteListQuery query) {
    final int page = Objects.requireNonNullElse(query.page(), 0);
    final int size = Objects.requireNonNullElse(query.size(), DEFAULT_PAGE_SIZE);
    final String sortBy = sanitizeSortBy(Objects.requireNonNullElse(query.sortBy(), DEFAULT_SORT_BY));
    final String sortDir = sanitizeSortDir(Objects.requireNonNullElse(query.sortDir(), DEFAULT_SORT_DIR));
    final String severityParam = query.severity() != null ? query.severity().name() : null;
    final String statusParam = query.status() != null ? query.status().name() : null;
    final String search = (query.search() != null && !query.search().isBlank()) ? query.search().trim() : null;

    final List<PlatformNote> notes = noteMapper.findAll(search, severityParam, statusParam, sortBy, sortDir, size, page * size);
    final long total = noteMapper.countAll(search, severityParam, statusParam);
    final int totalPages = (int) Math.ceil((double) total / size);

    final List<PlatformNoteDtos.PlatformNoteResponse> content = notes.stream()
        .map(PlatformNoteDtoMapper::toResponse)
        .toList();

    return new PlatformNoteDtos.PagedPlatformNoteResponse(content, page, size, total, totalPages);
  }

  @Override
  @Transactional(readOnly = true)
  public PlatformNoteDtos.PlatformNoteCountResponse count(final PlatformNoteDtos.PlatformNoteListQuery query) {
    final String severityParam = query.severity() != null ? query.severity().name() : null;
    final String statusParam = query.status() != null ? query.status().name() : null;
    final String search = (query.search() != null && !query.search().isBlank()) ? query.search().trim() : null;
    return new PlatformNoteDtos.PlatformNoteCountResponse(noteMapper.countAll(search, severityParam, statusParam));
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private String sanitizeSortBy(final String sortBy) {
    return switch (sortBy) {
      case "title" -> "title";
      case "severity" -> "severity";
      case "status" -> "status";
      case "updatedAt", "updated_at" -> "updated_at";
      default -> "created_at";
    };
  }

  private String sanitizeSortDir(final String sortDir) {
    return "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
  }
}
