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

package com.iqscaffold.iam.authentication.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthenticationDtos {

  private AuthenticationDtos() {}

  public record SignInRequest(
      @Email @NotBlank String email,
      @NotBlank String password) {}

  public record SignInResponse(
      String accessToken,
      String refreshToken,
      String tenantKey) {}

  public record RefreshTokenRequest(
      @NotBlank String refreshToken) {}

  public record ValidateTokenResponse(
      String userId,
      String email,
      String tenantId,
      List<String> authorities) {}

  public record TenantDiscoveryRequest(
      @Email @NotBlank String email,
      @NotBlank String password) {}

  public record TenantMembershipSummary(
      String tenantKey,
      String tenantName,
      String membershipStatus,
      List<String> authorities) {}
}
