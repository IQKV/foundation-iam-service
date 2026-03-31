# Requirements Document

## Introduction

The Identity and Access Management (IAM) service is a multi-tenant Spring Boot 3.x / Java 21 microservice providing user registration, JWT RS256 authentication, token refresh, account lockout, and tenant lifecycle management. It uses MyBatis 3.x for all persistence (no JPA/Hibernate), Liquibase for schema migrations, and RabbitMQ for async tenant provisioning.

All domain tables reside in the `public` PostgreSQL schema. Tenant isolation is enforced at the application layer via `TenantMembership` — the same user account can belong to multiple tenants with different roles in each. The `MyBatisSchemaInterceptor` and `TenantContext` infrastructure is in place to route future tenant-scoped queries to `t_{tenantKey}` schemas, but no application tables are created there in the current implementation.

## Glossary

- **IAM_Service** / **User_Service**: The simplified user authentication microservice with multi-tenancy support
- **Tenant**: An isolated customer instance identified by a unique `tenantKey`
- **tenantKey**: Immutable 8-char NanoID (alphabet `a-z0-9`) used in `X-Tenant-ID` header, JWT `tenant_id` claim, and schema name `t_{tenantKey}` (e.g. `xk7f2b9a`)
- **tenantName**: Human-readable display name for the tenant; immutable after creation; UNIQUE constraint enforced at the database level; not used as an identifier in headers or JWT claims
- **TenantContext**: ThreadLocal holder that stores the current `tenantKey` for the duration of a request
- **TenantExtractionFilter**: Servlet filter that resolves `tenantKey` from `X-Tenant-ID` header or JWT claim and sets `TenantContext`
- **MyBatisSchemaInterceptor**: MyBatis `Interceptor` that executes `SET search_path TO t_{tenantKey}, public` before each statement when a tenant context is active
- **TenantLiquibaseRunner**: `ApplicationRunner` that applies Liquibase migrations — system migrations to `public` on startup, tenant migrations to `t_{tenantKey}` on provisioning
- **TenantProvisioningConsumer**: RabbitMQ listener that asynchronously runs tenant migrations; sets tenant status to `ACTIVE` on success or `PROVISIONING_FAILED` on error
- **User**: Global identity — email + password + profile + email_verified flag (default false); not scoped to any single tenant; the same user can be a member of multiple tenants
- **TenantMembership**: Bridge record linking a `User` to a `Tenant` with per-membership `status` and `authorities`; `(user_id, tenant_key)` is unique
- **TenantMemberAuthority**: Authority/role scoped to a specific `TenantMembership` (e.g. `TENANT_OWNER`, `ADMIN`, `MEMBER`)
- **MembershipStatus**: `ACTIVE` | `SUSPENDED` | `REMOVED`
- **AccountStatus**: `ACTIVE`
- **TenantStatus**: `PROVISIONING` | `ACTIVE` | `SUSPENDED` | `DELETED` | `PROVISIONING_FAILED`
- **TokenDenylist**: Table of explicitly revoked JWT JTIs (covers regular signout); checked on every authenticated request by `JwtAuthenticationFilter`
- **AccountLockoutManager**: Component that tracks failed login attempts and enforces temporary lockout
- **Password_Reset_Service**: The service component responsible for initiating and completing the password reset flow
- **Password_Reset_Token**: A single-use, time-limited token (64-char hex, `SecureRandom` 32 bytes) stored in the `password_reset_tokens` table; scoped to a global `User`, not to a tenant
- **Password_Reset_Rate_Limiter**: Logic within `Password_Reset_Service` that enforces a maximum number of reset requests per email address within a configurable sliding window
- **JwtAuthenticationFilter**: Filter that checks two revocation conditions before Spring Security processes the JWT: (1) JTI present in `token_denylist`, (2) token `iat ≤ users.last_global_signout_at` (covers signout-all)
- **Public_Schema**: PostgreSQL `public` schema — contains all tables: tenants, users, tenant_memberships, tenant_member_authorities, token_denylist, failed_login_attempts, password_reset_tokens
- **Tenant_Schema**: PostgreSQL schema `t_{tenantKey}` — reserved for future tenant-scoped tables; currently empty

## Requirements

### Requirement 1: Tenant Registration and Provisioning

**User Story:** As a new user, I want to self-register and create a new tenant in a single step, so that I can get an isolated instance of the service without a separate provisioning call.

#### Acceptance Criteria

1. WHEN a signup request is received with a `tenantName` field and no tenant with that name exists, THE User_Service SHALL create a new tenant record with status `PROVISIONING` via `TenantMapper.insert(tenant)` and register the user as its first member with authority `TENANT_OWNER`
2. WHEN a tenant is created, THE Tenant_Manager SHALL generate a unique `tenantKey` as an 8-character lowercase alphanumeric NanoID (alphabet: a-z0-9, e.g. `xk7f2b9a`)
3. WHEN a tenant record is created with status `PROVISIONING`, THE Tenant_Manager SHALL publish a `TenantEvent` with routing key `tenant.created` to the `iqscaffold.events` exchange via RabbitMQ
4. WHEN the `TenantEvent(tenant.created)` is consumed from the `iqscaffold.tenant.provisioning` queue, THE TenantProvisioningConsumer SHALL run tenant-schema migrations via `TenantLiquibaseRunner.runMigrationsForTenant(tenantKey)` (zero changesets for MVP)
5. WHEN provisioning succeeds, THE TenantProvisioningConsumer SHALL update the tenant status to `ACTIVE` via `TenantMapper.updateStatus(tenantKey, ACTIVE)`
6. WHEN provisioning fails (migration throws an exception), THE TenantProvisioningConsumer SHALL update the tenant status to `PROVISIONING_FAILED` via `TenantMapper.updateStatus(tenantKey, PROVISIONING_FAILED)`, log the error, and NOT rethrow (message is not requeued; the failed state is observable via the status endpoint)
7. THE Tenant_Manager SHALL validate that tenant name is between 1 and 100 characters
8. WHEN a signup request creates a new tenant, THE User_Service SHALL return 201 Created with user ID, email, tenantKey, and status `PROVISIONING`; the client polls `GET /api/v1/iam/tenants/{tenantKey}` to detect when status transitions to `ACTIVE`
9. THE tenantKey SHALL be immutable after creation and SHALL be used as the identifier in `X-Tenant-ID` header and JWT `tenant_id` claim
10. THE name field SHALL be the human-readable display name, immutable after creation, and SHALL NOT be used as an identifier in headers or JWT claims; a UNIQUE constraint on `tenants.name` prevents duplicate names and makes the 409 "Tenant name already taken" check reliable
11. THE User_Service SHALL expose `GET /api/v1/iam/tenants/{tenantKey}` endpoint to retrieve tenant status (returns 200 OK; requires `TENANT_OWNER` authority for that tenant)
12. THE User_Service SHALL expose `PATCH /api/v1/iam/tenants/{tenantKey}/status` endpoint for status transitions (returns 200 OK; requires `TENANT_OWNER` authority for that tenant)
13. THE User_Service SHALL run a `@Scheduled` ShedLock-guarded job (`StuckTenantReaperJob.reapStuckTenants`) every 5 minutes that queries for tenants with status `PROVISIONING` whose `created_at` is older than a configurable threshold (`iqscaffold.tenancy.provisioning-timeout`, default `PT10M`) and transitions each to `PROVISIONING_FAILED` via `TenantMapper.updateStatus(tenantKey, PROVISIONING_FAILED)`; the lock SHALL be held for at most 4 minutes (`lockAtMostFor = "PT4M"`) and at least 1 minute (`lockAtLeastFor = "PT1M"`)
14. WHEN a tenant is in `PROVISIONING_FAILED` status, THE Tenant_Manager SHALL allow a retry by transitioning the tenant back to `PROVISIONING` and re-publishing the `TenantEvent(TENANT_CREATED)` message to RabbitMQ; this retry SHALL be triggered via `POST /api/v1/iam/tenants/{tenantKey}/retry-provisioning` (requires `TENANT_OWNER` authority) and returns 202 Accepted
15. THE allowed status transitions SHALL include `PROVISIONING_FAILED → PROVISIONING` exclusively for the retry-provisioning operation; this transition SHALL NOT be available via the generic `PATCH /api/v1/iam/tenants/{tenantKey}/status` endpoint
16. WHEN the retry-provisioning endpoint is called on a tenant NOT in `PROVISIONING_FAILED` status, THE Tenant_Manager SHALL return 409 Conflict with message "Tenant is not in PROVISIONING_FAILED state"
17. THE `TenantMapper` SHALL expose `findStuckProvisioning(Instant olderThan): List<Tenant>` to support the reaper job; the query SHALL select tenants WHERE `status = 'PROVISIONING' AND created_at < #{olderThan}`

