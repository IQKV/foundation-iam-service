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

import java.util.Map;
import java.util.UUID;

/**
 * Generic WebSocket-only push notification published by any platform service to the
 * {@code notification.iam.push} routing key on the shared {@code iqkv.events} exchange.
 *
 * <p>The IAM service persists the event as a {@code user_notifications} row and immediately
 * delivers it via STOMP to {@code /user/queue/notifications}. No email is sent.
 * The publisher is responsible for resolving the text into the user's preferred language;
 * this service stores and delivers whatever strings it receives.
 *
 * <p>{@code severity} must be one of {@code INFO}, {@code SUCCESS}, {@code WARNING},
 * {@code ERROR}. {@code type} is a free-form tag stored with the row (e.g.
 * {@code VENUE_IMPORT_EXTRACTED}) and can be used by the UI to decide how to render
 * or handle the notification. {@code payload} is optional and stored as JSONB.
 */
public record UserPushNotificationEvent(
    UUID targetUserId,
    String title,
    String message,
    String severity,
    String type,
    Map<String, Object> payload) {
}
