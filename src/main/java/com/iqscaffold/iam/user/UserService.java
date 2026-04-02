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

import java.util.UUID;

import com.iqscaffold.iam.user.dto.UserDtos;

public interface UserService {

  UserDtos.SignupResponse registerUser(UserDtos.RegisterUserRequest request);

  UserDtos.UserResponse getUserById(UUID userId);

  UserDtos.PagedUserResponse listUsers(int page, int size);

  UserDtos.UserResponse createUser(UserDtos.AdminCreateUserRequest request);

  UserDtos.UserResponse updateUser(UUID userId, String firstName, String lastName, String updatedBy);

  UserDtos.UserResponse patchUser(UUID userId, UserDtos.AdminUpdateUserRequest request);

  void deleteUser(UUID userId, String tenantKey);

  void deleteUserById(UUID userId);


}