### Requirement 2: Tenant Schema Management with MyBatis

**User Story:** As a system administrator, I want MyBatis configured with schema routing so that tenant context is available for routing, while all entities are created in the public schema.

#### Acceptance Criteria

1. THE User_Service SHALL configure MyBatis with `MyBatisSchemaInterceptor` registered in `SqlSessionFactory`
2. THE User*Service SHALL implement `MyBatisSchemaInterceptor` to intercept `StatementHandler.prepare()` and execute `SET search_path TO t*{tenantKey}, public` when a tenant context is active
3. WHEN no tenant context is active, THE `MyBatisSchemaInterceptor` SHALL NOT change the search_path (public schema is the default)
4. THE User_Service SHALL configure `mybatis.mapper-locations=classpath:mappers/**/*.xml` and `map-underscore-to-camel-case=true` in application.yml
5. WHEN MyBatis executes any statement, THE `MyBatisSchemaInterceptor` SHALL automatically route to the schema `t_{tenantKey}` when a tenant context is present
6. THE Tenant_Liquibase_Runner SHALL maintain two migration changelogs: `db/changelog/system/` for the public schema and `db/changelog/tenant/` for per-tenant schemas
7. WHEN the application starts, THE Tenant_Liquibase_Runner SHALL apply system migrations to the public schema
8. WHEN a new tenant is provisioned, THE Tenant*Liquibase_Runner SHALL apply tenant migrations to the `t*{tenantKey}` schema; for MVP the tenant changelog contains zero changesets
9. THE User_Service SHALL create all entity tables (tenants, users, user_authorities) in the public schema via system migrations
10. THE User_Service SHALL create all indexes in the public schema
11. THE `TenantLiquibaseRunner.runMigrationsForTenant` SHALL propagate exceptions to the caller; the `TenantProvisioningConsumer` is responsible for catching the exception and setting the tenant status to `PROVISIONING_FAILED`
12. THE User_Service SHALL store tenant registry in the public schema with table: tenants (id, tenant_key VARCHAR 12 UNIQUE, name VARCHAR 100 UNIQUE NOT NULL, status VARCHAR 20, created_at, updated_at, created_by, updated_by)

### Requirement 3: Tenant Context Management with MyBatis Integration

**User Story:** As a developer, I want tenant context automatically managed per request and integrated with MyBatis schema routing, so that all database operations are automatically routed to the correct tenant schema.

#### Acceptance Criteria

1. THE Tenant_Extraction_Filter SHALL execute before authentication for all requests except `/actuator/**`, `/api-docs/**`, `/swagger-ui/**`, `POST /api/v1/iam/auth/signup`, `POST /api/v1/iam/users/tenants`, `POST /api/v1/iam/users/email/verify`, `POST /api/v1/iam/users/email/resend-verification`, `POST /api/v1/iam/users/password/forgot`, and `POST /api/v1/iam/users/password/reset`
2. WHEN a request is received, THE Tenant_Extraction_Filter SHALL resolve tenant_id from X-Tenant-ID header with priority 1
3. IF X-Tenant-ID header is not present, THEN THE Tenant_Extraction_Filter SHALL resolve tenant_id from JWT token tenant_id claim with priority 2
4. IF tenant_id cannot be resolved, THEN THE Tenant_Extraction_Filter SHALL return 400 Bad Request with message "Tenant ID required"
5. WHEN tenant_id is resolved, THE Tenant_Extraction_Filter SHALL set TenantContext.setCurrentTenant(tenantId)
6. WHEN request processing completes, THE Tenant_Extraction_Filter SHALL call TenantContext.clear() in a finally block
7. THE Tenant_Context SHALL use ThreadLocal storage to maintain tenant_id per request thread
8. THE Tenant_Context SHALL provide static methods: setCurrentTenant(String), getCurrentTenant(), clear()
9. IF getCurrentTenant() is called when no tenant is set, THEN THE Tenant_Context SHALL throw IllegalStateException with message "No tenant context set"
10. THE MyBatis_Schema_Interceptor SHALL call TenantContext.getCurrentTenant() to determine the schema for each statement
11. WHEN MyBatis executes any statement, THE MyBatis_Schema_Interceptor SHALL automatically route the query to the schema returned by TenantContext.getCurrentTenant()
12. THE User_Service SHALL NOT require manual schema switching in mappers or services (MyBatisSchemaInterceptor handles routing transparently)

### Requirement 4: Tenant Membership Model

**User Story:** As a developer, I want tenant membership to be the isolation boundary, so that the same user can belong to multiple tenants with different roles in each (GitHub orgs model).

