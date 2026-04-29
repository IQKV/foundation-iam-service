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

/**
 * Invitation vertical slice — signup by invitation and team joining for multi-tenant SaaS.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Send time-limited invitation emails to new or existing users</li>
 *   <li>Accept invitations: create user account (new) or verify identity (existing)</li>
 *   <li>Create {@code TenantMembership} and {@code TenantMemberAuthority} on acceptance</li>
 *   <li>Revoke pending invitations</li>
 *   <li>Expire stale invitations via {@link com.iqkv.foundation.iamservice.invitation.InvitationReaperJob}</li>
 * </ul>
 *
 * <p>The {@code tenant_invitations} table lives in the system schema ({@code public}) so tokens
 * can be resolved before tenant context is established.
 */

package com.iqkv.foundation.iamservice.invitation;
