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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

public enum NotificationEventType {
  VERIFY_EMAIL,
  EMAIL_VERIFIED,
  PASSWORD_RESET_INITIATE,
  PASSWORD_RESET_CONFIRMED,
  INVITATION,
  /** Sent to the new tenant owner after their tenant is created during multi-tenant signup. */
  TENANT_OWNER_WELCOME,
  /** Sent to the invitee after they successfully accept a tenant invitation. */
  INVITATION_ACCEPTED
}
