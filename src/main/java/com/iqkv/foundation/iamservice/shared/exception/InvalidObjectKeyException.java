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
 * Thrown when an S3 object key is malformed or does not belong to the requesting user.
 */
public class InvalidObjectKeyException extends RuntimeException {

  public InvalidObjectKeyException(final String message) {
    super(message);
  }

  public InvalidObjectKeyException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
