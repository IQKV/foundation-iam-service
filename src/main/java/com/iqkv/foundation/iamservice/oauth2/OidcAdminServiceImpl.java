package com.iqkv.foundation.iamservice.oauth2;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.dto.OidcDtos;
import com.iqkv.foundation.iamservice.oauth2.mapper.UserIdentityMapper;
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcAdminServiceImpl implements OidcAdminService {

  private static final Logger log = LoggerFactory.getLogger(OidcAdminServiceImpl.class);

  private final UserIdentityMapper userIdentityMapper;
  private final UserMapper userMapper;

  public OidcAdminServiceImpl(final UserIdentityMapper userIdentityMapper,
                              final UserMapper userMapper) {
    this.userIdentityMapper = userIdentityMapper;
    this.userMapper = userMapper;
  }

  @Override
  public List<OidcDtos.AdminLinkedIdentityResponse> listUserIdentities(final UUID userId) {
    assertUserExists(userId);
    return userIdentityMapper.findByUserId(userId).stream()
        .map(this::toAdminLinkedIdentityResponse)
        .toList();
  }

  @Override
  @Transactional
  public void unmergeIdentity(final UUID userId, final UUID identityId, final UUID actorUserId) {
    assertUserExists(userId);
    final UserIdentity identity = userIdentityMapper.findByUserId(userId).stream()
        .filter(candidate -> identityId.equals(candidate.getId()))
        .findFirst()
        .orElseThrow(() -> new OidcIdentityNotFoundException("OIDC identity not found for user: " + identityId));

    userIdentityMapper.deleteById(identityId);
    log.info("Platform admin {} unmerged OIDC identity {} from user {} (provider={}, providerSub={})",
        actorUserId, identityId, userId, identity.getProvider(), identity.getProviderSub());
  }

  private void assertUserExists(final UUID userId) {
    userMapper.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private OidcDtos.AdminLinkedIdentityResponse toAdminLinkedIdentityResponse(final UserIdentity identity) {
    return new OidcDtos.AdminLinkedIdentityResponse(
        identity.getId(),
        identity.getUserId(),
        identity.getProvider(),
        identity.getProviderSub(),
        identity.getEmail(),
        identity.getDisplayName(),
        identity.getAvatarUrl(),
        identity.getLinkedAt() != null ? identity.getLinkedAt().toString() : null,
        identity.getLastUsedAt() != null ? identity.getLastUsedAt().toString() : null
    );
  }
}