#### Acceptance Criteria

1. THE `User` domain object SHALL NOT have a `tenant_id` field — it is a global identity; email is globally unique
2. THE `TenantMembership` domain object SHALL be the isolation boundary: `(user_id, tenant_key)` is unique and carries per-membership `status` and `authorities`
3. THE User_Service SHALL resolve the active membership for `(userId, TenantContext.getCurrentTenant())` on every tenant-scoped operation via `TenantMembershipMapper.findByUserIdAndTenantKey(userId, tenantKey)`
4. IF no membership exists for `(userId, tenantKey)`, THEN THE User_Service SHALL return 403 Forbidden with message "User is not a member of this tenant"
5. IF the membership `status` is `SUSPENDED` or `REMOVED`, THEN THE User_Service SHALL return 403 Forbidden
6. THE User_Service SHALL never allow cross-tenant data access — all tenant-scoped operations must resolve and validate the membership first
7. THE `TenantMemberAuthority` domain object SHALL hold the authority list scoped to a specific membership (e.g. `TENANT_OWNER`, `ADMIN`, `MEMBER`)

### Requirement 5: Tenant Status Management

**User Story:** As a system administrator, I want to manage tenant status, so that I can suspend or delete tenants when needed.

#### Acceptance Criteria

1. WHEN a tenant status update request is received, THE Tenant_Manager SHALL allow transitions: ACTIVE → SUSPENDED, SUSPENDED → ACTIVE, ACTIVE → DELETED, SUSPENDED → DELETED, PROVISIONING_FAILED → DELETED
2. WHEN a tenant is set to SUSPENDED status, THE Authentication_Manager SHALL reject all authentication attempts for users in that tenant with 403 Forbidden
3. WHEN a tenant is set to DELETED status, THE Tenant_Manager SHALL mark the tenant record as deleted via `TenantMapper.updateStatus(tenantKey, DELETED)` but SHALL NOT drop the tenant schema or purge membership data; this is an intentional soft-delete for data retention and auditability — schema cleanup and hard-delete are out of scope for this implementation and must be handled by a separate administrative process
4. THE Tenant_Manager SHALL validate that status transitions follow allowed paths (no DELETED → ACTIVE)
5. IF an invalid status transition is requested, THEN THE Tenant_Manager SHALL return 400 Bad Request with message "Invalid status transition"
6. WHEN tenant status update succeeds, THE Tenant_Manager SHALL return 200 OK with updated tenant information; the caller must hold `TENANT_OWNER` authority for the tenant

### Requirement 6: JWT Token Tenant Claims

**User Story:** As a developer, I want JWT tokens to include tenant_id claim, so that tenant context can be resolved from tokens.

#### Acceptance Criteria

1. WHEN an access token is generated, THE JWT_Token_Generator SHALL include tenant_id claim with value from TenantContext.getCurrentTenant()
2. WHEN a refresh token is generated, THE JWT_Token_Generator SHALL include tenant_id claim with value from TenantContext.getCurrentTenant()
3. THE JWT_Token_Generator SHALL use JwtClaimNames.TENANT_ID constant for the tenant_id claim (never raw string "tenant_id")
4. WHEN a token is validated, THE User_Service SHALL extract tenant_id using JwtClaimNames.TENANT_ID constant
5. WHEN token refresh is requested, THE JWT_Token_Generator SHALL validate that tenant_id from refresh token matches TenantContext.getCurrentTenant()
6. IF tenant_id mismatch is detected during token refresh, THEN THE JWT_Token_Generator SHALL return 403 Forbidden with message "Tenant context mismatch"
7. THE User_Service SHALL include tenant_id in both access and refresh token responses for client reference

### Requirement 7: Tenant Isolation Validation

**User Story:** As a security engineer, I want strict tenant isolation enforced via membership, so that cross-tenant data access is prevented.

#### Acceptance Criteria

1. THE User_Service SHALL resolve `TenantMembership` for `(userId, TenantContext.getCurrentTenant())` on every tenant-scoped operation via `TenantMembershipMapper`
2. IF no active membership exists, THE GlobalExceptionHandler SHALL return 403 Forbidden with ProblemDetail
3. WHEN `MembershipNotFoundException` is thrown, THE GlobalExceptionHandler SHALL return 403 Forbidden with ProblemDetail
4. THE User_Service SHALL load authorities from `TenantMemberAuthority` for the resolved membership via `TenantMemberAuthorityMapper.findAuthorityValuesByMembershipId(membershipId)`
5. THE User_Service SHALL never expose membership data of other tenants in error messages or API responses

### Requirement 8: User Registration

**User Story:** As a new user, I want to register an account and create a new tenant, so that I can access the system.

#### Acceptance Criteria

1. WHEN a registration request is received with valid email, password, and `tenantName`, THE User_Service SHALL:
   a. Upsert the `User` atomically via `UserMapper.upsertByEmail(user)` — a single `INSERT ... ON CONFLICT (email) DO NOTHING` statement; after the upsert, load the user via `UserMapper.findByEmail(email)` to obtain the canonical record regardless of whether it was just created or already existed; this eliminates the TOCTOU race condition that would occur with a separate existence-check followed by an insert
   b. Check tenant name uniqueness atomically via `TenantMapper.insertIfAbsent(tenant)` — a single `INSERT ... ON CONFLICT (name) DO NOTHING` statement; if zero rows were inserted the name was already taken, return 409 Conflict with message "Tenant name already taken"; joining an existing tenant requires an invitation (out of scope for this implementation)
   c. Create a `TenantMembership` for `(userId, tenantKey)` with authority `TENANT_OWNER`
2. THE User_Service SHALL resolve the tenant from the `tenantName` field in the signup request body — `X-Tenant-ID` header is NOT required for signup
3. IF a `TenantMembership` already exists for `(userId, tenantKey)`, THEN THE User_Service SHALL return 409 Conflict with message "User is already a member of this tenant"
4. WHEN a user account is created, THE User_Service SHALL hash the password using BCrypt with strength 12
5. THE User_Service SHALL validate that email format conforms to RFC 5322 standard
6. THE User_Service SHALL validate that password length is between 8 and 128 characters
7. THE User_Service SHALL validate that password contains at least one uppercase letter, one lowercase letter, one digit, and one special character
8. WHEN registration succeeds, THE User_Service SHALL return 201 Created with user ID, email, tenantKey, and tenantStatus (password excluded)
9. THE User_Service SHALL expose `POST /api/v1/iam/auth/signup` endpoint for user registration (returns 201 Created)

### Requirement 9: Account Lockout

**User Story:** As a security engineer, I want failed login attempts tracked and accounts temporarily locked, so that brute-force attacks are mitigated.

#### Acceptance Criteria

