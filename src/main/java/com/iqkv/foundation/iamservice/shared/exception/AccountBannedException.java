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

package com.iqkv.foundation.iamservice.shared.exception;

/**
 * Thrown when a login attempt is made by a user who is banned (either globally or from the specific tenant).
 * Maps to HTTP 403.
 */
public class AccountBannedException extends RuntimeException {

  public AccountBannedException() {
    super("Account is banned");
  }

  public AccountBannedException(final String message) {
    super(message);
  }
}
