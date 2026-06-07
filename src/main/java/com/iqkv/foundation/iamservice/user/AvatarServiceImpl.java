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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AvatarServiceImpl implements AvatarService {

  private static final Logger log = LoggerFactory.getLogger(AvatarServiceImpl.class);
  private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

  private final UserMapper userMapper;
  private final MinioClient minioClient;
  private final ObjectStorageConfigurationProperties storageProps;

  public AvatarServiceImpl(final UserMapper userMapper,
                           final MinioClient minioClient,
                           final ObjectStorageConfigurationProperties storageProps) {
    this.userMapper = userMapper;
    this.minioClient = minioClient;
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
      // MinIO 9.x uses io.minio.Http.Method enum
      final String presignedUrl = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(io.minio.Http.Method.PUT)
              .bucket(storageProps.bucketName())
              .object(objectKey)
              .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
              .build());
      final String rewritten = rewriteToPublicEndpoint(presignedUrl);
      log.debug("Presigned PUT URL generated: objectKey={}, url={}", objectKey, rewritten);
      return rewritten;
    } catch (final Exception e) {
      log.error("Failed to generate presigned URL for objectKey={}", objectKey, e);
      throw new RuntimeException("Failed to generate presigned URL", e);
    }
  }

  /**
   * Rewrites the host:port of a presigned URL from the internal MinIO endpoint
   * to the public-facing endpoint, so browser clients can reach it.
   * No-op when {@code publicEndpoint} is blank (local dev or AWS S3).
   *
   * <p>Uses {@link java.net.URI} parsing instead of string prefix matching so that
   * equivalent URLs with/without explicit default ports (e.g. :80, :443) are handled
   * correctly — MinIO SDK may append the port even when it is the scheme default.
   */
  private String rewriteToPublicEndpoint(final String presignedUrl) {
    final String publicEndpoint = storageProps.publicEndpoint();
    if (publicEndpoint == null || publicEndpoint.isBlank()) {
      return presignedUrl;
    }
    try {
      final java.net.URI presigned = new java.net.URI(presignedUrl);
      final java.net.URI internal = new java.net.URI(storageProps.endpoint().replaceAll("/$", ""));
      final java.net.URI pub = new java.net.URI(publicEndpoint.replaceAll("/$", ""));

      // Compare scheme + host + effective port (treat -1 as scheme default)
      final int presignedPort = effectivePort(presigned);
      final int internalPort = effectivePort(internal);

      if (presigned.getScheme().equalsIgnoreCase(internal.getScheme())
          && presigned.getHost().equalsIgnoreCase(internal.getHost())
          && presignedPort == internalPort) {
        // Replace scheme+authority with the public base, keep path+query unchanged
        final String pathAndQuery = presigned.getRawPath()
            + (presigned.getRawQuery() != null ? "?" + presigned.getRawQuery() : "");
        return pub.toString() + pathAndQuery;
      }

      log.warn("Presigned URL host '{}:{}' does not match internal endpoint '{}:{}', returning as-is",
          presigned.getHost(), presignedPort, internal.getHost(), internalPort);
      return presignedUrl;
    } catch (final java.net.URISyntaxException e) {
      log.warn("Could not parse presigned URL '{}', returning as-is", presignedUrl, e);
      return presignedUrl;
    }
  }

  private static int effectivePort(final java.net.URI uri) {
    if (uri.getPort() != -1) {
      return uri.getPort();
    }
    return switch (uri.getScheme().toLowerCase()) {
      case "https" -> 443;
      case "http" -> 80;
      default -> -1;
    };
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