1. THE User_Service SHALL track failed login attempts per email address in a `failed_login_attempts` table (id UUID PK, email VARCHAR 255, attempted_at TIMESTAMP)
2. WHEN a login attempt fails, THE Authentication_Manager SHALL record the attempt via `AccountLockoutManager.recordFailedAttempt(email)` which delegates to `FailedLoginAttemptMapper.insert(attempt)`
3. WHEN the number of failed attempts within the lockout window reaches the configured threshold, THE Authentication_Manager SHALL reject further login attempts with 403 Forbidden and message "Account temporarily locked"
4. THE lockout threshold and window duration SHALL be configurable via `iqscaffold.auth.security.rate-limiting.login-attempts` and `lockout-duration`
5. WHEN a login attempt succeeds, THE Authentication_Manager SHALL reset the failed attempt counter via `AccountLockoutManager.reset(email)` which delegates to `FailedLoginAttemptMapper.deleteByEmail(email)`
6. THE User_Service SHALL expose `AccountLockoutManager` as a Spring component with `recordFailedAttempt(String email)`, `isLocked(String email)`, and `reset(String email)` methods
7. THE `failed_login_attempts` table SHALL be created via a Liquibase system migration with index `idx_failed_login_attempts_email`

### Requirement 10: User Authentication (Sign In)

**User Story:** As a verified user, I want to sign in with my email and password within a tenant context, so that I can receive JWT tokens with my tenant-specific authorities.

#### Acceptance Criteria

1. WHEN valid credentials are provided for an ACTIVE user, THE Authentication_Manager SHALL resolve the `TenantMembership` for `(userId, TenantContext.getCurrentTenant())` and return tokens with membership authorities
2. THE User_Service SHALL resolve tenant context from `X-Tenant-ID` header before authentication
3. THE Authentication_Manager SHALL validate credentials against the global `User` record via `UserMapper.findByEmail(email)`
4. IF the tenant status is `SUSPENDED`, THEN THE Authentication_Manager SHALL return 403 Forbidden with message "Tenant suspended"
5. IF the tenant status is `DELETED` or `PROVISIONING_FAILED`, THEN THE Authentication_Manager SHALL return 403 Forbidden with message "Tenant not available"
6. IF no active `TenantMembership` exists for `(userId, tenantKey)`, THEN THE Authentication_Manager SHALL return 403 Forbidden with message "User is not a member of this tenant"
7. WHEN valid credentials are provided, THE JwtTokenGenerator SHALL create an RS256-signed access token with 15-minute expiration containing: `sub`, `iss`, `iat`, `exp`, `jti`, `type=access`, `userId`, `username`, `email`, `firstName`, `lastName`, `tenant_id`, `email_verified`, `authorities`
8. WHEN valid credentials are provided, THE JwtTokenGenerator SHALL create an RS256-signed refresh token with 7-day expiration containing: `sub`, `iss`, `iat`, `exp`, `jti`, `type=refresh`, `username`, `tenant_id`
9. All JWT claim names SHALL use `JwtClaimNames` constants (never raw strings)
10. THE User_Service SHALL expose `POST /api/v1/iam/auth/signin` endpoint (returns 200 OK with `accessToken`, `refreshToken`, `tenantKey`)
11. THE User_Service SHALL expose `POST /api/v1/iam/users/tenants` endpoint for tenant discovery (returns 200 OK; see Requirement 13)

### Requirement 11: Token Refresh

**User Story:** As an authenticated user, I want to refresh my access token using my refresh token, so that I can maintain my session without re-entering credentials.

#### Acceptance Criteria

1. WHEN a valid refresh token is provided, THE JwtTokenGenerator SHALL issue a new access token (15-min) and a new refresh token (7-day), both with `tenant_id` claim
2. THE JwtTokenGenerator SHALL validate that `tenant_id` from the refresh token matches `TenantContext.getCurrentTenant()`; return 403 with "Tenant context mismatch" on mismatch
3. THE JwtTokenGenerator SHALL validate that the token `type` claim equals `"refresh"`; return 401 if not
4. IF the refresh token is expired, THE JwtTokenGenerator SHALL return 401 with "Refresh token expired"
5. IF the refresh token signature is invalid, THE JwtTokenGenerator SHALL return 401 with "Invalid token signature"
6. IF the user or tenant is SUSPENDED/DELETED, THE JwtTokenGenerator SHALL return 403 Forbidden
7. THE User_Service SHALL expose `POST /api/v1/iam/auth/refresh` endpoint (returns 200 OK with new token pair)

### Requirement 12: Signout and Token Revocation

**User Story:** As an authenticated user, I want to sign out and revoke my tokens, so that my session is terminated securely.

#### Acceptance Criteria

1. WHEN a user signs out, THE User_Service SHALL add the current token's JTI to the `token_denylist` table via `TokenDenylistService.denyToken(jti, userId, expiresAt)`
2. WHEN a user signs out from all sessions, THE User_Service SHALL:
   a. Record `last_global_signout_at = NOW()` on the `users` record via `UserMapper.updateLastGlobalSignoutAt(userId, Instant.now())`
   b. Also add the current token's JTI to the denylist (so the calling token is immediately invalid too)
   c. Any JWT where `iat ≤ last_global_signout_at` SHALL be treated as revoked, regardless of JTI
3. ON every authenticated request, THE `JwtAuthenticationFilter` SHALL:
   a. Check `TokenDenylistService.isRevoked(jti)` — return 401 if the JTI is explicitly revoked
   b. Check `UserMapper.findLastGlobalSignoutAt(userId)` — return 401 if `iat ≤ last_global_signout_at`
   c. Return `{"title":"Token revoked","status":401}` for either condition
4. THE `JwtAuthenticationFilter` SHALL be registered before `BearerTokenAuthenticationFilter` in the `SecurityFilterChain`
5. THE `TokenDenylistService` SHALL run `@Scheduled(cron = "0 0 * * * *")` cleanup that deletes entries where `expires_at < NOW()`
6. THE cleanup job SHALL be guarded by ShedLock (`@SchedulerLock`) so that only one pod executes the deletion at a time in a horizontally scaled deployment; the lock SHALL be held for at most 55 minutes (`lockAtMostFor = "PT55M"`) and released after 5 minutes minimum (`lockAtLeastFor = "PT5M"`)
7. THE User_Service SHALL expose `POST /api/v1/iam/auth/signout` endpoint (returns 204 No Content)
8. THE User_Service SHALL expose `POST /api/v1/iam/auth/signout-all` endpoint (returns 204 No Content)
9. THE User_Service SHALL expose `POST /api/v1/iam/auth/validate` endpoint to validate a JWT and return decoded user context for gateway introspection (returns 200 OK with `userId`, `email`, `tenantId`, `authorities`)

