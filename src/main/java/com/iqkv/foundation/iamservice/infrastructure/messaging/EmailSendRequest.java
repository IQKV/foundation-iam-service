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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

/**
 * Transport-layer descriptor for a single outbound email.
 *
 * <p>Produced by {@link EmailService} after template rendering and i18n subject
 * resolution are complete.  Each {@link EmailSender} implementation
 * receives one of these and is responsible only for delivering it — no
 * knowledge of Thymeleaf, MessageSource, or {@link NotificationEvent} required.
 *
 * @param from        the sender address (e.g. {@code "iQ Key Value <noreply@iqkv.com>"})
 * @param fromName    the sender display name (used for SMTP; Resend expects it embedded in {@code from})
 * @param replyTo     optional reply-to address; {@code null} or blank means none
 * @param to          the recipient address
 * @param subject     the fully-resolved, localised email subject
 * @param htmlBody    the fully-rendered HTML body
 */
public record EmailSendRequest(
    String from,
    String fromName,
    String replyTo,
    String to,
    String subject,
    String htmlBody
) {
}
