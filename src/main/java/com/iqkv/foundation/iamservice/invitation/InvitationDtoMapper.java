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

package com.iqkv.foundation.iamservice.invitation;

import java.time.ZoneOffset;

/**
 * Static mapper between {@link TenantInvitation} domain objects and {@link InvitationDtos} records.
 * Final utility class — no instances, no Spring bean.
 */
public final class InvitationDtoMapper {

  private InvitationDtoMapper() {}

  public static InvitationDtos.InvitationResponse toResponse(final TenantInvitation invitation) {
    if (invitation == null) {
      throw new IllegalArgumentException("invitation must not be null");
    }
    return new InvitationDtos.InvitationResponse(
        invitation.getId(),
        invitation.getTenantKey(),
        invitation.getInvitedEmail(),
        invitation.getAuthority(),
        invitation.getStatus().name(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt().toInstant(ZoneOffset.UTC));
  }

  public static InvitationDtos.InvitationPreviewResponse toPreviewResponse(
      final TenantInvitation invitation,
      final String tenantName,
      final boolean requiresSignup) {
    if (invitation == null) {
      throw new IllegalArgumentException("invitation must not be null");
    }
    return new InvitationDtos.InvitationPreviewResponse(
        invitation.getId(),
        tenantName,
        invitation.getInvitedEmail(),
        invitation.getAuthority(),
        invitation.getExpiresAt(),
        requiresSignup);
  }
}
