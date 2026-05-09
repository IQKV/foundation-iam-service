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

package com.iqkv.foundation.iamservice.shared.exception;

/**
 * Thrown when a client supplies an account status value that does not match
 * any constant in {@link com.iqkv.foundation.iamservice.user.AccountStatus}.
 *
 * <p>Maps to HTTP 400 Bad Request.
 */
public class InvalidAccountStatusException extends RuntimeException {

  public InvalidAccountStatusException(final String value, final String[] allowed) {
    super("Invalid account status: '" + value + "'. Allowed values: " + String.join(", ", allowed));
  }
}
