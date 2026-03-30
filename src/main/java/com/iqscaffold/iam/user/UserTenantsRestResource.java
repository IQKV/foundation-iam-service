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

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iqscaffold.iam.authentication.AuthenticationService;
import com.iqscaffold.iam.authentication.dto.AuthenticationDtos;

@RestController
@RequestMapping("/api/v1/iam/users")
public class UserTenantsRestResource {

  private final AuthenticationService authenticationService;

  public UserTenantsRestResource(final AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/tenants")
  public ResponseEntity<List<AuthenticationDtos.TenantMembershipSummary>> listTenants(
      @Valid @RequestBody final AuthenticationDtos.TenantDiscoveryRequest request) {
    return ResponseEntity.ok(authenticationService.listUserTenants(request.email(), request.password()));
  }
}