### Requirement 13: Tenant Discovery

**User Story:** As a user who has forgotten which tenant to sign in to, I want to look up my active tenant memberships using my credentials, so that I can select the correct tenant before signing in.

#### Acceptance Criteria

1. THE User_Service SHALL expose `POST /api/v1/iam/users/tenants` as a public endpoint (no `X-Tenant-ID` header required, no JWT required)
2. WHEN a request is received with valid email and password, THE Authentication_Manager SHALL validate credentials against the global `User` record via `UserMapper.findByEmail(email)` — the same lockout rules apply (Req 9)
3. IF credentials are invalid, THE Authentication_Manager SHALL return 401 Unauthorized (same response as failed sign-in; do not reveal whether the email exists)
4. WHEN credentials are valid, THE Authentication_Manager SHALL return 200 OK with the list of tenants the user is an active member of, via `TenantMembershipMapper.findByUserId(userId)` joined with tenant details — only memberships with status `ACTIVE` and tenant status `ACTIVE` are included
5. THE response SHALL contain for each tenant: `tenantKey`, `tenantName`, `membershipStatus`, `authorities` — sufficient for the client to display a tenant picker and then call `POST /api/v1/iam/auth/signin` with the chosen `X-Tenant-ID`
6. THE endpoint SHALL NOT issue any JWT token — it is a credential-gated lookup only
7. THE `TenantExtractionFilter` SHALL skip `POST /api/v1/iam/users/tenants` (no tenant context needed)

### Requirement 15: User Profile Retrieval (Multi-Tenant)

**User Story:** As an authenticated user, I want to retrieve my profile information, so that I can view my account details.

#### Acceptance Criteria

1. WHEN an authenticated user requests their profile, THE User_Service SHALL return user ID, email, firstName, lastName, createdAt, and account status from the public schema via `UserMapper.findById(userId)` (validated via TenantMembership for the current tenant)
2. THE User_Service SHALL extract user ID from the JWT access token sub claim
3. THE User_Service SHALL extract tenant_id from the JWT access token and validate it matches TenantContext.getCurrentTenant()
4. THE User_Service SHALL never return password hash in any response
5. IF no active TenantMembership exists for the user in the current tenant, THEN THE User_Service SHALL return 404 Not Found
6. WHEN profile retrieval succeeds, THE User_Service SHALL return 200 OK with user profile data
7. THE User_Service SHALL expose `GET /api/v1/iam/users/me` endpoint (returns 200 OK)

### Requirement 16: User Profile Update (Multi-Tenant)

**User Story:** As an authenticated user, I want to update my profile information, so that I can keep my account details current.

#### Acceptance Criteria

1. WHEN an authenticated user updates their profile, THE User_Service SHALL update firstName and lastName in the public schema via `UserMapper.update(user)` (validated via TenantMembership for the current tenant)
2. THE User_Service SHALL extract user ID from the JWT access token sub claim
3. THE User_Service SHALL extract tenant_id from the JWT access token and validate it matches TenantContext.getCurrentTenant()
4. THE User_Service SHALL validate that firstName and lastName do not exceed 100 characters
5. THE User_Service SHALL set updatedBy field to the user ID from the JWT token
6. THE User_Service SHALL set updatedAt field to the current timestamp in the service layer before calling `UserMapper.update(user)`
7. WHEN profile update succeeds, THE User_Service SHALL return 200 OK with updated user profile
8. THE User_Service SHALL expose `PATCH /api/v1/iam/users/me` endpoint to update firstName and lastName (returns 200 OK)

### Requirement 17: User Deletion (Multi-Tenant)

**User Story:** As an authenticated user, I want to delete my account, so that I can remove my data from the system.

#### Acceptance Criteria

1. WHEN an authenticated user requests account deletion, THE User_Service SHALL remove the user's TenantMembership (and cascaded TenantMemberAuthority records) for the current tenant via `TenantMembershipMapper.deleteById(membershipId)`
2. THE User_Service SHALL extract tenant_id from JWT and validate it matches TenantContext.getCurrentTenant()
3. WHEN account deletion succeeds, THE User_Service SHALL return 204 No Content

### Requirement 18: Email Verification

**User Story:** As a new user, I want to verify my email address after signup, so that the system can confirm I own the address and unlock full account capabilities.

#### Acceptance Criteria

1. WHEN a user registers, THE User_Service SHALL generate a cryptographically random 64-character hex token via `SecureRandom` (32 bytes → hex), store it in `email_verification_tokens` with a 24-hour expiry, and publish a `NotificationEvent` of type `VERIFY_EMAIL` containing the token; the user record is created with `email_verified = false`
2. THE User_Service SHALL expose `POST /api/v1/iam/users/email/verify` as a public endpoint (no `X-Tenant-ID`, no JWT required); request body: `{ "token": "<hex-token>" }`
3. WHEN a valid, non-expired token is received, THE User_Service SHALL set `users.email_verified = true` via `UserMapper.setEmailVerified(userId)`, delete the token record, and return 200 OK
4. WHEN an invalid or expired token is received, THE User_Service SHALL return 400 Bad Request with message "Invalid or expired verification token"; expired tokens SHALL NOT reveal whether the email exists
5. THE User_Service SHALL expose `POST /api/v1/iam/users/email/resend-verification` as a public endpoint; request body: `{ "email": "<address>" }`; response is always 202 Accepted regardless of whether the email exists (prevents enumeration)
6. WHEN a resend request is received for an already-verified email, THE User_Service SHALL silently return 202 Accepted without sending an email
7. WHEN a resend request is received, THE User_Service SHALL enforce a rate limit of 3 resend attempts per email per hour via `email_verification_tokens.resend_count` and `last_resend_at`; if the limit is exceeded, return 429 Too Many Requests with `Retry-After` header
8. WHEN a resend is allowed, THE User_Service SHALL invalidate all existing tokens for that user, generate a new token, and publish a new `NotificationEvent(VERIFY_EMAIL)`
9. THE `email_verified` field SHALL be included as a claim in the JWT access token via `JwtClaimNames.EMAIL_VERIFIED`
10. THE User_Service SHALL gate the following operations behind `email_verified = true`: tenant status changes (`PATCH /api/v1/iam/tenants/{tenantKey}/status`), retry-provisioning (`POST /api/v1/iam/tenants/{tenantKey}/retry-provisioning`); return 403 Forbidden with message "Email address not verified" if the claim is false
11. THE `email_verification_tokens` table SHALL be cleaned up by a `@Scheduled` job `ExpiredVerificationTokenReaperJob` running hourly, guarded by ShedLock (`lockAtMostFor = "PT55M"`, `lockAtLeastFor = "PT5M"`), deleting rows where `expires_at < NOW()`
12. THE `TenantExtractionFilter` SHALL skip `POST /api/v1/iam/users/email/verify` and `POST /api/v1/iam/users/email/resend-verification`

