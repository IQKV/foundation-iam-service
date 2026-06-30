package com.iqkv.foundation.iamservice.oauth2;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.dto.OidcDtos;

public interface OidcAdminService {

  List<OidcDtos.AdminLinkedIdentityResponse> listUserIdentities(UUID userId);

  void unmergeIdentity(UUID userId, UUID identityId, UUID actorUserId);
}
