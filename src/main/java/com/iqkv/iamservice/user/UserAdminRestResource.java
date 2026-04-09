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

package com.iqkv.iamservice.user;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iqkv.iamservice.user.dto.UserDtos;

@RestController
@RequestMapping("/api/v1/iam/admin/users")
@Tag(name = "User Admin", description = "Admin CRUD operations for users")
@Validated
public class UserAdminRestResource {

  private final UserService userService;

  public UserAdminRestResource(final UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  @Operation(summary = "List users", description = "Returns a paginated list of all users, ordered by creation date descending.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of users returned"),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content)
  })
  public ResponseEntity<UserDtos.PagedUserResponse> listUsers(
      @Parameter(description = "Zero-based page index", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "Page size (1–100)", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(userService.listUsers(page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User found"),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> getUser(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @PostMapping
  @Operation(summary = "Create user", description = "Creates a new user with a random temporary password. The user should reset their password on first login.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "User created",
          headers = @Header(name = "Location", description = "URL of the created user",
              schema = @Schema(type = "string", format = "uri"))),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> createUser(
      @Valid @RequestBody UserDtos.AdminCreateUserRequest request) {
    final var created = userService.createUser(request);
    final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Replace user", description = "Full replacement of firstName and lastName. All fields are required.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User updated"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> replaceUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @Valid @RequestBody UserDtos.UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request.firstName(), request.lastName(), "system"));
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Partially update user", description = "Updates only the provided fields. Omitted fields are left unchanged.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User patched"),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserDtos.UserResponse> patchUser(
      @Parameter(description = "User UUID") @PathVariable UUID id,
      @RequestBody UserDtos.AdminUpdateUserRequest request) {
    return ResponseEntity.ok(userService.patchUser(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user", description = "Permanently deletes the user and all associated memberships (cascade).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "User deleted"),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User UUID") @PathVariable UUID id) {
    userService.deleteUserById(id);
    return ResponseEntity.noContent().build();
  }
}