### Requirement 19: Database Schema and Migrations

**User Story:** As a developer, I want database schema managed through Liquibase migrations targeting the public schema only, so that schema changes are versioned and reproducible.

#### Acceptance Criteria

1. THE User_Service SHALL use Liquibase for all database schema changes with a single system changelog targeting the public schema
2. THE User_Service SHALL create the following tables in the public schema:
    - `tenants` (id UUID PK, tenant_key VARCHAR 12 UNIQUE NOT NULL, name VARCHAR 100 UNIQUE NOT NULL, status VARCHAR 20, created_at, updated_at, created_by, updated_by)
    - `users` (id UUID PK, email VARCHAR 255 UNIQUE NOT NULL, password_hash VARCHAR 255, first_name VARCHAR 100, last_name VARCHAR 100, status VARCHAR 50, last_global_signout_at TIMESTAMP NULL, created_at, updated_at, created_by, updated_by) — no tenant_id column
    - `tenant_memberships` (id UUID PK, user_id UUID NOT NULL FK → users.id ON DELETE CASCADE, tenant_key VARCHAR 12 NOT NULL FK → tenants.tenant_key ON DELETE CASCADE, status VARCHAR 50 NOT NULL, created_at, updated_at, created_by, updated_by); UNIQUE constraint on (user_id, tenant_key)
    - `tenant_member_authorities` (id UUID PK, membership_id UUID NOT NULL FK → tenant_memberships.id ON DELETE CASCADE, authority VARCHAR 50 NOT NULL)
    - `token_denylist` (id UUID PK, jti VARCHAR 255 UNIQUE NOT NULL, user_id UUID NOT NULL FK → users.id ON DELETE CASCADE, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL)
    - `failed_login_attempts` (id UUID PK, email VARCHAR 255 NOT NULL, attempted_at TIMESTAMP NOT NULL)
3. THE User*Service SHALL maintain per-tenant migration changelogs in `db/changelog/tenant/`; for MVP these contain zero changesets — the tenant schema `t*{tenantKey}` is created and Liquibase tracking tables are initialized, but no application tables are created there yet
4. THE User_Service SHALL create indexes: idx_tenants_tenant_key, idx_tenants_name (UNIQUE), idx_users_email, idx_tenant_memberships_user_id, idx_tenant_memberships_tenant_key, idx_tenant_member_authorities_membership_id, idx_token_denylist_jti, idx_token_denylist_user_id, idx_failed_login_attempts_email
5. ALL Liquibase changeset files SHALL follow naming convention YYYYMMDDhhmmss-description.xml
6. ALL Liquibase changesets SHALL have author set to "iqscaffold"
7. THE User_Service SHALL organize all migrations in db/changelog/system/ for the public schema

### Requirement 20: Coding Standards Compliance (Multi-Tenant)

**User Story:** As a developer, I want the codebase to follow IQ Scaffold Platform coding guidelines with multi-tenancy support, so that the code is consistent, maintainable, and follows best practices.

#### Acceptance Criteria

1. THE User_Service SHALL organize code by feature/domain in vertical slices (tenant, user, authentication, etc.)
2. THE User_Service SHALL place multi-tenancy infrastructure in tenancy/ package including `MyBatisSchemaInterceptor` and `TenantContext`
3. THE User_Service SHALL use Java records for all DTOs with validation annotations
4. THE User_Service SHALL group all DTOs for each domain in a single {Entity}Dtos.java container class
5. THE User_Service SHALL use manual DTO mappers as final utility classes with static methods only
6. THE User_Service SHALL use interface + implementation pattern for all services
7. THE User_Service SHALL apply @Transactional at class level and @Transactional(readOnly = true) on read-only methods
8. THE User_Service SHALL use constructor injection with final fields for all dependencies
9. THE User_Service SHALL use @PreAuthorize on all controller endpoints with explicit authority lists
10. THE User_Service SHALL use ProblemDetail (RFC 9457) for all error responses
11. THE User_Service SHALL generate UUIDs in the service layer before calling mapper insert methods (no ORM-managed ID generation)
12. THE User_Service SHALL set createdAt and updatedAt timestamps in the service layer before calling mapper insert/update methods
13. THE User_Service SHALL pass enum values as strings to MyBatis mappers; MyBatis EnumTypeHandler handles the conversion
14. THE User_Service SHALL use JwtClaimNames constants for all JWT claim access including TENANT_ID (never raw strings)
15. THE User_Service SHALL use RS256 algorithm for JWT signing (never HS256)
16. THE User_Service SHALL use SLF4J for all logging (never System.out.println)
17. THE User_Service SHALL use Java 21+ features including records, pattern matching, switch expressions, and text blocks where appropriate
18. THE User_Service SHALL ensure `User` is a global identity with no `tenant_id` field; tenant scoping is done via `TenantMembership`
19. THE User_Service SHALL resolve `TenantMembership` for `(userId, tenantKey)` on every tenant-scoped operation; never bypass membership validation
20. THE User_Service SHALL implement `MyBatisSchemaInterceptor` in tenancy/ package and register it in `MyBatisConfig`
21. THE User_Service SHALL configure `mybatis.mapper-locations` and `map-underscore-to-camel-case=true` in application.yml
22. THE User_Service SHALL NOT include any `spring.jpa.*` or `hibernate.*` properties in application.yml
23. THE User_Service SHALL load authorities from `TenantMemberAuthority` for the resolved membership when generating JWT tokens via `TenantMemberAuthorityMapper.findAuthorityValuesByMembershipId(membershipId)`
24. ALL MyBatis SQL SHALL reside in XML mapper files under `src/main/resources/mappers/`; no inline `@Select`/`@Insert`/`@Update`/`@Delete` annotations on mapper interfaces
25. THE User_Service SHALL use ShedLock with the JDBC store (`shedlock-provider-jdbc-template`) to prevent concurrent execution of `@Scheduled` jobs across multiple pods; the `shedlock` table SHALL be created via a Liquibase system migration
26. THE User_Service SHALL implement `StuckTenantReaperJob` as a `@Component` with a `@Scheduled(cron = "0 */5 * * * *")` `@SchedulerLock(name = "StuckTenantReaperJob.reapStuckTenants", lockAtMostFor = "PT4M", lockAtLeastFor = "PT1M")` method; the provisioning timeout SHALL be configurable via `iqscaffold.tenancy.provisioning-timeout` (default `PT10M`)

### Requirement 21: API Documentation (Multi-Tenant)

