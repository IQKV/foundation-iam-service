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

package com.iqkv.foundation.iamservice.user;

import java.util.UUID;

import com.iqkv.foundation.iamservice.user.dto.UserDtos;

/**
 * Service for managing user avatar uploads to S3-compatible object storage.
 *
 * <p>Provides a two-phase upload flow:
 * <ol>
 *   <li><b>Initiate</b> — generates a presigned PUT URL for direct client upload to S3.</li>
 *   <li><b>Confirm</b> — persists the avatar URL in the database after successful upload.</li>
 * </ol>
 *
 * <p>Avatars are stored in the configured bucket with the key pattern:
 * {@code avatars/{userId}/{timestamp}.{extension}}
 */
public interface AvatarService {

  /**
   * Initiates an avatar upload by generating a presigned PUT URL.
   *
   * <p>The client receives a temporary URL (valid for 15 minutes) to upload
   * the avatar file directly to S3. The object key is generated server-side
   * to prevent path traversal and ensure consistent naming.
   *
   * @param userId UUID of the user uploading the avatar
   * @return response containing the presigned URL and object key
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException if no user exists with the given ID
   */
  UserDtos.AvatarUploadInitResponse initiateUpload(UUID userId);

  /**
   * Confirms a successful avatar upload and persists the URL.
   *
   * <p>After the client uploads the file to S3 using the presigned URL,
   * this method validates the object key and updates the user's
   * {@code avatar_url} field. If the user already has an avatar, the old
   * object is deleted from S3.
   *
   * @param userId    UUID of the user confirming the upload
   * @param objectKey the S3 object key returned from {@link #initiateUpload}
   * @return response containing the public avatar URL
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException    if no user exists with the given ID
   * @throws com.iqkv.foundation.iamservice.shared.exception.InvalidObjectKeyException if the object key is malformed or does not belong to the user
   */
  UserDtos.AvatarResponse confirmUpload(UUID userId, String objectKey);

  /**
   * Deletes the user's avatar from both S3 and the database.
   *
   * <p>Sets {@code avatar_url} to {@code null} and removes the object from S3.
   * If the user has no avatar, this method is a no-op.
   *
   * @param userId UUID of the user whose avatar is being deleted
   * @throws com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException if no user exists with the given ID
   */
  void deleteAvatar(UUID userId);
}
