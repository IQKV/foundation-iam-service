package com.iqkv.foundation.iamservice.oauth2;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserIdentity {
  private UUID id;
  private UUID userId;
  private String provider;
  private String providerSub;
  private String email;
  private String displayName;
  private String avatarUrl;
  private LocalDateTime linkedAt;
  private LocalDateTime lastUsedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getProviderSub() {
    return providerSub;
  }

  public void setProviderSub(String providerSub) {
    this.providerSub = providerSub;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public LocalDateTime getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(LocalDateTime linkedAt) {
    this.linkedAt = linkedAt;
  }

  public LocalDateTime getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(LocalDateTime lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