**User Story:** As an API consumer, I want comprehensive API documentation including multi-tenancy requirements, so that I can understand how to integrate with the service.

#### Acceptance Criteria

1. THE User_Service SHALL use SpringDoc OpenAPI for API documentation generation
2. THE User_Service SHALL annotate all REST endpoints with @Operation, @ApiResponses, and @Tag
3. THE User_Service SHALL document all request and response schemas with descriptions
4. THE User_Service SHALL expose Swagger UI at /swagger-ui.html
5. THE User_Service SHALL expose OpenAPI JSON specification at /api-docs
6. THE User_Service SHALL mark all authenticated endpoints with @SecurityRequirement(name = "bearerAuth")
7. THE User_Service SHALL document all error responses with appropriate HTTP status codes and ProblemDetail schema
8. THE User_Service SHALL document X-Tenant-ID header as required on all tenant-scoped endpoints
9. THE User_Service SHALL document tenant_id claim in JWT token structure

### Requirement 22: Token Denylist and Revocation

**User Story:** As a security engineer, I want issued tokens to be revocable, so that signout is effective and compromised tokens can be invalidated.

#### Acceptance Criteria

1. THE User_Service SHALL maintain a `token_denylist` table storing revoked JWT JTIs with their expiry timestamps
2. WHEN a user signs out, THE User_Service SHALL insert the current token's JTI and expiresAt into the denylist via `TokenDenylistMapper.insert(entry)`
3. WHEN a user signs out from all sessions, THE User_Service SHALL set `last_global_signout_at = NOW()` on the user record and also add the current token's JTI to the denylist; any token with `iat ≤ last_global_signout_at` is treated as revoked by `JwtAuthenticationFilter`
4. ON every authenticated request, THE `JwtAuthenticationFilter` SHALL call `TokenDenylistService.isRevoked(jti)` and return 401 Unauthorized if the token is revoked
5. THE `TokenDenylistService` SHALL expose a `@Scheduled(cron = "0 0 * * * *")` `cleanupExpired()` method that calls `TokenDenylistMapper.deleteByExpiresAtBefore(Instant.now())` to purge expired entries hourly
6. THE `JwtAuthenticationFilter` SHALL be registered before `BearerTokenAuthenticationFilter` in the `SecurityFilterChain`

### Requirement 23: Observability

**User Story:** As an operator, I want metrics, health checks, and structured logging, so that I can monitor the service in production.

#### Acceptance Criteria

1. THE User_Service SHALL expose health, info, metrics, and prometheus endpoints on management port 8081
2. THE User_Service SHALL enable Kubernetes liveness and readiness probes via `management.endpoint.health.probes.enabled: true`
3. THE User_Service SHALL use structured JSON logging via `logstash-logback-encoder`
4. THE User_Service SHALL include correlationId in all log entries via MDC (set by `CorrelationIdFilter`)
5. THE User_Service SHALL expose Prometheus metrics via `micrometer-registry-prometheus`
6. THE User_Service SHALL track `auth.success`, `auth.failure` (with tenantId and reason tags), and `tenant.created` counters
7. THE User_Service SHALL track `auth.duration` timer with management port 8081
8. THE User_Service SHALL restrict actuator endpoints to `health,info,metrics,prometheus` in the production profile

### Requirement 24: Forgot-Password Endpoint

**User Story:** As a user who has forgotten their password, I want to request a password reset link via my email address, so that I can regain access to my account without contacting support.

#### Acceptance Criteria

1. THE IAM_Service SHALL expose `POST /api/v1/iam/users/password/forgot` as a public endpoint that requires no `X-Tenant-ID` header and no JWT token
2. WHEN a forgot-password request is received with a valid email address, THE Password_Reset_Service SHALL look up the global `User` record via `UserMapper.findByEmail(email)`
3. IF no `User` record exists for the provided email, THE IAM_Service SHALL return 200 OK with a generic message — the response SHALL be identical to the success response to prevent email enumeration
4. WHEN a valid `User` record is found, THE Password_Reset_Service SHALL generate a `Password_Reset_Token` using `SecureRandom` (32 bytes, hex-encoded to 64 characters) and persist it via `PasswordResetTokenMapper.insert(token)` with `expires_at = NOW() + 1 hour`
5. WHEN a new `Password_Reset_Token` is created, THE Password_Reset_Service SHALL invalidate any previously existing tokens for the same user via `PasswordResetTokenMapper.deleteByUserId(userId)` before inserting the new token
6. WHEN a `Password_Reset_Token` is persisted, THE Password_Reset_Service SHALL publish a `NotificationEvent` with type `PASSWORD_RESET_INITIATED` and the reset token value to the `iqscaffold.events` exchange via RabbitMQ
7. THE IAM_Service SHALL return 200 OK with message `"If an account with that email exists, a password reset link has been sent"` regardless of whether the email was found
8. THE `TenantExtractionFilter` SHALL skip `POST /api/v1/iam/users/password/forgot` (no tenant context required)
9. WHEN a forgot-password request is received, THE Password_Reset_Service SHALL enforce a rate limit of at most 3 requests per email address within a configurable sliding window (default: 15 minutes, property: `iqscaffold.auth.password-reset.rate-limit-window`)
10. IF the rate limit is exceeded, THE IAM_Service SHALL return 429 Too Many Requests with message `"Too many password reset requests. Please try again later."`

### Requirement 25: Reset-Password Endpoint

**User Story:** As a user who has received a password reset email, I want to submit my new password using the one-time token from the email, so that I can regain access to my account.

#### Acceptance Criteria

1. THE IAM_Service SHALL expose `POST /api/v1/iam/users/password/reset` as a public endpoint that requires no `X-Tenant-ID` header and no JWT token
2. WHEN a reset-password request is received, THE Password_Reset_Service SHALL look up the `Password_Reset_Token` record via `PasswordResetTokenMapper.findByToken(token)`
3. IF no matching `Password_Reset_Token` record is found, THE IAM_Service SHALL return 400 Bad Request with message `"Invalid or expired password reset token"`
4. IF the `Password_Reset_Token` record exists but `expires_at` is before `NOW()`, THE IAM_Service SHALL return 400 Bad Request with message `"Invalid or expired password reset token"` — the response SHALL be identical to the not-found case to prevent token oracle attacks
5. WHEN the token is valid, THE Password_Reset_Service SHALL validate the new password: length between 8 and 128 characters, at least one uppercase letter, one lowercase letter, one digit, and one special character — the same rules as user registration (Requirement 8)
6. IF the new password fails validation, THE IAM_Service SHALL return 400 Bad Request with a descriptive validation error message
7. WHEN the new password passes validation, THE Password_Reset_Service SHALL hash the new password using BCrypt with strength 12 and update the `users` record via `UserMapper.updatePassword(userId, newPasswordHash)`
8. WHEN the password is updated, THE Password_Reset_Service SHALL delete the consumed token via `PasswordResetTokenMapper.deleteByToken(token)` to ensure single-use semantics
9. WHEN the password is updated, THE Password_Reset_Service SHALL invalidate all active sessions by updating `users.last_global_signout_at = NOW()` via `UserMapper.updateLastGlobalSignoutAt(userId, Instant.now())` — this causes `JwtAuthenticationFilter` to reject all previously issued tokens
10. WHEN the password is updated, THE Password_Reset_Service SHALL publish a `NotificationEvent` with type `PASSWORD_RESET_CONFIRMED` to the `iqscaffold.events` exchange via RabbitMQ
11. WHEN the password reset completes successfully, THE IAM_Service SHALL return 200 OK with message `"Password has been reset successfully"`
12. THE `TenantExtractionFilter` SHALL skip `POST /api/v1/iam/users/password/reset` (no tenant context required)

