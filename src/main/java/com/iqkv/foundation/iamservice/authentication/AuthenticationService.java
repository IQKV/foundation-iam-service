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

package com.iqkv.foundation.iamservice.authentication;

import java.util.List;

import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;

public interface AuthenticationService {

  AuthenticationDtos.TokenResponse signIn(AuthenticationDtos.SignInRequest request);

  /**
   * Platform admin sign-in. No tenant context required.
   * Authenticates the user globally and loads authorities exclusively from {@code platform_authorities}.
   * Returns 403 if the user has no platform-level authortities.
   */
  AuthenticationDtos.TokenResponse adminSignIn(AuthenticationDtos.SignInRequest request);

  AuthenticationDtos.TokenResponse refresh(AuthenticationDtos.RefreshTokenRequest request);

  /**
   * Platform admin token refresh. No tenant context required.
   * Validates the refresh token, re-loads platform authorities, and issues a new token pair.
   * Returns 401 if the token is invalid or expired, 403 if the user no longer has platform authorities.
   */
  AuthenticationDtos.TokenResponse adminRefresh(AuthenticationDtos.RefreshTokenRequest request);

  void signOut(String jti, String userId, String expiresAt);

  void signOutAll(String userId, String jti, String expiresAt);

  AuthenticationDtos.ValidateTokenResponse validateToken(String token);

  List<AuthenticationDtos.TenantMembershipSummary> listUserTenants(String email, String password);

  void verifyEmail(String token);

  void resendVerification(String email);

  /**
   * Returns the provisioning status of a tenant by its key.
   * Used by the public post-signup polling endpoint — no authentication required.
   *
   * @param tenantKey the tenant's unique key
   * @return the current status string: PROVISIONING, ACTIVE, or PROVISIONING_FAILED
   */
  String getProvisioningStatus(String tenantKey);
}
