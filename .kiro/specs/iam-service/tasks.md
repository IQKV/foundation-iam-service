# Implementation Plan: Identity and Access Management

## Overview

Implement a multi-tenant Spring Boot 3.x / Java 21 authentication microservice with MyBatis 3.x for persistence, schema-per-tenant routing via `MyBatisSchemaInterceptor`, async tenant provisioning via RabbitMQ, and JWT RS256 tokens with `tenant_id` claim.

## Tasks

- [x] 1. Project scaffold and build configuration
  - [x] 1.1 Create Maven `pom.xml`
    - Spring Boot 3.x parent, Java 21, packaging jar
    - Dependencies: `spring-boot-starter-web`, `mybatis-spring-boot-starter`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-amqp`, `spring-boot-starter-actuator`, `spring-boot-starter-validation`, `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`
    - `postgresql` driver, `liquibase-core`
    - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (RS256)
    - `springdoc-openapi-starter-webmvc-ui` 2.x, `micrometer-registry-prometheus`, `logstash-logback-encoder`
    - `shedlock-spring` and `shedlock-provider-jdbc-template` (distributed lock for `@Scheduled` jobs)
    - Test: `testcontainers` (postgresql, rabbitmq), `spring-boot-starter-test`, `mybatis-spring-boot-starter-test`
    - NO `spring-boot-starter-data-jpa`, NO Hibernate BOM, NO JPA dependencies
    - _Requirements: 20.1_

  - [x] 1.2 Create `src/main/resources/application.yml` and profile configs
    - `application.yml`: `mybatis.mapper-locations: classpath:mappers/**/*.xml`; `mybatis.configuration.map-underscore-to-camel-case: true`; `spring.liquibase.enabled: false`; actuator on port 8081; JWT RS256 key paths + `expiry: PT15M` + `refresh-expiry: P7D` via env vars; `iqscaffold.tenancy.provisioning-timeout: PT10M`; `iqscaffold.notification.mail.from: ${MAIL_FROM:noreply@example.com}`; `iqscaffold.notification.mail.reply-to: ${MAIL_REPLY_TO:}`; `iqscaffold.notification.default-locale: en`; `iqscaffold.notification.base-url: ${APP_BASE_URL:http://localhost:3000}`; `iqscaffold.auth.password-reset.token-ttl: PT1H`; `iqscaffold.auth.password-reset.rate-limit-window: PT15M`; `iqscaffold.auth.password-reset.rate-limit-max-requests: 3`; `spring.mail.host: ${MAIL_HOST:localhost}`; `spring.mail.port: ${MAIL_PORT:587}`; `spring.mail.username: ${MAIL_USERNAME:}`; `spring.mail.password: ${MAIL_PASSWORD:}`; `spring.mail.properties.mail.smtp.auth: true`; `spring.mail.properties.mail.smtp.starttls.enable: true`; `spring.messages.basename: messages`; `spring.messages.encoding: UTF-8`; `spring.messages.fallback-to-system-locale: false`; all sensitive values via `${ENV_VAR:default}`
    - NO `spring.jpa.*` or `hibernate.*` properties anywhere
    - `application-local.yml`: relaxed password policy (`min-length: 6`), `liquibase.contexts: demo`, all actuator endpoints exposed, longer token TTLs (`PT30M`/`P30D`), `spring.mail.host: localhost`, `spring.mail.port: 1025` (Mailpit/MailHog), `spring.mail.properties.mail.smtp.auth: false`, `spring.mail.properties.mail.smtp.starttls.enable: false`
    - `application-dev.yml`: K8s service DNS for infra hosts, `liquibase.contexts: demo`, debug logging
    - `application-production.yml`: all secrets from env vars (no defaults), Swagger disabled, `liquibase.contexts: !demo`, `server.shutdown: graceful`
    - `application-test.yml`: `liquibase.contexts: demo`, debug Liquibase logging, `spring.mail.host: localhost`, `spring.mail.port: 3025`
    - _Requirements: 2.4, 20.21-20.22_

  - [x] 1.3 Create `src/main/resources/logback-spring.xml`
    - Console appender with `LogstashEncoder` for structured JSON output

  - [x] 1.4 Create `IamServiceApplication.java` and configuration properties classes
    - `IamServiceApplication`: `@SpringBootApplication` main class with `@EnableScheduling` and `@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")`
    - `@ConfigurationProperties` records:
      - `DatabaseConfigurationProperties` (`iqscaffold.database`): url, username, password, pool size
      - `AuthConfigurationProperties` (`iqscaffold.auth`): jwt (private-key-path, public-key-path, expiry, refresh-expiry, issuer), security (password encoder-strength, min-length, rate-limiting login-attempts + lockout-duration), passwordReset (token-ttl `PT1H`, rate-limit-window `PT15M`, rate-limit-max-requests `3`)
      - `MessagingConfigurationProperties` (`iqscaffold.messaging.rabbitmq`): host, credentials, exchanges, queues, routing-keys, enabled flag
      - `LiquibaseConfigurationProperties` (`iqscaffold.liquibase`): system-change-log, tenant-change-log, contexts
      - `TenancyConfigurationProperties` (`iqscaffold.tenancy`): schema-prefix (`t_`), default-schema (`public`), provisioning-timeout (`PT10M`) — used by `StuckTenantReaperJob` to identify stuck tenants
      - `NotificationConfigurationProperties` (`iqscaffold.notification`): mail (from, reply-to), default-locale (`"en"`), base-url — used by `EmailService` for template rendering and link generation
    - _Requirements: 20.1_

- [x] 2. Multi-tenancy infrastructure
  - [x] 2.1 Implement `TenantContext` (ThreadLocal holder)
    - Static `setCurrentTenant(String)`, `getCurrentTenant()`, `clear()` methods
    - `setCurrentTenant` throws `IllegalArgumentException` on null/blank input
    - `getCurrentTenant()` throws `IllegalStateException("No tenant context set for current thread")` when empty
    - _Requirements: 3.7, 3.8, 3.9_

  - [x] 2.2 Implement `MyBatisSchemaInterceptor`
    - `@Intercepts({ @Signature(type=StatementHandler.class, method="prepare", args={Connection.class, Integer.class}) })`
    - In `intercept()`: call `TenantContext.getCurrentTenant()`; if present execute `SET search_path TO t_{tenantKey}, public` on the connection via a `Statement`; if `IllegalStateException` (no context) proceed without changing search_path
    - _Requirements: 2.2, 2.3, 2.5, 3.10, 3.11, 3.12_

  - [x] 2.3 Implement `MyBatisConfig`
    - `@Configuration @MapperScan("com.iqscaffold.iam")`
    - `SqlSessionFactory` bean: set `DataSource`, mapper locations (`classpath:mappers/**/*.xml`), `mapUnderscoreToCamelCase=true`, register `MyBatisSchemaInterceptor`
    - _Requirements: 2.1, 2.4_

  - [x] 2.4 Implement `TenantExtractionFilter`
    - `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`, extends `OncePerRequestFilter`
    - Priority 1: `X-Tenant-ID` header; Priority 2: JWT `tenant_id` claim — constructor-inject `JwtDecoder`, decode Bearer token, extract `JwtClaimNames.TENANT_ID` claim
    - Returns 400 `application/problem+json` `{"title":"Tenant ID required","status":400}` when unresolvable
    - Always calls `TenantContext.clear()` in `finally` block
    - `shouldNotFilter`: skips `/actuator/**`, `/api-docs/**`, `/swagger-ui/**`, `POST /api/v1/iam/auth/signup`, `POST /api/v1/iam/users/tenants`, `POST /api/v1/iam/users/email/verify`, `POST /api/v1/iam/users/email/resend-verification`, `POST /api/v1/iam/users/password/forgot`, and `POST /api/v1/iam/users/password/reset`
    - _Requirements: 3.1-3.6_

- [x] 3. Domain objects, enumerations, exceptions, and constants
  - [x] 3.1 Create enumerations
    - `TenantStatus`: `PROVISIONING`, `ACTIVE`, `SUSPENDED`, `DELETED`, `PROVISIONING_FAILED`
    - `AccountStatus`: `ACTIVE`
    - `MembershipStatus`: `ACTIVE`, `SUSPENDED`, `REMOVED`
    - _Requirements: 1.7, 4.2, 8.3_

  - [x] 3.2 Create plain Java domain objects (no JPA/Hibernate annotations)
    - `Tenant`: id (UUID), tenantKey, name, status (TenantStatus), createdAt, updatedAt, createdBy, updatedBy
    - `User`: id (UUID), email, passwordHash, firstName, lastName, status (AccountStatus), emailVerified (boolean, default false), lastGlobalSignoutAt (Instant, nullable), createdAt, updatedAt, createdBy, updatedBy — no tenant_id field
    - `EmailVerificationToken`: id (UUID), userId (UUID), token (String, 64-char hex), expiresAt (Instant), resendCount (int, default 0), lastResendAt (Instant, nullable), createdAt (Instant)
    - `TenantMembership`: id (UUID), userId (UUID), tenantKey, status (MembershipStatus), createdAt, updatedAt, createdBy, updatedBy
    - `TenantMemberAuthority`: id (UUID), membershipId (UUID), authority
    - `TokenDenylist`: id (UUID), jti, userId (UUID), expiresAt (Instant), createdAt (Instant)
    - `FailedLoginAttempt`: id (UUID), email, attemptedAt (Instant)
    - Plain getters/setters only — no `@Entity`, `@Table`, `@Column`, `@GeneratedValue`, or any JPA annotations
    - _Requirements: 4.1, 4.2, 4.7_

  - [x] 3.3 Create custom exceptions in `shared/exception/`
    - `UserNotFoundException`, `MembershipNotFoundException` (fields: userId, tenantKey), `EmailAlreadyRegisteredException`, `TenantSuspendedException`, `UserRegistrationException`, `UserManagementException`, `TenantContextMismatchException`
    - `TenantManagementException`: sealed class with permitted subtypes `TenantAlreadyExistsException`, `TenantNotFoundException`, `SchemaProvisioningException`, `InvalidTenantStateException`
    - `InvalidVerificationTokenException` (→ 400), `VerificationRateLimitException` (→ 429, carries `retryAfterSeconds: long`)
    - `TenantNotAvailableException` (→ 403) — thrown when tenant status is `DELETED` or `PROVISIONING_FAILED` during sign-in (req 10.4/10.5)
    - `AccountLockedException` (→ 403) — thrown by `AccountLockoutManager` when `isLocked()` returns true; message "Account temporarily locked"
    - `TenantMembershipAlreadyExistsException` (→ 409) — thrown when a `TenantMembership` already exists for `(userId, tenantKey)` during signup; message "User is already a member of this tenant"
    - `InvalidTokenTypeException` (→ 401) — thrown when the JWT `type` claim is not `"refresh"` during token refresh (req 11.3); message "Invalid token type"
    - `TokenExpiredException` (→ 401) — thrown for an expired refresh token (req 11.4); message "Refresh token expired"
    - `InvalidTokenSignatureException` (→ 401) — thrown for an invalid JWT signature (req 11.5); message "Invalid token signature"
    - `PasswordResetTokenNotFoundException` (→ 400) — thrown when a password reset token is not found or already consumed; message "Invalid or expired password reset token"
    - `PasswordResetRateLimitException` (→ 429, carries `retryAfterSeconds: long`) — thrown when the rate limit for password reset requests is exceeded
    - _Requirements: 4.4, 4.5_

  - [x] 3.4 Create `JwtClaimNames` and `UserServiceConstants`
    - `JwtClaimNames`: constants for `SUB`, `ISS`, `IAT`, `EXP`, `JTI`, `TYPE`, `USER_ID`, `USERNAME`, `EMAIL`, `FIRST_NAME`, `LAST_NAME`, `TENANT_ID`, `AUTHORITIES`, `EMAIL_VERIFIED`; token type values `TYPE_ACCESS="access"` / `TYPE_REFRESH="refresh"`; issuer constant `ISSUER="iqscaffold-iam-service"`
    - `UserServiceConstants`: service-wide string constants
    - _Requirements: 20.14_

- [x] 4. MyBatis mapper interfaces and XML mapper files
  - [x] 4.1 Implement `TenantMapper` interface and `src/main/resources/mappers/tenant/TenantMapper.xml`
    - Interface: `insertIfAbsent(Tenant)`, `findByTenantKey(String): Optional<Tenant>`, `findByStatus(String): List<Tenant>`, `existsByName(String): boolean`, `updateStatus(@Param tenantKey, @Param status)`, `findStuckProvisioning(@Param olderThan Instant): List<Tenant>`
    - XML: `resultMap` with all column mappings; `INSERT ... ON CONFLICT (name) DO NOTHING` for `insertIfAbsent`; `findStuckProvisioning` selects `WHERE status = 'PROVISIONING' AND created_at < #{olderThan}`; `SELECT`, `UPDATE` statements targeting `public.tenants`
    - _Requirements: 1.1, 1.4, 1.17, 5.3_

  - [x] 4.2 Implement `UserMapper` interface and `src/main/resources/mappers/user/UserMapper.xml`
    - Interface: `upsertByEmail(User)`, `findById(UUID): Optional<User>`, `findByEmail(String): Optional<User>`, `existsByEmail(String): boolean`, `update(User)`, `updateLastGlobalSignoutAt(@Param userId, @Param lastGlobalSignoutAt): void`, `findLastGlobalSignoutAt(@Param userId): Optional<Instant>`, `setEmailVerified(@Param userId UUID): void`
    - XML: `resultMap` mapping `email_verified` column to `emailVerified` field; `upsertByEmail` includes `email_verified` column (value `#{emailVerified}`, defaults to `false`); `setEmailVerified` issues `UPDATE public.users SET email_verified = true WHERE id = #{userId}`; all other SQL targeting `public.users`
    - _Requirements: 8.1, 10.3_

  - [x] 4.3 Implement `TenantMembershipMapper` interface and `TenantMembershipMapper.xml`
    - Interface: `insert(TenantMembership)`, `findByUserIdAndTenantKey(@Param userId, @Param tenantKey): Optional<TenantMembership>`, `existsByUserIdAndTenantKey(@Param userId, @Param tenantKey): boolean`, `findByTenantKey(String): List<TenantMembership>`, `findByUserId(UUID): List<TenantMembership>`, `deleteById(UUID)`
    - XML: all SQL targeting `public.tenant_memberships`
    - _Requirements: 4.2, 4.3_

  - [x] 4.4 Implement `TenantMemberAuthorityMapper` interface and `TenantMemberAuthorityMapper.xml`
    - Interface: `insert(TenantMemberAuthority)`, `findByMembershipId(UUID): List<TenantMemberAuthority>`, `findAuthorityValuesByMembershipId(UUID): List<String>`, `deleteByMembershipId(UUID)`
    - XML: all SQL targeting `public.tenant_member_authorities`
    - _Requirements: 4.7, 20.23_

  - [x] 4.5 Implement `TokenDenylistMapper` interface and `TokenDenylistMapper.xml`
    - Interface: `insert(TokenDenylist)`, `existsByJti(String): boolean`, `findByUserId(UUID): List<TokenDenylist>`, `deleteByExpiresAtBefore(Instant)`
    - XML: all SQL targeting `public.token_denylist`
    - _Requirements: 12.1, 12.3_

  - [x] 4.6 Implement `FailedLoginAttemptMapper` interface and `FailedLoginAttemptMapper.xml`
    - Interface: `insert(FailedLoginAttempt)`, `countByEmailAndAttemptedAtAfter(@Param email, @Param since): long`, `deleteByEmail(String)`
    - XML: all SQL targeting `public.failed_login_attempts`
    - _Requirements: 9.1, 9.2_

  - [x] 4.7 Implement `EmailVerificationTokenMapper` interface and `EmailVerificationTokenMapper.xml`
    - XML: `resultMap` mapping all columns; `countResendsWithinWindow` counts rows WHERE `user_id = #{userId} AND last_resend_at >= #{since}`; all SQL targeting `public.email_verification_tokens`
    - _Requirements: 18.1, 18.7, 18.8, 18.11_

  - [x] 4.8 Directory Structure
    ├── user/
    │   ├── User.java
    │   ├── AccountStatus.java
    │   ├── UserMapper.java
    │   ├── UserMapper.xml
    │   ├── UserService.java
    │   ├── UserServiceImpl.java
    │   ├── UserRestResource.java
    │   └── dto/
    │       ├── UserDtos.java
    │       └── UserDtoMapper.java
    ├── email/
    │   ├── EmailVerificationRestResource.java
    │   └── dto/
    │       └── EmailVerificationDtos.java
    ├── passwordreset/
    │   ├── PasswordResetToken.java
    │   ├── PasswordResetTokenMapper.java
    │   ├── PasswordResetTokenMapper.xml
    │   ├── PasswordResetService.java
    │   ├── PasswordResetServiceImpl.java
    │   ├── PasswordResetRestResource.java
    │   ├── ExpiredPasswordResetTokenReaperJob.java
    │   └── dto/
    │       └── PasswordResetDtos.java
    - Interface: `insert(PasswordResetToken)`, `findByToken(String): Optional<PasswordResetToken>`, `deleteByToken(String)`, `deleteByUserId(UUID)`, `deleteByExpiresAtBefore(Instant)`, `countByUserIdAndCreatedAtAfter(@Param userId, @Param since): int`
    - XML: `resultMap` mapping all columns; all SQL targeting `public.password_reset_tokens`
    - Update `UserMapper.xml` to include `updatePassword(@Param userId, @Param passwordHash)` method
    - _Requirements: 25.7, 26.1-26.3_

- [x] 5. Liquibase migrations
  - [x] 5.1 Create system schema migrations in `db/changelog/system/`
    - `db.changelog-master.xml`: master changelog including all system changesets in order
    - `20260115120000-create-tenants-table.xml`: `tenants` table with UNIQUE constraint on both `tenant_key` and `name`; indexes `idx_tenants_tenant_key`, `idx_tenants_name` (UNIQUE)
    - `20260115120100-create-users-table.xml`: `users` table with `email_verified BOOLEAN NOT NULL DEFAULT FALSE` and `last_global_signout_at TIMESTAMP NULL`; index `idx_users_email`
    - `20260115120200-create-tenant-memberships-table.xml`: `tenant_memberships` table; UNIQUE constraint on `(user_id, tenant_key)`; indexes `idx_tenant_memberships_user_id`, `idx_tenant_memberships_tenant_key`
    - `20260115120300-create-tenant-member-authorities-table.xml`: `tenant_member_authorities` table; index `idx_tenant_member_authorities_membership_id`
    - `20260115120400-create-token-denylist-table.xml`: `token_denylist` table; indexes `idx_token_denylist_jti`, `idx_token_denylist_user_id`, `idx_token_denylist_expires_at`
    - `20260115120500-create-failed-login-attempts-table.xml`: `failed_login_attempts` table; index `idx_failed_login_attempts_email`
    - `20260115120600-create-shedlock-table.xml`: `shedlock` table (name VARCHAR 64 PK, lock_until TIMESTAMP, locked_at TIMESTAMP, locked_by VARCHAR 255)
    - `20260115120700-create-email-verification-tokens-table.xml`: `email_verification_tokens` table with `token VARCHAR(64) UNIQUE NOT NULL`, `expires_at TIMESTAMP NOT NULL`, `resend_count INT NOT NULL DEFAULT 0`, `last_resend_at TIMESTAMP NULL`, FK `user_id → users.id ON DELETE CASCADE`; indexes `idx_email_verification_tokens_token` (UNIQUE), `idx_email_verification_tokens_user_id`, `idx_email_verification_tokens_expires_at`
    - `20260115120800-create-password-reset-tokens-table.xml`: `password_reset_tokens` table; columns: id UUID PK, user_id UUID NOT NULL FK → users.id ON DELETE CASCADE, token VARCHAR(64) UNIQUE NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL; indexes `idx_password_reset_tokens_user_id`, `idx_password_reset_tokens_token` (UNIQUE)
    - All changesets: `author="iqscaffold"`
    - _Requirements: 19.2, 19.4-19.7, 26.1-26.2_

  - [x] 5.2 Create tenant schema changelog `db/changelog/tenant/master.xml`
    - Empty master changelog (zero changesets for MVP); Liquibase tracking tables are initialized in `t_{tenantKey}` when `TenantLiquibaseRunner.runMigrationsForTenant` runs
    - _Requirements: 2.8, 19.3_

  - [x] 5.3 Implement `TenantLiquibaseRunner`
    - Implements `ApplicationRunner`; constructor-inject `DataSource`
    - `run()`: call `runMigrations("public", "db/changelog/system/master.xml")`
    - `runMigrationsForTenant(String tenantKey)`: call `runMigrations("t_" + tenantKey, "db/changelog/tenant/master.xml")`; propagate all exceptions to the caller (do NOT catch here)
    - `runMigrations(String schema, String changelogPath)`: private — get connection, `CREATE SCHEMA IF NOT EXISTS {schema}`, `SET search_path TO {schema}`, build `Liquibase`, call `update(new Contexts(), new LabelExpression())`
    - _Requirements: 2.6-2.8, 2.11_

- [ ] 6. Account lockout
  - [ ] 6.1 Implement `AccountLockoutManager`
    - `@Component`; constructor-inject `FailedLoginAttemptMapper`, `AuthConfigurationProperties`
    - `recordFailedAttempt(String email)`: build `FailedLoginAttempt` with `UUID.randomUUID()` and `Instant.now()`, call `mapper.insert(attempt)`
    - `isLocked(String email)`: call `mapper.countByEmailAndAttemptedAtAfter(email, Instant.now().minus(lockoutDuration))`, return `count >= threshold`
    - `reset(String email)`: call `mapper.deleteByEmail(email)`
    - _Requirements: 9.2-9.6_

- [ ] 7. Token denylist
  - [ ] 7.1 Implement `TokenDenylistService`
    - `@Component`; constructor-inject `TokenDenylistMapper`
    - `denyToken(String jti, UUID userId, Instant expiresAt)`: build `TokenDenylist` with `UUID.randomUUID()` and `Instant.now()` for createdAt, call `mapper.insert(entry)`
    - `isRevoked(String jti): boolean`: call `mapper.existsByJti(jti)`
    - `@Scheduled(cron = "0 0 * * * *")` `@SchedulerLock(name = "TokenDenylistService.cleanupExpired", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")` `cleanupExpired()`: call `mapper.deleteByExpiresAtBefore(Instant.now())`
    - `ShedLockConfig`: `@Configuration` bean providing `JdbcTemplateLockProvider` with `usingDbTime()` backed by the existing `DataSource`
    - _Requirements: 12.1, 12.5, 12.6, 20.25_

  - [ ] 7.2 Implement `StuckTenantReaperJob`
    - `@Component`; constructor-inject `TenantMapper`, `TenancyConfigurationProperties`
    - `@Scheduled(cron = "0 */5 * * * *")` `@SchedulerLock(name = "StuckTenantReaperJob.reapStuckTenants", lockAtMostFor = "PT4M", lockAtLeastFor = "PT1M")` `reapStuckTenants()`:
      1. Compute `cutoff = Instant.now().minus(tenancyProps.provisioningTimeout())`
      2. Call `tenantMapper.findStuckProvisioning(cutoff)`
      3. For each stuck tenant: call `tenantMapper.updateStatus(tenantKey, "PROVISIONING_FAILED")`; log ERROR with tenantKey and created_at
      4. Log WARN summary if any tenants were reaped; return immediately if list is empty
    - `TenancyConfigurationProperties` gains `provisioningTimeout` field (type `Duration`, default `PT10M`); add `provisioning-timeout: PT10M` to `application.yml` under `iqscaffold.tenancy`
    - _Requirements: 1.13, 1.17, 20.26_

  - [ ] 7.3 Implement `ExpiredVerificationTokenReaperJob`
    - `@Component`; constructor-inject `EmailVerificationTokenMapper`
    - `@Scheduled(cron = "0 0 * * * *")` `@SchedulerLock(name = "ExpiredVerificationTokenReaperJob.cleanup", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")` `cleanup()`: call `emailVerificationTokenMapper.deleteByExpiresAtBefore(Instant.now())`
    - _Requirements: 18.11_

  - [ ] 7.4 Implement `ExpiredPasswordResetTokenReaperJob`
    - `@Component`; constructor-inject `PasswordResetTokenMapper`
    - `@Scheduled(cron = "0 0 * * * *")` `@SchedulerLock(name = "ExpiredPasswordResetTokenReaperJob.cleanup", lockAtMostFor = "PT55M", lockAtLeastFor = "PT5M")` `cleanup()`: call `passwordResetTokenMapper.deleteByExpiresAtBefore(Instant.now())`
    - _Requirements: 26.4_

  - [ ] 7.5 Implement `JwtAuthenticationFilter`
    - On every authenticated request: extract Bearer token, decode JWT, then check both revocation conditions:
      1. JTI denylist: call `tokenDenylistService.isRevoked(jti)` — covers regular signout
      2. Global signout: call `userMapper.findLastGlobalSignoutAt(userId)`; revoked if `iat ≤ lastGlobalSignoutAt` — covers signout-all
    - Return 401 with `{"title":"Token revoked","status":401}` if either check fails
    - Skip filter for public endpoints (same list as `SecurityConfig`)
    - Register as a filter in `SecurityConfig` before `BearerTokenAuthenticationFilter`
    - _Requirements: 12.3, 12.4_

- [ ] 8. Messaging infrastructure
  - [ ] 8.1 Implement `RabbitMQConfig`
    - `@ConditionalOnProperty(name = "iqscaffold.messaging.rabbitmq.enabled", havingValue = "true")`, `@Profile("!test")`
    - Exchanges: `iqscaffold.events` (topic, durable), `iqscaffold.dlx` (topic, durable)
    - Queues with DLX args and 24h TTL: `iqscaffold.user.events` (routing `user.#`), `iqscaffold.notifications` (routing `notification.*`), `iqscaffold.tenant.provisioning` (routing `tenant.created`)
    - Dead-letter queue: `iqscaffold.dlq` (no TTL, bound to `iqscaffold.dlx` with `#`)
    - String constants: `EVENTS_EXCHANGE`, `DLX_EXCHANGE`, `USER_EVENTS_QUEUE`, `NOTIFICATIONS_QUEUE`, `TENANT_PROVISIONING_QUEUE`, `DLQ`; routing key constants for all event types
    - _Requirements: 1.3_

  - [ ] 8.2 Implement event payload classes in `infrastructure/messaging/`
    - `TenantEvent`: tenantKey, tenantName, eventType (enum: `TENANT_CREATED`, `TENANT_UPDATED`, `TENANT_DELETED`), occurredAt
    - `UserEvent`: userId, tenantId, email, eventType (enum: `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`), occurredAt
    - `NotificationEvent`: recipientEmail, locale (String, e.g. `"en"`, `"ru"`), type (`NotificationEventType` enum: `VERIFY_EMAIL`, `EMAIL_VERIFIED`), payload (`Map<String, Object>` — template variables), occurredAt
    - `NotificationEventType`: enum `VERIFY_EMAIL`, `EMAIL_VERIFIED`
    - `MessagingException`: unchecked exception wrapping AMQP errors
    - _Requirements: 1.3, 18.1_

  - [ ] 8.3 Implement `MessagingService`
    - Constructor-inject `RabbitTemplate`, `ObjectMapper`
    - `publishTenantCreated(String tenantKey, String tenantName)`: build `TenantEvent(TENANT_CREATED)`, publish to `EVENTS_EXCHANGE` with routing key `tenant.created`
    - `publishTenantUpdated(String tenantKey)`: routing key `tenant.updated`
    - `publishUserEvent(UserEvent event, String routingKey)`: publish to `EVENTS_EXCHANGE`
    - `publishNotification(NotificationEvent event)`: publish to `EVENTS_EXCHANGE` with routing key `notification.email`
    - Wrap `AmqpException` in `MessagingException`
    - _Requirements: 1.3, 18.1_

  - [ ] 8.4 Implement `UserEventPublisher`
    - Facade over `MessagingService`
    - `publishUserCreated(User user)`: routing key `user.created`
    - `publishUserDeleted(User user)`: routing key `user.deleted`
    - _Requirements: 1.3_

  - [ ] 8.5 Implement `UserEventListener`
    - `@RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE)` — log received events (stub for now)
    - _Requirements: 1.3_

  - [ ] 8.6 Implement `NotificationConsumer` and email infrastructure
    - `NotificationConsumer`: `@Component`; `@RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)`; constructor-inject `EmailService`; `handleNotification(NotificationEvent event)`: delegate to `emailService.send(event)`; log errors but do NOT rethrow — failed messages route to DLQ via `x-dead-letter-exchange`
    - `EmailService`: `@Service`; constructor-inject `JavaMailSender`, `TemplateEngine` (Thymeleaf), `NotificationConfigurationProperties`, `MessageSource`; `send(NotificationEvent event)`: resolve `Locale` from `event.getLocale()` (fallback to `notificationProps.defaultLocale()`); switch on `event.getType()` to select template name; build Thymeleaf `Context` with locale + `event.getPayload()` variables; render HTML via `templateEngine.process(templateName, ctx)`; build `MimeMessage` via `javaMailSender.createMimeMessage()`; set from (`notificationProps.mail().from()`), to, subject (resolved from `MessageSource` with locale), HTML body; call `javaMailSender.send(msg)`; log success/failure
    - Template resolution: `email/signup/verify-email` → `src/main/resources/templates/email/signup/verify-email.html`; `email/signup/email-verified` → `src/main/resources/templates/email/signup/email-verified.html`; `email/password-reset/initiate` → `src/main/resources/templates/email/password-reset/initiate.html`; `email/password-reset/confirmed` → `src/main/resources/templates/email/password-reset/confirmed.html`
    - Subject keys: `email.verify-email.subject`, `email.email-verified.subject`, `email.password-reset.initiate.subject`, `email.password-reset.confirmed.subject` in `messages*.properties`
    - `NotificationConfigurationProperties` (`iqscaffold.notification`): `mail.from` (String), `mail.reply-to` (String, optional), `default-locale` (String, default `"en"`), `base-url` (String — used in templates for links, e.g. `https://app.example.com`)
    - `MessageSource` bean (`@Bean messageSource()`): `ReloadableResourceBundleMessageSource`, basename `classpath:messages`, encoding UTF-8, cache seconds 3600, `fallbackToSystemLocale=false`; also configure `spring.messages.basename=messages` in `application.yml` so Spring's auto-config aligns
    - _Requirements: 18.1, 18.5, 18.8_

- [ ] 9. Security configuration and JWT infrastructure
  - [ ] 9.1 Implement `SecurityConfig`
    - Stateless sessions, CSRF disabled
    - Public endpoints: `/actuator/**`, `/api-docs/**`, `/swagger-ui/**`, `/.well-known/**`, `/api/v1/iam/auth/signup`, `/api/v1/iam/users/tenants`, `/api/v1/iam/auth/signin`, `/api/v1/iam/auth/refresh`, `/api/v1/iam/auth/validate`, `/api/v1/iam/users/email/verify`, `/api/v1/iam/users/email/resend-verification`, `/api/v1/iam/users/password/forgot`, `/api/v1/iam/users/password/reset`
    - `GET /api/v1/iam/tenants/**` and `PATCH /api/v1/iam/tenants/**` require `TENANT_OWNER` authority (configured in `authorizeHttpRequests`, not `@PreAuthorize`, so it applies globally)
    - `NimbusJwtDecoder` bean with RSA public key loaded from `AuthConfigurationProperties.jwt().publicKeyPath()`
    - `JwtGrantedAuthoritiesConverter` with no prefix, claim name `JwtClaimNames.AUTHORITIES`
    - `BCryptPasswordEncoder(12)` bean
    - Register `JwtAuthenticationFilter` before `BearerTokenAuthenticationFilter`
    - _Requirements: 10.1, 20.15, 20.16_

  - [ ] 9.2 Implement `JwtTokenGenerator`
    - Constructor loads RSA `PrivateKey` from `AuthConfigurationProperties.jwt().privateKeyPath()`
    - `generateAccessToken(User user, String tenantKey, List<String> authorities)`: RS256, expiry from `authProps.jwt().expiry()` (default PT15M); claims: sub, iss, iat, exp, jti (UUID), type=access, userId, username, email, firstName, lastName, tenant_id, email_verified, authorities — use `JwtClaimNames` constants exclusively
    - `generateRefreshToken(User user, String tenantKey)`: RS256, expiry from `authProps.jwt().refreshExpiry()` (default P7D); claims: sub, iss, iat, exp, jti (UUID), type=refresh, username, tenant_id
    - _Requirements: 6.1-6.4, 10.7-10.10, 20.14, 20.15_

  - [ ] 9.3 Implement `CorrelationIdFilter`
    - `@Order(Ordered.HIGHEST_PRECEDENCE)`, extends `OncePerRequestFilter`
    - Read `X-Correlation-ID` header; generate UUID if absent; put in MDC as `correlationId`; set on response; remove in `finally`
    - _Requirements: 23.4_

- [ ] 10. Tenant management feature
  - [ ] 10.1 Implement `TenantDtos` and `TenantDtoMapper`
    - `UpdateTenantStatusRequest`: status TenantStatus (`@NotNull`)
    - `TenantResponse`: tenantKey, name, status, createdAt
    - `TenantDtoMapper`: final class, static `toResponse(Tenant): TenantResponse`
    - _Requirements: 1.8, 5.7, 20.3-20.5_

  - [ ] 10.2 Implement `TenantService` interface and `TenantServiceImpl`
    - `@Service @Transactional`; `@Transactional(readOnly=true)` on read methods
    - `createTenant(String tenantName, UUID ownerUserId)`: check `tenantMapper.existsByName(tenantName)`, throw `TenantAlreadyExistsException` if duplicate; generate 8-char NanoID tenantKey (alphabet `a-z0-9`); set `id = UUID.randomUUID()`, `createdAt = LocalDateTime.now()`, `createdBy = ownerUserId.toString()`; call `tenantMapper.insertIfAbsent(tenant)`; call `messagingService.publishTenantCreated(tenantKey, tenantName)`; return tenant
    - `getTenantByKey(String tenantKey)`: call `tenantMapper.findByTenantKey(tenantKey)`, throw `TenantNotFoundException` if absent
    - `updateTenantStatus(String tenantKey, TenantStatus newStatus)`: validate allowed transitions (ACTIVE→SUSPENDED, SUSPENDED→ACTIVE, ACTIVE→DELETED, SUSPENDED→DELETED, PROVISIONING_FAILED→DELETED); call `tenantMapper.updateStatus(tenantKey, newStatus.name())`
    - `retryProvisioning(String tenantKey)`: load tenant via `tenantMapper.findByTenantKey(tenantKey)`, throw `TenantNotFoundException` if absent; assert `tenant.getStatus() == PROVISIONING_FAILED`, throw `InvalidTenantStateException("Tenant is not in PROVISIONING_FAILED state")` → 409 if not; call `tenantMapper.updateStatus(tenantKey, "PROVISIONING")`; call `messagingService.publishTenantCreated(tenantKey, tenant.getName())`; return updated tenant
    - _Requirements: 1.1-1.3, 1.7-1.10, 1.14-1.16, 5.1-5.7_

  - [ ] 10.3 Implement `TenantProvisioningConsumer`
    - `@RabbitListener(queues = RabbitMQConfig.TENANT_PROVISIONING_QUEUE)`
    - Extract tenantKey from `TenantEvent`
    - Call `tenantLiquibaseRunner.runMigrationsForTenant(tenantKey)`
    - On success: call `tenantMapper.updateStatus(tenantKey, "ACTIVE")`; call `messagingService.publishTenantUpdated(tenantKey)`
    - On any exception: call `tenantMapper.updateStatus(tenantKey, "PROVISIONING_FAILED")`; log error; do NOT rethrow (message is not requeued; failed state is observable via the status endpoint and retryable via `POST /retry-provisioning`)
    - _Requirements: 1.4-1.6_

  - [ ] 10.4 Implement `TenantRestResource`
    - `@RestController @RequestMapping("/api/v1/iam/tenants")`
    - `GET /{tenantKey}` → 200 OK, body `TenantResponse`; `@PreAuthorize("hasAuthority('TENANT_OWNER')")`
    - `PATCH /{tenantKey}/status` → 200 OK, body `TenantResponse`; `@PreAuthorize("hasAuthority('TENANT_OWNER')")`
    - `POST /{tenantKey}/retry-provisioning` → 202 Accepted, body `TenantResponse`; `@PreAuthorize("hasAuthority('TENANT_OWNER')")`; delegates to `TenantService.retryProvisioning(tenantKey)`
    - `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement(name="bearerAuth")` on all endpoints
    - _Requirements: 1.11-1.12, 1.14, 5.7, 21.1-21.9_

- [ ] 11. User registration and profile management
  - [ ] 11.1 Implement `UserDtos` and `UserDtoMapper`
    - `RegisterUserRequest`: email (`@Email @NotBlank`), password (`@Size(min=8,max=128) @Pattern` for complexity), firstName (`@NotBlank @Size(max=100)`), lastName (`@NotBlank @Size(max=100)`), tenantName (`@NotBlank @Size(min=1, max=100)`)
    - `UpdateUserRequest`: firstName, lastName (both `@NotBlank @Size(max=100)`)
    - `UserResponse`: id (UUID), email, firstName, lastName, status, createdAt — no passwordHash
    - `SignupResponse`: userId (UUID), email, tenantKey, tenantStatus — returned on successful signup
    - `UserDtoMapper`: final class, static `toResponse(User): UserResponse`, static `toSignupResponse(User, Tenant): SignupResponse`
    - _Requirements: 8.4-8.8, 15.4, 16.8, 20.3-20.5_

  - [ ] 11.2 Implement `UserService` interface and `UserServiceImpl`
    - `@Service @Transactional`; `@Transactional(readOnly=true)` on reads
    - Constructor-inject: `UserMapper`, `TenantMembershipMapper`, `TenantMemberAuthorityMapper`, `TenantService`, `UserEventPublisher`, `PasswordEncoder`
    - `registerUser(RegisterUserRequest)`:
      1. Build a `User` object with `UUID.randomUUID()`, BCrypt-hashed password, `emailVerified = false`, `createdAt = LocalDateTime.now()`, `createdBy = "system"`; call `userMapper.upsertByEmail(user)` (`INSERT ... ON CONFLICT (email) DO NOTHING`); then load the canonical record via `userMapper.findByEmail(email)` — this is safe under concurrent requests because the upsert is atomic
      2. Generate 8-char NanoID `tenantKey`; build a `Tenant` with `UUID.randomUUID()`, status `PROVISIONING`, `createdAt = LocalDateTime.now()`; call `tenantMapper.insertIfAbsent(tenant)` (`INSERT ... ON CONFLICT (name) DO NOTHING`); if zero rows inserted (name already taken) throw `TenantAlreadyExistsException("Tenant name already taken")` → 409; otherwise load the new tenant via `tenantMapper.findByTenantKey(tenantKey)`
      3. Build `TenantMembership` with `UUID.randomUUID()`, `createdAt = LocalDateTime.now()`, call `membershipMapper.insert(membership)`
      4. Build `TenantMemberAuthority` with `UUID.randomUUID()`, authority `"TENANT_OWNER"`, call `authorityMapper.insert(authority)`
      5. Publish `TenantEvent(TENANT_CREATED)` via `MessagingService.publishTenantCreated(tenantKey, tenantName)`
      6. Publish `UserEvent(USER_CREATED)` via `UserEventPublisher`
      7. Generate email verification token: `SecureRandom` 32 bytes → hex string (64 chars); build `EmailVerificationToken` with `UUID.randomUUID()`, `expiresAt = Instant.now().plus(24h)`, `resendCount = 0`; call `emailVerificationTokenMapper.insert(token)`
      8. Publish `NotificationEvent(VERIFY_EMAIL, recipientEmail=user.getEmail(), locale="en", payload={verificationUrl, firstName, expiresInHours:24})` via `MessagingService.publishNotification(event)`
      9. Return `UserDtoMapper.toSignupResponse(user, tenant)`
    - `getUserById(UUID id)`: `userMapper.findById(id)`, throw `UserNotFoundException` if absent
    - `updateUser(UUID id, String firstName, String lastName, String updatedBy)`: find user, update fields + `updatedAt = LocalDateTime.now()`, call `userMapper.update(user)`
    - `deleteUser(UUID userId, String tenantKey)`: resolve membership via `membershipMapper.findByUserIdAndTenantKey`, call `membershipMapper.deleteById(membership.getId())` (DB cascade removes authorities), publish `UserEvent(USER_DELETED)` — does NOT delete the global `User` record
    - _Requirements: 8.1-8.10, 15.1-15.7, 16.1-16.8, 17.1-17.3_

  - [ ] 11.3 Implement `UserRestResource`
    - `@RestController @RequestMapping("/api/v1/iam/users")`
    - `GET /me` → 200 OK, `UserResponse`; extract userId from JWT `JwtClaimNames.USER_ID` claim
    - `PATCH /me` → 200 OK, `UserResponse`
    - `DELETE /me` → 204 No Content
    - `POST /tenants` → 200 OK, body `List<TenantMembershipSummary>`; delegates to `AuthenticationService.listUserTenants(email, password)` — public discovery, no tenant context required
    - `@PreAuthorize("isAuthenticated()")` on `/me` endpoints; `@PreAuthorize("permitAll()")` on `/tenants`
    - `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement(name="bearerAuth")` on all endpoints
    - _Requirements: 13.1, 15.6, 16.7, 17.3, 21.1-21.9_

- [ ] 12. Membership service
  - [ ] 12.1 Implement `MembershipService` interface and `MembershipServiceImpl`
    - `@Service @Transactional(readOnly=true)`
    - `resolveMembership(UUID userId, String tenantKey): TenantMembership`: call `membershipMapper.findByUserIdAndTenantKey(userId, tenantKey)`, throw `MembershipNotFoundException` if absent; throw `MembershipNotFoundException` if status is `SUSPENDED` or `REMOVED`
    - `getAuthorities(UUID membershipId): List<String>`: call `authorityMapper.findAuthorityValuesByMembershipId(membershipId)`
    - _Requirements: 4.2-4.6, 7.1-7.5_

- [ ] 13. Authentication feature
  - [ ] 13.1 Implement `AuthenticationDtos` and `AuthenticationDtoMapper`
    - `SignInRequest`: email (`@Email @NotBlank`), password (`@NotBlank`)
    - `TenantDiscoveryRequest`: email (`@Email @NotBlank`), password (`@NotBlank`)
    - `TenantMembershipSummary`: tenantKey, tenantName, membershipStatus, authorities (List<String>)
    - `RefreshTokenRequest`: refreshToken (`@NotBlank`)
    - `TokenResponse`: accessToken, refreshToken, tenantKey
    - `ValidateTokenResponse`: userId (UUID), email, tenantId, authorities (List<String>)
    - `VerifyEmailRequest`: token (`@NotBlank @Size(min=64, max=64)`)
    - `ResendVerificationRequest`: email (`@Email @NotBlank`)
    - `ForgotPasswordRequest`: email (`@Email @NotBlank`)
    - `ResetPasswordRequest`: token (`@NotBlank @Size(min=64, max=64)`), newPassword (`@Size(min=8,max=128)`)
    - _Requirements: 10.10, 12.8, 13.5, 25.1_

  - [ ] 13.2 Implement `AuthenticationService` interface and `AuthenticationServiceImpl`
    - `@Service @Transactional`
    - `authenticate(String email, String password)`:
      1. Find tenant via `tenantMapper.findByTenantKey(TenantContext.getCurrentTenant())`; throw `TenantSuspendedException` if SUSPENDED; throw `TenantNotFoundException` if DELETED or PROVISIONING_FAILED
      2. Find user via `userMapper.findByEmail(email)`; throw `UserNotFoundException` if absent
      3. Check `accountLockoutManager.isLocked(email)`; throw `BadCredentialsException("Account temporarily locked")` if true
      4. Validate password with `passwordEncoder.matches`; on failure call `accountLockoutManager.recordFailedAttempt(email)` then throw `BadCredentialsException`
      5. Resolve membership via `membershipService.resolveMembership(user.getId(), tenantKey)`
      6. Load authorities via `membershipService.getAuthorities(membership.getId())`
      7. Call `accountLockoutManager.reset(email)`
      8. Generate access + refresh tokens via `JwtTokenGenerator`
      9. Return `TokenResponse`
    - `refreshToken(String refreshToken)`: decode token, assert `type=refresh`, assert `tenant_id` matches `TenantContext.getCurrentTenant()` (throw `TenantContextMismatchException` on mismatch), check user/membership/tenant status, issue new token pair
    - `validateToken(String token)`: decode, check denylist, return `ValidateTokenResponse`
    - `listUserTenants(String email, String password)`: validate credentials (same lockout rules as `authenticate`); if valid call `membershipMapper.findByUserId(userId)`, load tenant for each, filter to memberships with status `ACTIVE` and tenant status `ACTIVE`, load authorities per membership, return `List<TenantMembershipSummary>`; if invalid credentials throw `BadCredentialsException`
    - `verifyEmail(String token)`: call `emailVerificationTokenMapper.findByToken(token)`; if absent or `expiresAt < Instant.now()` throw `InvalidVerificationTokenException` → 400; call `userMapper.setEmailVerified(userId)`; call `emailVerificationTokenMapper.deleteByUserId(userId)`; publish `NotificationEvent(EMAIL_VERIFIED, recipientEmail, locale="en", payload={firstName, signinUrl})` via `MessagingService.publishNotification(event)`
    - `resendVerification(String email)`: always returns without error (prevents enumeration); find user by email — if not found or `emailVerified = true` return silently; call `emailVerificationTokenMapper.countResendsWithinWindow(userId, Instant.now().minus(1h))`; if count >= 3 throw `VerificationRateLimitException` → 429 with `Retry-After: 3600`; call `emailVerificationTokenMapper.deleteByUserId(userId)`; generate new token; insert; publish `NotificationEvent(VERIFY_EMAIL, recipientEmail, locale="en", payload={verificationUrl, firstName, expiresInHours:24})`
    - _Requirements: 10.1-10.11, 11.1-11.7, 12.1-12.8, 13.1-13.7, 18.3-18.8_

  - [ ] 13.3 Implement `AuthenticationRestResource`
    - `@RestController @RequestMapping("/api/v1/iam/auth")`
    - `POST /signup` → 201 Created + `Location: /api/v1/iam/users/me` header, body `SignupResponse`; delegates to `UserService.registerUser(request)`
    - `POST /signin` → 200 OK, body `TokenResponse`
    - `POST /refresh` → 200 OK, body `TokenResponse`
    - `POST /signout` → 204 No Content; extract jti + expiresAt from current JWT, call `tokenDenylistService.denyToken(jti, userId, expiresAt)`
    - `POST /signout-all` → 204 No Content; call `userMapper.updateLastGlobalSignoutAt(userId, Instant.now())` to invalidate all previously issued tokens, then also call `tokenDenylistService.denyToken(jti, userId, expiresAt)` for the current token
    - `POST /validate` → 200 OK, body `ValidateTokenResponse`; `@PreAuthorize("permitAll()")`
    - _Requirements: 10.10, 12.6-12.8, 6.7_

  - [ ] 13.4 Implement `EmailVerificationRestResource`
    - `@RestController @RequestMapping("/api/v1/iam/users/email")`
    - `POST /verify` → 200 OK; public; body `{ "token": "<hex>" }`; delegates to `AuthenticationService.verifyEmail(token)`
    - `POST /resend-verification` → 202 Accepted; public; body `{ "email": "<address>" }`; delegates to `AuthenticationService.resendVerification(email)`
    - _Requirements: 18.2, 18.5_

  - [ ] 13.5 Implement `PasswordResetRestResource`
    - `@RestController @RequestMapping("/api/v1/iam/users/password")`
    - `POST /forgot` → 200 OK; public; body `{ "email": "<address>" }`; delegates to `PasswordResetService.initiatePasswordReset(email)`
    - `POST /reset` → 200 OK; public; body `{ "token": "<hex>", "newPassword": "<pass>" }`; delegates to `PasswordResetService.completePasswordReset(token, newPassword)`
    - _Requirements: 24.1, 24.7, 25.1, 25.11_

  - [ ] 13.6 Implement `PasswordResetService` interface and `PasswordResetServiceImpl`
    - `@Service @Transactional`
    - `initiatePasswordReset(String email)`:
      1. Call `passwordResetTokenMapper.countByUserIdAndCreatedAtAfter(userId, Instant.now().minus(rateLimitWindow))`; if count >= `rateLimitMaxRequests` throw `PasswordResetRateLimitException`
      2. Find user by email via `userMapper.findByEmail(email)` — if not found return silently (prevents enumeration)
      3. Delete existing tokens via `passwordResetTokenMapper.deleteByUserId(userId)`
      4. Generate 64-char hex token (`SecureRandom` 32 bytes → `HexFormat.of().formatHex(bytes)`)
      5. Build `PasswordResetToken` with `UUID.randomUUID()`, `expiresAt = Instant.now().plus(tokenTtl)`; call `passwordResetTokenMapper.insert(prt)`
      6. Publish `NotificationEvent(PASSWORD_RESET_INITIATED, recipientEmail=email, locale="en", payload={resetUrl, firstName, token})`
      7. Return silently regardless of outcome
    - `completePasswordReset(String token, String newPassword)`:
      1. Call `passwordResetTokenMapper.findByToken(token)`; if absent or `expiresAt < Instant.now()` throw `PasswordResetTokenNotFoundException`
      2. Validate password (same rules as registration: 8–128 chars, upper, lower, digit, special)
      3. Hash with BCrypt strength 12 via `passwordEncoder.encode(newPassword)`
      4. Call `userMapper.updatePassword(userId, hash)`
      5. Call `passwordResetTokenMapper.deleteByToken(token)`
      6. Call `userMapper.updateLastGlobalSignoutAt(userId, Instant.now())` to invalidate all existing sessions
      7. Publish `NotificationEvent(PASSWORD_RESET_CONFIRMED, recipientEmail, locale="en", payload={firstName})`
    - _Requirements: 24.1-24.10, 25.1-25.12, 26.1-26.5, 27.1-27.9, 28.1-28.8_

- [ ] 14. Global exception handler and observability
  - [ ] 14.1 Implement `GlobalExceptionHandler` (`@RestControllerAdvice`)
    - Shared `problem(String type, String title, int status, String detail, HttpServletRequest request)` helper: builds RFC 9457 `ProblemDetail` with type, title, status, detail, instance (request URI), correlationId (from MDC), requestId (`req-` + 8-char UUID)
    - Handlers:
      - `MethodArgumentNotValidException` → 400, fields array of `{field, message}`
      - `ConstraintViolationException` → 400
      - `AuthenticationException` / `BadCredentialsException` → 401
      - `AccessDeniedException` → 403
      - `InvalidVerificationTokenException` → 400 "Invalid or expired verification token"
      - `VerificationRateLimitException` → 429 with `Retry-After` header (value from exception's `retryAfterSeconds`)
      - `MembershipNotFoundException` → 403
      - `TenantContextMismatchException` → 403
      - `TenantSuspendedException` → 403 "Tenant suspended"
      - `TenantNotAvailableException` → 403 "Tenant not available"
      - `AccountLockedException` → 403 "Account temporarily locked"
      - `TenantMembershipAlreadyExistsException` → 409 "User is already a member of this tenant"
      - `InvalidTokenTypeException` → 401 "Invalid token type"
      - `TokenExpiredException` → 401 "Refresh token expired"
      - `InvalidTokenSignatureException` → 401 "Invalid token signature"
      - `PasswordResetTokenNotFoundException` → 400 "Invalid or expired password reset token"
      - `PasswordResetRateLimitException` → 429 with `Retry-After` header (value from exception's `retryAfterSeconds`)
      - `TenantManagementException` subtypes → Java 21 switch: `TenantAlreadyExistsException` → 409, `TenantNotFoundException` → 404, `SchemaProvisioningException` → 503, `InvalidTenantStateException` → 409, others → 422
      - `UserNotFoundException` → 404
      - `UserRegistrationException` → 409
      - `UserManagementException` → 422
      - `MessagingException` → 503 "Messaging service unavailable"
      - `Exception` catch-all → 500
    - Log WARN for 4xx, ERROR with stack trace for 5xx
    - _Requirements: 7.3, 7.4, 20.10_

  - [ ] 14.2 Implement `UserServiceMetrics`
    - `@Component`, constructor-inject `MeterRegistry`
    - Counters: `auth.success` (tags: tenantId), `auth.failure` (tags: tenantId, reason), `tenant.created`
    - Timer: `auth.duration` (tags: tenantId)
    - _Requirements: 23.6, 23.7_

  - [ ] 14.3 Configure actuator
    - `management.server.port: 8081`
    - `management.endpoints.web.exposure.include: health,info,metrics,prometheus`
    - `management.endpoint.health.probes.enabled: true` (liveness + readiness for Kubernetes)
    - `management.endpoint.health.show-details: when-authorized`
    - Production profile: `show-details: never`
    - _Requirements: 23.1-23.5_

- [ ] 15. OpenAPI documentation
  - [ ] 15.1 Implement `OpenApiConfig`
    - `@SecurityScheme(name="bearerAuth", type=HTTP, scheme="bearer", bearerFormat="JWT")`
    - `OpenAPI` bean with `Info`, `Server`, and `SecurityRequirement`
    - _Requirements: 21.1-21.9_

  - [ ] 15.2 Annotate all REST controllers
    - `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement(name="bearerAuth")` on all endpoints
    - `@Parameter(name="X-Tenant-ID", in=HEADER, required=true)` on all tenant-scoped endpoints
    - Swagger UI at `/swagger-ui.html`, spec at `/api-docs`
    - _Requirements: 21.1-21.9_

## Notes

- tenantKey is always 8 chars, alphabet `a-z0-9` (NanoID); schema `t_{tenantKey}` is created during provisioning
- All UUIDs are generated in the service layer with `UUID.randomUUID()` before calling mapper insert methods
- All timestamps (createdAt, updatedAt) are set in the service layer with `LocalDateTime.now()` or `Instant.now()`
- `User` is a global identity — email is globally unique, no `tenant_id` field
- Tenant isolation is enforced via `TenantMembership` — always resolve `(userId, tenantKey)` before any tenant-scoped operation
- The same user can be a member of multiple tenants simultaneously with different authorities in each
- JWT `tenant_id` claim holds the `tenantKey`; authorities come from `TenantMemberAuthority` for that membership
- `JwtTokenGenerator` takes `tenantKey` and `authorities` as explicit parameters — never reads from a user field
- All SQL lives in XML mapper files under `src/main/resources/mappers/` — no inline MyBatis annotations on mapper interfaces
- All method parameters, catch clause variables, and for-each loop variables must use `final`
- No `@Entity`, `@Table`, `@Column`, `@GeneratedValue`, `@MappedSuperclass`, or any JPA/Hibernate annotations anywhere in the codebase
- `JwtAuthenticationFilter` checks two revocation conditions on every authenticated request: JTI denylist (covers regular signout) and `iat ≤ last_global_signout_at` on the user record (covers signout-all, invalidating every token issued before that timestamp regardless of device)
- `TokenDenylistService.cleanupExpired()` runs hourly via `@Scheduled` + `@SchedulerLock`; only one pod executes the deletion per cycle — others skip via the `shedlock` table in PostgreSQL
- `UserMapper.upsertByEmail` and `TenantMapper.insertIfAbsent` use `INSERT ... ON CONFLICT DO NOTHING` to eliminate TOCTOU race conditions on concurrent signups; after the upsert the service always reloads the canonical record via `findByEmail` / `findByTenantKey` rather than assuming the inserted values are current
- `StuckTenantReaperJob` runs every 5 minutes (ShedLock-guarded) and transitions any tenant stuck in `PROVISIONING` beyond `iqscaffold.tenancy.provisioning-timeout` (default `PT10M`) to `PROVISIONING_FAILED`; this self-heals tenants whose RabbitMQ message was lost or whose consumer pod crashed before processing began
- `PROVISIONING_FAILED` tenants can be retried via `POST /api/v1/iam/tenants/{tenantKey}/retry-provisioning` (requires `TENANT_OWNER`); this transitions the tenant back to `PROVISIONING` and re-publishes the `tenant.created` event; the `PROVISIONING_FAILED → PROVISIONING` transition is only available through this dedicated endpoint, not through `PATCH /status`