### Requirement 26: Password Reset Token Persistence

**User Story:** As a developer, I want password reset tokens stored securely in the database with automatic expiry, so that the reset flow is reliable and tokens cannot be reused.

#### Acceptance Criteria

1. THE IAM_Service SHALL create a `password_reset_tokens` table via a Liquibase system migration with columns: `id UUID PRIMARY KEY`, `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`, `token VARCHAR(64) NOT NULL UNIQUE`, `expires_at TIMESTAMP NOT NULL`, `created_at TIMESTAMP NOT NULL`
2. THE IAM_Service SHALL create an index `idx_password_reset_tokens_user_id` on `password_reset_tokens(user_id)` and an index `idx_password_reset_tokens_token` on `password_reset_tokens(token)` via the same Liquibase migration
3. THE `PasswordResetTokenMapper` SHALL expose: `insert(PasswordResetToken)`, `findByToken(String): Optional<PasswordResetToken>`, `deleteByToken(String)`, `deleteByUserId(UUID)`, `deleteByExpiresAtBefore(Instant)`, and `countByUserIdAndCreatedAtAfter(UUID userId, Instant since): int`
4. THE IAM_Service SHALL run a `@Scheduled(cron = "0 0 * * * *")` ShedLock-guarded cleanup job (`ExpiredPasswordResetTokenReaperJob`) that deletes records where `expires_at < NOW()` via `PasswordResetTokenMapper.deleteByExpiresAtBefore(Instant.now())`; the lock SHALL be held for at most 55 minutes (`lockAtMostFor = "PT55M"`) and at least 5 minutes (`lockAtLeastFor = "PT5M"`)
5. THE `Password_Reset_Token` domain object SHALL be a plain Java class (no JPA/Hibernate annotations) with fields: `UUID id`, `UUID userId`, `String token`, `Instant expiresAt`, `Instant createdAt`

### Requirement 27: Password Reset Email Notifications

**User Story:** As a user, I want to receive a clear email when I request a password reset and a confirmation email when my password is changed, so that I am informed about security-sensitive actions on my account.

#### Acceptance Criteria

1. WHEN a `NotificationEvent` with type `PASSWORD_RESET_INITIATED` is consumed from the RabbitMQ queue, THE NotificationConsumer SHALL render the `Initiate_Template` (`password-reset/initiate.html`) using Thymeleaf and send the email via `JavaMailSender`
2. THE `Initiate_Template` SHALL include: the user's first name, a password reset link constructed as `{baseUrl}/reset-password?token={token}`, and an expiry notice stating the link is valid for 1 hour
3. WHEN a `NotificationEvent` with type `PASSWORD_RESET_CONFIRMED` is consumed from the RabbitMQ queue, THE NotificationConsumer SHALL render the `Confirmed_Template` (`password-reset/confirmed.html`) using Thymeleaf and send the email via `JavaMailSender`
4. THE `Confirmed_Template` SHALL include: the user's first name and a security notice advising the user to contact support if they did not initiate the password change
5. THE `NotificationEventType` enum SHALL be extended with values `PASSWORD_RESET_INITIATED` and `PASSWORD_RESET_CONFIRMED`
6. THE email subject lines SHALL be resolved from `messages*.properties` i18n resource bundles using keys `email.password-reset.initiate.subject` and `email.password-reset.confirmed.subject`
7. THE `Initiate_Template` and `Confirmed_Template` SHALL be provided in all existing i18n locales (en, es, it, ru) via the `messages*.properties` resource bundles
8. IF the RabbitMQ publish of a `NotificationEvent` fails, THE Password_Reset_Service SHALL log the error and still return the appropriate HTTP response to the caller — email delivery failure SHALL NOT cause the HTTP request to fail
9. THE `NotificationEvent` payload for `PASSWORD_RESET_INITIATED` SHALL include: `userId`, `email`, `firstName`, `token`, and `baseUrl` (resolved from `iqscaffold.notification.base-url` configuration property via `NotificationConfigurationProperties.baseUrl()`)

### Requirement 28: Password Reset Security Constraints

**User Story:** As a security engineer, I want the password reset flow to be resistant to common attacks, so that the feature does not introduce new vulnerabilities into the IAM service.

#### Acceptance Criteria

1. THE `Password_Reset_Token` value SHALL be generated using `SecureRandom` (32 bytes, hex-encoded) — the same generation strategy as `EmailVerificationToken`
2. THE IAM_Service SHALL return identical HTTP responses for "email not found" and "email found" cases in the forgot-password flow to prevent email enumeration (Requirement 24, criteria 3 and 7)
3. THE IAM_Service SHALL return identical HTTP responses for "token not found" and "token expired" cases in the reset-password flow to prevent token oracle attacks (Requirement 25, criteria 3 and 4)
4. WHEN a password reset is completed, THE Password_Reset_Service SHALL invalidate all existing sessions via `UserMapper.updateLastGlobalSignoutAt` (Requirement 25, criterion 9) — this is a MUST to prevent session fixation after a credential change
5. THE `password_reset_tokens` table SHALL enforce a UNIQUE constraint on the `token` column to prevent token collision
6. THE `Password_Reset_Token` SHALL have a maximum lifetime of 1 hour (`expires_at = NOW() + PT1H`) — configurable via `iqscaffold.auth.password-reset.token-ttl` (default `PT1H`)
7. WHEN a new `Password_Reset_Token` is requested for a user who already has an active token, THE Password_Reset_Service SHALL delete the existing token before issuing a new one — only one active token per user is permitted at any time
8. THE forgot-password and reset-password endpoints SHALL be included in the `TenantExtractionFilter` skip list so that no tenant context is required for these public endpoints
