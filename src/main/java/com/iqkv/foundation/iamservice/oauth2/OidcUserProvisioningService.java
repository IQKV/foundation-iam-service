package com.iqkv.foundation.iamservice.oauth2;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;

public interface OidcUserProvisioningService {

  AuthenticationDtos.TokenResponse provisionAndIssueTokens(OidcIdentity identity, String tenantKey);
}
