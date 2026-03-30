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

package com.iqscaffold.iam.authentication;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqscaffold.iam.authentication.dto.AuthenticationDtos;
import com.iqscaffold.iam.security.JwtClaimNames;
import com.iqscaffold.iam.user.UserService;
import com.iqscaffold.iam.user.dto.UserDtos;

@RestController
@RequestMapping("/api/v1/iam/auth")
public class AuthenticationRestResource {

  private final AuthenticationService authenticationService;
  private final UserService userService;

  public AuthenticationRestResource(final AuthenticationService authenticationService,
                                    final UserService userService) {
    this.authenticationService = authenticationService;
    this.userService = userService;
  }

  @PostMapping("/signup")
  public ResponseEntity<UserDtos.SignupResponse> signup(
      @Valid @RequestBody final UserDtos.RegisterUserRequest request) {
    final var response = userService.registerUser(request);
    return ResponseEntity
        .created(URI.create("/api/v1/iam/users/me"))
        .body(response);
  }

  @PostMapping("/signin")
  public ResponseEntity<AuthenticationDtos.TokenResponse> signIn(
      @Valid @RequestBody final AuthenticationDtos.SignInRequest request) {
    return ResponseEntity.ok(authenticationService.signIn(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationDtos.TokenResponse> refresh(
      @Valid @RequestBody final AuthenticationDtos.RefreshTokenRequest request) {
    return ResponseEntity.ok(authenticationService.refresh(request));
  }

  @PostMapping("/signout")
  public ResponseEntity<Void> signOut(@AuthenticationPrincipal final Jwt jwt) {
    final String jti = jwt.getId();
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final Instant expiresAt = jwt.getExpiresAt();
    authenticationService.signOut(jti, userId, expiresAt != null ? expiresAt.toString() : Instant.now().toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/signout-all")
  public ResponseEntity<Void> signOutAll(@AuthenticationPrincipal final Jwt jwt) {
    final String jti = jwt.getId();
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final Instant expiresAt = jwt.getExpiresAt();
    authenticationService.signOutAll(userId, jti, expiresAt != null ? expiresAt.toString() : Instant.now().toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/validate")
  public ResponseEntity<AuthenticationDtos.ValidateTokenResponse> validate(
      @AuthenticationPrincipal final Jwt jwt) {
    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    final String email = jwt.getClaimAsString(JwtClaimNames.EMAIL);
    final String tenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
    final List<String> authorities = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);
    return ResponseEntity.ok(new AuthenticationDtos.ValidateTokenResponse(
        UUID.fromString(userId), email, tenantId,
        authorities != null ? authorities : List.of()));
  }
}
