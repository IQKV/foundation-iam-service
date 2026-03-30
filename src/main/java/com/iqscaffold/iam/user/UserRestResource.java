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

package com.iqscaffold.iam.user;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqscaffold.iam.user.dto.UserDtos;

@RestController
@RequestMapping("/api/v1/iam/users")
public class UserRestResource {

  private final UserService userService;

  public UserRestResource(final UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserDtos.UserResponse> getProfile(@AuthenticationPrincipal final Jwt jwt) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @PatchMapping("/me")
  public ResponseEntity<UserDtos.UserResponse> updateProfile(
      @AuthenticationPrincipal final Jwt jwt,
      @Valid @RequestBody final UserDtos.UpdateProfileRequest request) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
