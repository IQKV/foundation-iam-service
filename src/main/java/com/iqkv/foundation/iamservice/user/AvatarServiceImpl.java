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

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.iqkv.foundation.iamservice.infrastructure.config.ObjectStorageConfigurationProperties;
import com.iqkv.foundation.iamservice.shared.exception.InvalidObjectKeyException;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.user.dto.UserDtos;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AvatarServiceImpl implements AvatarService {

  private static final Logger log = LoggerFactory.getLogger(AvatarServiceImpl.class);
  private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

  private final UserMapper userMapper;
  private final MinioClient minioClient;
  private final MinioClient presigningMinioClient;
  private final ObjectStorageConfigurationProperties storageProps;

  public AvatarServiceImpl(final UserMapper userMapper,
                           final MinioClient minioClient,
                           @Qualifier("presigningMinioClient")
                           final MinioClient presigningMinioClient,
                           final ObjectStorageConfigurationProperties storageProps) {
    this.userMapper = userMapper;
    this.minioClient = minioClient;
    this.presigningMinioClient = presigningMinioClient;
    this.storageProps = storageProps;
  }

  @Override
  public UserDtos.AvatarUploadInitResponse initiateUpload(final UUID userId) {
    userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    final String objectKey = generateObjectKey(userId);
    final String presignedUrl = generatePresignedPutUrl(objectKey);

    log.info("Avatar upload initiated: userId={}, objectKey={}", userId, objectKey);
    return new UserDtos.AvatarUploadInitResponse(presignedUrl, objectKey, PRESIGNED_URL_EXPIRY_MINUTES);
  }

  @Override
  public UserDtos.AvatarResponse confirmUpload(final UUID userId, final String objectKey) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    // Validate object key belongs to this user
    if (!objectKey.startsWith("avatars/" + userId + "/")) {
      throw new InvalidObjectKeyException("Object key does not belong to user: " + userId);
    }

    // Delete old avatar if exists
    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
      deleteObjectFromS3(extractObjectKeyFromUrl(user.getAvatarUrl()));
    }

    // Build public URL
    final String avatarUrl = buildPublicUrl(objectKey);

    // Persist to database
    userMapper.updateAvatarUrl(userId, avatarUrl);

    log.info("Avatar upload confirmed: userId={}, avatarUrl={}", userId, avatarUrl);
    return new UserDtos.AvatarResponse(avatarUrl);
  }

  @Override
  public void deleteAvatar(final UUID userId) {
    final User user = userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
      log.debug("No avatar to delete for userId={}", userId);
      return;
    }

    final String objectKey = extractObjectKeyFromUrl(user.getAvatarUrl());
    deleteObjectFromS3(objectKey);

    userMapper.updateAvatarUrl(userId, null);
    log.info("Avatar deleted: userId={}", userId);
  }

  private String generateObjectKey(final UUID userId) {
    final long timestamp = Instant.now().toEpochMilli();
    return String.format("avatars/%s/%d.jpg", userId, timestamp);
  }

  private String generatePresignedPutUrl(final String objectKey) {
    try {
      final String presignedUrl = presigningMinioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(io.minio.Http.Method.PUT)
              .bucket(storageProps.bucketName())
              .object(objectKey)
              .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
              .build());
      log.debug("Presigned PUT URL generated: objectKey={}, url={}", objectKey, presignedUrl);
      return presignedUrl;
    } catch (final Exception e) {
      log.error("Failed to generate presigned URL for objectKey={}", objectKey, e);
      throw new RuntimeException("Failed to generate presigned URL", e);
    }
  }

  private String buildPublicUrl(final String objectKey) {
    // Use publicEndpoint when set so the stored avatar URL is browser-reachable.
    // Falls back to endpoint for local dev / AWS S3 (where endpoint is already public).
    final String base = (storageProps.publicEndpoint() != null && !storageProps.publicEndpoint().isBlank())
        ? storageProps.publicEndpoint()
        : storageProps.endpoint();
    return String.format("%s/%s/%s", base.replaceAll("/$", ""), storageProps.bucketName(), objectKey);
  }

  private String extractObjectKeyFromUrl(final String avatarUrl) {
    // Extract object key from URL: {endpoint}/{bucket}/{objectKey}
    final String bucketPrefix = "/" + storageProps.bucketName() + "/";
    final int keyStart = avatarUrl.indexOf(bucketPrefix);
    if (keyStart == -1) {
      log.warn("Could not extract object key from avatarUrl={}", avatarUrl);
      return "";
    }
    return avatarUrl.substring(keyStart + bucketPrefix.length());
  }

  private void deleteObjectFromS3(final String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(storageProps.bucketName())
              .object(objectKey)
              .build());
      log.debug("Deleted object from S3: objectKey={}", objectKey);
    } catch (final Exception e) {
      log.warn("Failed to delete object from S3: objectKey={}", objectKey, e);
      // Non-fatal — avatar URL is already removed from DB
    }
  }
}
