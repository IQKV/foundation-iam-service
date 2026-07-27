/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.iamservice.security;

/**
 * Constants for JWT claim names used throughout the IAM service.
 * All JWT claim access must use these constants — never raw strings.
 */
public final class JwtClaimNames {

  public static final String SUB = "sub";
  public static final String ISS = "iss";
  public static final String IAT = "iat";
  public static final String EXP = "exp";
  public static final String JTI = "jti";
  public static final String TYPE = "type";
  public static final String USER_ID = "user_id";
  public static final String EMAIL = "email";
  public static final String FIRST_NAME = "first_name";
  public static final String LAST_NAME = "last_name";
  public static final String TENANT_ID = "tenant_id";
  public static final String AUTHORITIES = "authorities";
  public static final String EMAIL_VERIFIED = "email_verified";
  public static final String PLAN_CODE = "plan_code";
  public static final String ONBOARDING_COMPLETED = "onboarding_completed";
  public static final String PROFILE_COMPLETED = "profile_completed";

  public static final String TYPE_ACCESS = "access";
  public static final String TYPE_REFRESH = "refresh";

  public static final String ISSUER = "foundation-iam-service";

  private JwtClaimNames() {
  }
}
