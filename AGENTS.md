# Repository Guidelines & Agent Instructions

## Overview

This document provides comprehensive guidelines for repository management, development workflows, and collaboration standards for this Maven-based Java project. It serves as a reference for both human developers and AI agents working with this codebase.

**Project:** `foundation-iam-service` — Identity and Access Management microservice for the IQKV Foundation platform.
**Package root:** `com.iqkv.foundation.iamservice`
**Main class:** `IamServiceApplication`

## 🏛️ Repository Structure & Organization

### Actual Project Layout

```
foundation-iam-service/
├── .github/                          # GitHub workflows and automation
│   └── workflows/                    # CI/CD pipeline definitions
├── docker/                           # Docker support files
│   ├── grafana/                      # Grafana dashboards and provisioning
│   └── dbgate/                       # DBGate connection configs
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/iqkv/foundation/iamservice/
│   │   │       ├── IamServiceApplication.java   # Spring Boot entry point
│   │   │       ├── announcement/                # System announcements
│   │   │       ├── authentication/              # Signin, refresh, signout, token ops
│   │   │       ├── ban/                         # User ban management
│   │   │       ├── denylist/                    # JWT token denylist (Redis)
│   │   │       ├── email/                       # Email verification tokens
│   │   │       ├── invitation/                  # Tenant invitation flow
│   │   │       ├── locale/                      # i18n locale management
│   │   │       ├── lockout/                     # Brute-force account lockout
│   │   │       ├── magiclink/                   # Magic link authentication
│   │   │       ├── membership/                  # Tenant membership & authorities
│   │   │       ├── notification/                # In-app notifications
│   │   │       ├── oauth2/                      # OAuth2/OIDC SSO (Google/GitHub/Microsoft)
│   │   │       ├── passwordreset/               # Password reset tokens
│   │   │       ├── plan/                        # Billing plan entitlement
│   │   │       ├── platformadmin/               # Platform-level admin operations
│   │   │       ├── platformauthority/           # Platform-wide authority assignments
│   │   │       ├── security/                    # JwtAuthenticationFilter, JwtClaimNames
│   │   │       ├── shared/                      # Shared exceptions, utilities
│   │   │       ├── signup/                      # User signup orchestration
│   │   │       ├── tenancy/                     # Tenant context resolution
│   │   │       ├── tenant/                      # Tenant CRUD, provisioning
│   │   │       └── user/                        # User profile, avatar, account ops
│   │   │       └── infrastructure/
│   │   │           ├── config/                  # @ConfigurationProperties records
│   │   │           ├── metrics/                 # Micrometer / Prometheus metrics
│   │   │           └── messaging/               # RabbitMQ event publishers
│   │   └── resources/
│   │       ├── application.yml                  # Base configuration (all profiles)
│   │       ├── application-local.yml            # Local dev profile
│   │       ├── application-test.yml             # Test profile
│   │       ├── keys/                            # RSA key pair (private.pem / public.pem)
│   │       ├── mappers/                         # MyBatis XML mapper files
│   │       ├── templates/                       # Thymeleaf email templates
│   │       ├── i18n/                            # Message bundles
│   │       └── db/changelog/
│   │           ├── system/                      # Liquibase: platform-level schema
│   │           └── tenant/                      # Liquibase: per-tenant schema
│   └── test/
│       └── java/com/iqkv/foundation/iamservice/
│           ├── IntegrationTest.java             # @IntegrationTest composite annotation
│           ├── TechnicalStructureTest.java      # ArchUnit onion architecture validation
│           ├── IamServiceApplicationTests.java  # Context load smoke test
│           ├── authentication/                  # AuthenticationService unit tests
│           ├── ban/, denylist/, email/          # Per-bounded-context unit tests
│           ├── invitation/, locale/, lockout/
│           ├── membership/, notification/
│           ├── passwordreset/, platformadmin/
│           ├── platformauthority/, signup/
│           ├── tenant/, user/
│           └── infrastructure/
├── compose.yaml                      # Full local dev stack
├── compose.base.yaml                 # Base services (postgres, redis, rabbitmq)
├── compose.container.yaml            # App container compose
├── pom.xml                           # Maven build (parent: boot-parent-pom 0.24.23)
└── AGENTS.md                         # This file
```

**Package structure per bounded context:**

Each bounded context (e.g. `authentication`, `user`, `tenant`) follows a flat-in-package convention — not the DDD sub-layered approach. Within a context package you will typically find:

- `*Service.java` / `*ServiceImpl.java` — business logic
- `*Mapper.java` — MyBatis `@Mapper` interface (persistence)
- `*RestResource.java` — REST controller (inbound adapter)
- `dto/*Dtos.java` — nested record DTOs grouped by context
- Domain model classes (e.g. `User.java`, `Tenant.java`, `AccountStatus.java`)

`infrastructure/` is a cross-cutting package for Spring configuration, RabbitMQ messaging, and metrics. `shared/` holds common exceptions used across contexts.

## 🤖 AI Agent Guidelines

### AI Communication Standards

**CRITICAL: Agents must communicate concisely and avoid unnecessary verbosity.**

#### Response Length Guidelines

```yaml
summaries:
    max_length: "2-3 sentences"
    focus: "What was done, not how it was done"
    avoid: "Bullet point lists, detailed recaps, obvious statements"

explanations:
    when_detailed: "Complex technical concepts, architecture decisions, security implications"
    when_brief: "Simple changes, routine operations, status updates"

verification:
    format: "Minimal wording - state outcome only"
    example: "Tests pass. Coverage at 65%."
    avoid: "Lengthy descriptions of what was verified"
```

#### Prohibited Outputs

**NEVER create these files:**

- ❌ `SUMMARY.md` - Auto-generated work summaries
- ❌ `CHANGES.md` - Change documentation (use commit messages)
- ❌ `REVIEW.md` - Code review summaries
- ❌ `ANALYSIS.md` - Analysis reports (communicate directly)
- ❌ Any markdown file documenting agent work unless explicitly requested

**Why:** These files create noise, clutter the repository, and provide no value. Use commit messages and direct communication instead.

#### Concise vs Verbose Examples

**❌ VERBOSE (Don't do this):**

```
I have successfully completed the implementation of the new user authentication
endpoint. Here's a comprehensive summary of everything I did:

• Created a new UserAuthenticationController class
• Implemented the login method with proper validation
• Added JWT token generation logic
• Created comprehensive unit tests
• Updated the OpenAPI documentation

All tests are passing successfully and the code is ready for review.
```

**✅ CONCISE (Do this):**

```
Added user authentication endpoint with JWT token generation. Tests pass, coverage at 68%.
```

#### When to Be Detailed

Provide detailed explanations ONLY for:

1. **Complex Architecture Decisions**
2. **Security Implications**
3. **Breaking Changes**
4. **Non-Obvious Technical Choices**

#### Response Templates

**For Simple Changes:**

```
Changed X to Y. Tests pass.
```

**For Bug Fixes:**

```
Fixed [issue]. Root cause: [brief explanation]. Added regression test.
```

**For New Features:**

```
Implemented [feature]. Includes [key components]. Tests pass, coverage [X]%.
```

**For Refactoring:**

```
Refactored [component] to [improvement]. No behavior changes. Tests pass.
```

#### Communication Principles

1. **Action-Oriented**: Focus on what was done, not the process
2. **Results-First**: State the outcome immediately
3. **No Redundancy**: Don't repeat what's obvious from the code
4. **No Meta-Commentary**: Don't describe your own actions
5. **Trust the User**: They can read code; don't explain obvious changes
6. **Verification is Brief**: "Tests pass" is sufficient

### Technology Stack Context

This is the confirmed technology stack. Do not assume alternatives — read the actual dependencies before making recommendations.

**Runtime & Framework**

- Java 25 with modern features (records, pattern matching, text blocks, `var`, `final` parameters)
- Spring Boot (parent: `com.iqkv:boot-parent-pom:0.24.23`)
- Maven for build management; `.mvn/` wrapper present
- `@SpringBootApplication`, `@EnableScheduling`, `@EnableSchedulerLock`, `@ConfigurationPropertiesScan` on main class

**Persistence**

- **MyBatis** (NOT Spring Data JPA / Hibernate) — `@Mapper` interfaces + XML mapper files in `src/main/resources/mappers/`
- `mybatis.configuration.map-underscore-to-camel-case=true`
- PostgreSQL (production), H2 (test scope only)
- **Liquibase** for schema migrations — two separate changelogs:
    - `db/changelog/system/db.changelog-system.xml` — platform-level tables (tenants, users, platform_authorities, shedlock, …)
    - `db/changelog/tenant/db.changelog-tenant.xml` — per-tenant schema (provisioned dynamically)
    - Liquibase is disabled by default in `application.yml` (`spring.liquibase.enabled: false`); the custom `iqkv.liquibase.*` runner handles it

**Caching & Session**

- Redis (`spring-boot-starter-data-redis`) — used for token denylist, rate-limit counters, lockout state, magic-link tokens, OAuth2 state, etc.

**Messaging**

- RabbitMQ (`spring-boot-starter-amqp`) — async notifications (email, in-app), signin attempt events published via `MessagingService`

**Security**

- Spring Security (`spring-boot-starter-security`)
- OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`) — validates Bearer JWTs
- OAuth2 Client (`spring-boot-starter-security-oauth2-client`) — SSO with Google, GitHub, Microsoft
- **JJWT** (`io.jsonwebtoken:jjwt-api/impl/jackson`) — RS256 JWT generation (`JwtTokenGenerator`) using RSA PEM key pair
- BCrypt password encoding at strength 12

**Object Storage**

- MinIO S3-compatible client (`io.minio:minio`) — avatar uploads, presigned URL generation

**Scheduling**

- ShedLock (`shedlock-spring` + `shedlock-provider-jdbc-template`) — distributed lock for scheduled jobs (invitation reaper, etc.)

**Internal Platform Libraries**

- `com.iqkv:foundation-tenancy` — `TenantContext` ThreadLocal, tenant resolution
- `com.iqkv:foundation-entitlement-plan-resolver-mvc` — billing plan quota enforcement
- `com.iqkv:foundation-audit-model` + `foundation-audit-spi` — audit event model

**Observability**

- Spring Boot Actuator — `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` on port `8081`
- Micrometer + Prometheus registry — custom `IamServiceMetrics` for auth duration timers and outcome counters
- Logstash Logback encoder — structured JSON logs
- Git commit ID plugin — build info in `/actuator/info`

**API Documentation**

- SpringDoc OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui`)
- Swagger UI at `/swagger-ui.html`, API docs at `/api-docs`
- Controllers use `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement`

**Email & Templates**

- Spring Mail (`spring-boot-starter-mail`) + Thymeleaf templates (`spring-boot-starter-thymeleaf`) for transactional emails (verification, password reset, invitations, magic links)
- i18n message bundles under `src/main/resources/i18n/`

**Multi-Tenancy**

- Tenant resolved from `X-Tenant-ID` request header, stored in `TenantContext` (ThreadLocal)
- Two rollout modes configured via `iqkv.platform.rollout-mode`:
    - `MULTI_TENANT` (default) — each signup creates a new tenant; user gets `TENANT_OWNER` authority
    - `SINGLE_TENANT` — all users join a pre-provisioned default tenant with `MEMBER` authority

**Unique ID Generation**

- NanoID (`com.aventrix.jnanoid:jnanoid`) — 8-char alphanumeric tenant keys

## 📋 Development Standards

### Branch Strategy

```
main (production-ready code)
├── develop (main development branch)
├── feature/* (new features)
├── bugfix/* (bug fixes)
├── improvement/* (enhancements)
├── hotfix/* (production fixes)
└── rfc/* (request for comments)
```

### Branch Naming Conventions

```bash
feature/add-magic-link-authentication
bugfix/fix-token-refresh-tenant-mismatch
improvement/optimize-membership-query
hotfix/critical-lockout-bypass
rfc/new-oidc-provider-flow
```

### Commit Message Format (Conventional Commits)

**Format:**

```
type(scope): subject

[optional body]

[optional footer]
```

**Allowed Types:** `feat`, `fix`, `rfc`, `docs`, `style`, `improvement`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`, `revert`

**Scope examples for this project:** `auth`, `user`, `tenant`, `membership`, `invitation`, `oauth2`, `magiclink`, `denylist`, `lockout`, `ban`, `notification`, `security`, `config`, `metrics`

**Rules:**

- Subject line: 6–220 characters, imperative mood, no trailing period
- Use lowercase for type and scope

**Examples:**

```bash
feat(magiclink): add magic link token exchange endpoint

fix(lockout): reset failed attempts counter after successful signin

perf(membership): add index on tenant_memberships.user_id column

refactor(auth): extract token validation into separate method
```

### AI Commit Message Generation

After completing multi-file changes, AI agents should present a commit message for review before applying it:

```
Here's the suggested commit message:

---
feat(invitation): add invitation expiry reaper with ShedLock

Schedules a periodic job to clean up expired tenant invitations.
Uses ShedLock to prevent duplicate execution across instances.
---

Accept, modify, or reject?
```

### Code Quality Standards

#### Java Code Standards (Java 25)

**No Lombok.** All dependency injection uses explicit constructor injection. All fields are `final`.

```java
@Service
public class ExampleServiceImpl implements ExampleService {

  private static final Logger log = LoggerFactory.getLogger(ExampleServiceImpl.class);

  private final ExampleMapper exampleMapper;
  private final OtherService otherService;

  public ExampleServiceImpl(final ExampleMapper exampleMapper,
                            final OtherService otherService) {
    this.exampleMapper = exampleMapper;
    this.otherService = otherService;
  }
}
```

**Use `final` on parameters and local variables where practical:**

```java
public ExampleDto findById(final UUID id) {
  final var entity = exampleMapper.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Not found: " + id));
  return toDto(entity);
}
```

**Modern Java features in active use:**

```java
// Records for DTOs — grouped in a single *Dtos.java file per context
public class ExampleDtos {
  public record CreateRequest(@NotBlank String name, @Email String email) {}
  public record ExampleResponse(UUID id, String name, String email, Instant createdAt) {}
}

// Pattern matching
if (exception instanceof BadCredentialsException bce) {
  log.warn("Bad credentials: {}", bce.getMessage());
}

// Switch expressions
String label = switch (status) {
  case ACTIVE -> "Active";
  case LOCKED -> "Locked";
  case SUSPENDED -> "Suspended";
  default -> "Unknown";
};

// Text blocks
var sql = """
    SELECT * FROM users
    WHERE email = #{email}
    AND status = 'ACTIVE'
    """;

// var for obvious types
var membership = membershipMapper.findByUserAndTenant(userId, tenantKey);
```

**@ConfigurationProperties — use validated records, not classes:**

```java
@Validated
@ConfigurationProperties(prefix = "iqkv.example")
public record ExampleConfigurationProperties(
    @NotBlank String apiKey,
    @NotNull Duration tokenTtl,
    @Valid @NotNull Nested nested
) {
  public record Nested(@NotBlank String value) {}
}
```

#### Persistence Pattern (MyBatis)

**This project uses MyBatis, not Spring Data JPA.** Do not use `JpaRepository`, `@Entity`, `@Column`, or JPQL.

**Mapper interface** — annotated with `@Mapper`, resides in the bounded context package:

```java
@Mapper
public interface ExampleMapper {

  void insert(Example example);

  Optional<Example> findById(UUID id);

  List<Example> findAll(@Param("limit") int limit,
                        @Param("offset") int offset,
                        @Param("search") String search);

  long countAll(@Param("search") String search);

  void update(Example example);

  void deleteById(UUID id);
}
```

**XML mapper** — placed in `src/main/resources/mappers/<context>/ExampleMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.iqkv.foundation.iamservice.example.ExampleMapper">

  <resultMap id="ExampleResultMap" type="com.iqkv.foundation.iamservice.example.Example">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="createdAt" column="created_at"/>
  </resultMap>

  <select id="findById" resultMap="ExampleResultMap">
    SELECT id, name, created_at
    FROM examples
    WHERE id = #{id}
  </select>

  <insert id="insert">
    INSERT INTO examples (id, name, created_at)
    VALUES (#{id}, #{name}, #{createdAt})
  </insert>

</mapper>
```

**Underscore-to-camelCase mapping is globally enabled** (`mybatis.configuration.map-underscore-to-camel-case=true`), so `created_at` maps to `createdAt` automatically.

#### REST Controller Pattern

Controllers are named `*RestResource.java` (not `*Controller.java`). Every endpoint gets full OpenAPI annotations.

```java
@RestController
@RequestMapping("/api/v1/iam/examples")
@Tag(name = "Examples", description = "Example resource operations")
public class ExampleRestResource {

  private final ExampleService exampleService;

  public ExampleRestResource(final ExampleService exampleService) {
    this.exampleService = exampleService;
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get example by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Found"),
      @ApiResponse(responseCode = "404", description = "Not found")
  })
  public ResponseEntity<ExampleDtos.ExampleResponse> getById(
      @PathVariable final UUID id,
      @AuthenticationPrincipal final Jwt jwt) {
    return ResponseEntity.ok(exampleService.findById(id));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Create example")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Created"),
      @ApiResponse(responseCode = "400", description = "Validation error")
  })
  public ResponseEntity<ExampleDtos.ExampleResponse> create(
      @Valid @RequestBody final ExampleDtos.CreateRequest request) {
    final var response = exampleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
```

**Extracting the caller's identity from the JWT** (standard pattern in this project):

```java
// In controller method — extract userId and tenantKey from JWT claims
final UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
final String tenantKey = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
```

#### Tenant Context Pattern

Services that are tenant-scoped read the current tenant from `TenantContext`:

```java
// In a controller — set before delegating, clear in finally
@PostMapping("/signin")
public ResponseEntity<TokenResponse> signIn(
    @Valid @RequestBody final SignInRequest request,
    @RequestHeader("X-Tenant-ID") final String tenantKey) {
  try {
    TenantContext.setCurrentTenant(tenantKey);
    return ResponseEntity.ok(authenticationService.signIn(request));
  } finally {
    TenantContext.clear();
  }
}

// In a service — read the tenant set by the controller
final String tenantKey = TenantContext.getCurrentTenant();
```

#### Liquibase Migration Pattern

Migrations live in `src/main/resources/db/changelog/`. System-level tables go in `system/`, per-tenant tables in `tenant/`. Always reference the master changelog files rather than adding standalone scripts.

```xml
<!-- System changelog: db/changelog/system/db.changelog-system.xml -->
<changeSet id="020-add-examples-table" author="developer">
  <createTable tableName="examples">
    <column name="id" type="uuid">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="name" type="varchar(255)">
      <constraints nullable="false"/>
    </column>
    <column name="created_at" type="timestamptz"
            defaultValueComputed="NOW()">
      <constraints nullable="false"/>
    </column>
  </createTable>
</changeSet>
```

**ChangeSet ID format:** zero-padded sequential number + descriptive slug (e.g. `020-add-examples-table`).

#### Import Order (Checkstyle Enforced)

```java
// 1. Static imports — alphabetically sorted
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// 2. Jakarta / Java standard library — alphabetically sorted
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 3. Third-party / project libraries — alphabetically sorted
import com.iqkv.foundation.iamservice.shared.exception.UserNotFoundException;
import com.iqkv.foundation.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
```

**Import Rules:**

- Separate each group with a blank line
- Alphabetically sorted within each group
- No wildcard imports (`import java.util.*`)
- No unused imports
- No line-wrapped import or package statements

### Maven Command Best Practices for AI Agents

**Always skip Checkstyle during development iterations:**

```bash
# ✅ Development and testing
mvn clean verify -Dcheckstyle.skip=true
mvn test -Dcheckstyle.skip=true
mvn clean install -Dcheckstyle.skip=true

# ✅ Explicit style check when ready to commit
mvn checkstyle:check

# ❌ Avoid during active development — style failures block progress
mvn clean verify
```

```yaml
workflow:
    1. develop: "Implement with -Dcheckstyle.skip=true"
    2. test: "Iterate tests with -Dcheckstyle.skip=true"
    3. style: "Run mvn checkstyle:check in a focused pass"
    4. commit: "CI enforces Checkstyle automatically"
```

## 🔒 Security Guidelines

### Security Architecture Overview

This service is the authentication and identity authority for the platform. Any change touching auth, tokens, or access control must be treated as high-risk and reviewed carefully.

**Authentication flows implemented:**

| Flow                    | Endpoint                              | Notes                                                       |
| ----------------------- | ------------------------------------- | ----------------------------------------------------------- |
| Email + password signin | `POST /api/v1/iam/auth/signin`        | Tenant-scoped via `X-Tenant-ID` header                      |
| Platform admin signin   | `POST /api/v1/iam/auth/admin/signin`  | Requires `PLATFORM_ADMIN` platform authority                |
| Token refresh           | `POST /api/v1/iam/auth/refresh`       | Rotates both access + refresh tokens                        |
| Admin token refresh     | `POST /api/v1/iam/auth/admin/refresh` | Platform-scoped (no tenant_id in token)                     |
| Token exchange          | `POST /api/v1/iam/auth/exchange`      | Switch active tenant context                                |
| Signout                 | `POST /api/v1/iam/auth/signout`       | Revokes current JTI via denylist                            |
| Signout all             | `POST /api/v1/iam/auth/signout-all`   | Sets `last_global_signout_at`; invalidates all prior tokens |
| Token validation        | `POST /api/v1/iam/auth/validate`      | Used by other services to verify tokens                     |
| OAuth2 / OIDC           | `GET /api/v1/iam/auth/oauth2/*`       | Google, GitHub, Microsoft SSO                               |
| Magic link              | `POST /api/v1/iam/auth/magic-link/*`  | Initiate, resend, exchange                                  |
| Signup                  | `POST /api/v1/iam/auth/signup`        | Creates user + tenant atomically                            |

### JWT Token Design

- **Algorithm:** RS256 — tokens are signed with an RSA private key (`keys/private.pem`), verified with the public key (`keys/public.pem`)
- **Access token TTL:** 15 minutes (`iqkv.auth.jwt.expiry: PT15M`)
- **Refresh token TTL:** 7 days (`iqkv.auth.jwt.refresh-expiry: P7D`)
- **Issuer:** `foundation-iam-service`

**JWT claims in access tokens:**

```
sub           — user email
jti           — unique token ID (UUID, used for denylist revocation)
iss           — foundation-iam-service
iat / exp     — issued-at / expiry
type          — "access" or "refresh"
userId        — UUID
username      — display username
email         — user email
firstName     — first name
lastName      — last name
tenant_id     — tenantKey (null for platform admin tokens)
authorities   — list of authority strings (e.g. ["TENANT_OWNER", "ADMIN"])
email_verified — boolean
plan_code     — active billing plan code
onboarding_completed — boolean
profile_completed    — boolean
```

**Token revocation — two mechanisms:**

1. **JTI denylist** (Redis) — individual token revocation on signout; `JwtAuthenticationFilter` checks the JTI before every authenticated request
2. **Global signout timestamp** — `users.last_global_signout_at` in the DB; any token with `iat ≤ last_global_signout_at` is rejected, invalidating all previously issued tokens for that user

### Authentication Security Controls

**Brute-force lockout** (`AccountLockoutManager`):

- 5 failed login attempts triggers a lockout
- Lockout duration: 15 minutes (`iqkv.auth.security.rate-limiting.lockout-duration: PT15M`)
- Counter stored in Redis; reset on successful signin

**Account status checks** (applied in order during signin):

1. Tenant status — `SUSPENDED`, `DELETED`, `PROVISIONING_FAILED` block access
2. Account status — only `ACTIVE` users can sign in; `LOCKED` and other statuses are rejected
3. Ban check — `BanService` checks Redis/DB ban records before password validation
4. Lockout check — `AccountLockoutManager` checks Redis before password comparison
5. Password verification — BCrypt(12) via `PasswordEncoder.matches()`

**Password policy** (`iqkv.auth.security`):

- Minimum length: 8 characters
- BCrypt strength: 12 rounds

**Rate limits:**

- Password reset: 3 requests per 15-minute window
- Email verification resend: 3 resends per hour
- Magic link: 3 requests per 15-minute window

### Spring Security Configuration

Key decisions in `SecurityConfig`:

- Session policy: `STATELESS` — no server-side session
- CSRF: disabled (JWT-based, stateless)
- Public endpoints: actuator, API docs, signup, signin, email verify, password reset, magic link, OAuth2 flows, invitation accept, announcements, locales, JWKS
- `PLATFORM_ADMIN` authority required for `/api/v1/iam/admin/**`
- `TENANT_OWNER` or `ADMIN` required for tenant management endpoints
- All other requests require authentication
- `JwtAuthenticationConverter` maps the `authorities` claim to `GrantedAuthority` objects
- `JwtAuthenticationFilter` runs before `BearerTokenAuthenticationFilter` to enforce denylist + global signout

### Security Checklist

```yaml
authentication:
  - [ ] BCrypt(12) for all password storage — never plain text or weaker hash
  - [ ] RS256 JWT signed with RSA key pair — never HS256 with shared secret
  - [ ] Access token TTL remains short (≤ 15 min)
  - [ ] Refresh token rotation — new refresh token issued on every refresh
  - [ ] Account lockout applied before password check

authorization:
  - [ ] Method-level security with @PreAuthorize where endpoint-level is insufficient
  - [ ] Tenant isolation enforced — users can only access their own tenant's data
  - [ ] Platform admin endpoints require PLATFORM_ADMIN authority (not just any auth)
  - [ ] JWT tenant_id claim validated against X-Tenant-ID on token exchange/refresh

token_revocation:
  - [ ] Signout revokes JTI in Redis denylist
  - [ ] Signout-all updates last_global_signout_at — validate iat in JwtAuthenticationFilter
  - [ ] Token denylist TTL matches token expiry to prevent unbounded Redis growth

input_validation:
  - [ ] @Valid on all @RequestBody parameters
  - [ ] MyBatis parameterized queries — no string concatenation in SQL
  - [ ] No sensitive values (passwords, tokens) logged — use log IDs or masked values

data_protection:
  - [ ] Passwords never logged (log userId or email only)
  - [ ] JWT private key loaded from file path, never hardcoded
  - [ ] Secrets injected via environment variables (see .env.example)
  - [ ] .env.local is gitignored

secrets_management:
  - [ ] JWT keys: JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH env vars
  - [ ] DB credentials: DB_USERNAME / DB_PASSWORD
  - [ ] RabbitMQ: RABBITMQ_USERNAME / RABBITMQ_PASSWORD
  - [ ] Redis: REDIS_PASSWORD
  - [ ] OAuth2: OAUTH2_*_CLIENT_ID / OAUTH2_*_CLIENT_SECRET per provider
  - [ ] MinIO: OBJECTSTORAGE_ACCESS_KEY / OBJECTSTORAGE_SECRET_KEY
  - [ ] OIDC state encryption: OIDC_ENCRYPTION_KEY
```

### Security Testing Patterns

```java
@Test
@DisplayName("Should reject signin for locked account")
void shouldRejectSigninForLockedAccount() {
  // Arrange
  final var request = new AuthenticationDtos.SignInRequest("user@example.com", "password");
  final var user = buildUser(AccountStatus.LOCKED);
  when(tenantMapper.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(activeTenant()));
  when(userMapper.findByEmail("user@example.com")).thenReturn(Optional.of(user));

  // Act & Assert
  assertThatThrownBy(() -> authenticationService.signIn(request))
      .isInstanceOf(AccountLockedException.class);

  verifyNoInteractions(passwordEncoder);
}

@Test
@DisplayName("Should reject token refresh when token type is not refresh")
void shouldRejectRefreshWithAccessToken() {
  // Arrange
  final var jwt = buildJwt(Map.of(JwtClaimNames.TYPE, "access"));
  when(jwtDecoder.decode(anyString())).thenReturn(jwt);

  // Act & Assert
  assertThatThrownBy(() -> authenticationService.refresh(new RefreshTokenRequest("token")))
      .isInstanceOf(InvalidTokenTypeException.class);
}
```

## 🧪 Testing Standards

### Test Types and When to Use Each

| Type         | Annotation                            | Scope                             | When to use                               |
| ------------ | ------------------------------------- | --------------------------------- | ----------------------------------------- |
| Unit         | `@ExtendWith(MockitoExtension.class)` | Single class, all deps mocked     | Service logic, business rules, edge cases |
| Integration  | `@IntegrationTest`                    | Full Spring context, test profile | Multi-layer flows, DB queries, messaging  |
| Architecture | `@AnalyzeClasses` (ArchUnit)          | Bytecode analysis                 | Enforce package dependency rules          |
| Smoke        | `IamServiceApplicationTests`          | Context load only                 | Verify context starts without errors      |

### Unit Tests (AAA Pattern)

Use `@ExtendWith(MockitoExtension.class)`. Inject all dependencies via constructor — **do not use `@InjectMocks`**. Instantiate the class under test manually in `@BeforeEach`.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ExampleService Unit Tests")
class ExampleServiceImplTest {

  @Mock
  private ExampleMapper exampleMapper;
  @Mock
  private OtherService otherService;

  private ExampleServiceImpl exampleService;

  @BeforeEach
  void setUp() {
    exampleService = new ExampleServiceImpl(exampleMapper, otherService);
  }

  @Test
  @DisplayName("Should return example when found by ID")
  void shouldReturnExampleWhenFoundById() {
    // Arrange
    final var id = UUID.randomUUID();
    final var example = buildExample(id);
    when(exampleMapper.findById(id)).thenReturn(Optional.of(example));

    // Act
    final var result = exampleService.findById(id);

    // Assert
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.name()).isEqualTo(example.getName());
    verify(exampleMapper).findById(id);
  }

  @Test
  @DisplayName("Should throw when example not found")
  void shouldThrowWhenNotFound() {
    // Arrange
    final var id = UUID.randomUUID();
    when(exampleMapper.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> exampleService.findById(id))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
```

**Key rules for unit tests:**

- `@DisplayName` on every test — describe the business expectation, not the method call
- Use `assertThatThrownBy` (AssertJ) for exception assertions — not `assertThrows`
- Use `lenient()` for stubs that may not be called in all test paths
- Use `verifyNoInteractions(mock)` to assert a dependency was never touched
- Never mock `TenantContext` — call `TenantContext.setCurrentTenant("test-key")` directly in arrange and clean up with `TenantContext.clear()` in `@AfterEach`

### Integration Tests

Use the `@IntegrationTest` composite annotation. It combines `@SpringBootTest(classes = IamServiceApplication.class)` and `@ActiveProfiles("test")`.

```java
@IntegrationTest
class ExampleServiceIntegrationTest {

  @Autowired
  private ExampleService exampleService;

  @Autowired
  private ExampleMapper exampleMapper;

  @Test
  @DisplayName("Should persist and retrieve example")
  void shouldPersistAndRetrieveExample() {
    // Arrange
    final var request = new ExampleDtos.CreateRequest("test-name", "test@example.com");

    // Act
    final var created = exampleService.create(request);

    // Assert
    assertThat(created.id()).isNotNull();
    final var found = exampleMapper.findById(created.id());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("test-name");
  }
}
```

The `test` profile (`application-test.yml`) configures H2 in-memory database and disables external dependencies (RabbitMQ, Redis, MinIO) where possible via test stubs or Testcontainers.

**When to add Testcontainers:** For tests that require real PostgreSQL behaviour (jsonb columns, advisory locks, Liquibase migrations). Use the existing `@Container` pattern:

```java
@IntegrationTest
@Testcontainers
class ExampleRepositoryIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine");

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
}
```

RabbitMQ integration tests use `spring-rabbit-test` (`@RabbitListenerTest`, `RabbitListenerTestHarness`) — no Testcontainers needed for basic messaging assertions.

### Architecture Tests (ArchUnit)

`TechnicalStructureTest` enforces the onion architecture using ArchUnit's built-in `onionArchitecture()` rule. It runs against all non-test classes in the `com.iqkv.foundation.iamservice` package.

```java
@AnalyzeClasses(packagesOf = IamServiceApplication.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers = onionArchitecture()
      .withOptionalLayers(true)
      .ignoreDependency(belongToAnyOf(IamServiceApplication.class), alwaysTrue());
}
```

**What this enforces:** Domain classes must not depend on application or infrastructure classes. Application classes must not depend on infrastructure. Infrastructure can depend on everything. The `withOptionalLayers(true)` flag means not every context needs all layers.

Run architecture tests in isolation:

```bash
mvn test -Dtest=TechnicalStructureTest -Dcheckstyle.skip=true
```

### MyBatis Mapper Tests

Use `@MybatisTest` for isolated mapper tests against H2 or Testcontainers:

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ExampleMapperTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine");

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private ExampleMapper exampleMapper;

  @Test
  @DisplayName("Should find example by ID")
  void shouldFindById() {
    // test uses data inserted via @Sql or direct mapper insert
  }
}
```

### Quality Gates

```yaml
test_coverage:
    minimum_threshold: ">= 60%" # JaCoCo enforces this; build fails below threshold
    target: ">= 80%"
    report: "target/site/jacoco/index.html"

code_style:
    tool: "Checkstyle"
    enforcement: "mvn checkstyle:check — build fails on violations"

architecture:
    tool: "ArchUnit"
    test: "TechnicalStructureTest"
    rule: "onionArchitecture() — domain must not depend on infrastructure"
```

```bash
# Run all tests
mvn test -Dcheckstyle.skip=true

# Run a specific test class
mvn test -Dtest=AuthenticationServiceImplTest -Dcheckstyle.skip=true

# Run architecture tests only
mvn test -Dtest=TechnicalStructureTest -Dcheckstyle.skip=true

# Generate coverage report
mvn test jacoco:report -Dcheckstyle.skip=true
# Open: target/site/jacoco/index.html

# Full verify with all checks (pre-commit)
mvn clean verify
```

## 🔄 Workflow Management

### Pull Request Guidelines

#### PR Title Format

```
type(scope): description

Examples:
feat(auth): add magic link authentication flow
fix(lockout): reset counter after successful admin signin
improvement(membership): add index on tenant_memberships.user_id
refactor(denylist): extract JTI check into dedicated method
```

#### PR Description Template

```markdown
## Description

Brief description of changes and motivation.

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Changes Made

- List specific changes
- Include affected bounded contexts

## How Has This Been Tested?

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Test Coverage

- Current coverage: X%
- Coverage change: +/-X%

## Checklist

- [ ] Code follows style guidelines (mvn checkstyle:check passes)
- [ ] Commit messages follow Conventional Commits
- [ ] Self-review completed
- [ ] Tests cover new/modified code (≥ 60% threshold maintained)
- [ ] All tests pass locally
- [ ] Liquibase changeSet added for schema changes (correct changelog: system vs tenant)
- [ ] No sensitive data in logs

## Security Considerations

- [ ] No passwords, tokens, or secrets logged
- [ ] Input validation with @Valid on all request bodies
- [ ] Authorization checks in place (@PreAuthorize or SecurityConfig rules)
- [ ] Tenant isolation maintained

## Additional Notes
```

#### Review Criteria

**Automated Checks (Must Pass):**

- ✅ All tests pass
- ✅ Checkstyle validation passes
- ✅ JaCoCo coverage ≥ 60%
- ✅ ArchUnit `TechnicalStructureTest` passes
- ✅ Commit messages follow Conventional Commits
- ✅ No merge conflicts

**Manual Review Focus:**

```yaml
review_checklist:
    code_quality:
        - Constructor injection used (no Lombok, no @InjectMocks)
        - final on fields and parameters
        - Modern Java features used (records, var, switch expressions)
        - No wildcard imports, no unused imports

    persistence:
        - MyBatis @Mapper interface + XML mapper (not JPA)
        - Liquibase changeSet added for any schema change
        - Correct changelog (system/ vs tenant/) for new tables
        - Parameterized queries — no SQL string concatenation

    security:
        - Passwords never logged
        - New endpoints added to SecurityConfig with correct authority rules
        - Tenant isolation enforced for tenant-scoped data
        - Rate limiting considered for new public endpoints

    testing:
        - AAA pattern in unit tests
        - @DisplayName on every test method
        - Edge cases and failure paths covered
        - @IntegrationTest used for multi-layer tests

    documentation:
        - Full OpenAPI annotations on new endpoints (@Tag, @Operation, @ApiResponses)
        - @SecurityRequirement(name = "bearerAuth") on authenticated endpoints
        - X-Tenant-ID @Parameter documented where applicable
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow

The existing workflows in `.github/workflows/` handle:

- `check-commit-message.yml` — validates Conventional Commits format
- `check-pr-title.yml` — validates PR title format
- `check-markdown-links.yml` — validates links in markdown files
- `auto-approve-dependabot-pr.yml` — auto-approves Dependabot dependency updates
- `build-nodejs-project.yml` — Node.js build (for any front-end tooling)

**Recommended Java CI workflow** (add or update for the Maven build):

```yaml
name: CI

on:
    push:
        branches: [main, develop]
    pull_request:
        branches: [main, develop]

jobs:
    build-and-test:
        runs-on: ubuntu-latest

        steps:
            - uses: actions/checkout@v4

            - name: Set up JDK 25
              uses: actions/setup-java@v4
              with:
                  java-version: "25"
                  distribution: "temurin"
                  cache: "maven"

            - name: Build and Test
              run: mvn clean verify

            - name: Upload Coverage Report
              uses: codecov/codecov-action@v3
              with:
                  files: target/site/jacoco/jacoco.xml
```

`mvn clean verify` (without `-Dcheckstyle.skip`) is the correct gate for CI — style, tests, and coverage all enforced together.

## 🐳 Local Development Environment

### Docker Compose Stack

`compose.yaml` brings up the full local stack:

| Service    | Port         | Purpose                                |
| ---------- | ------------ | -------------------------------------- |
| PostgreSQL | 5432         | Primary database                       |
| RabbitMQ   | 5672 / 15672 | Messaging broker / management UI       |
| Redis      | 6379         | Caching, denylist, rate-limit counters |
| MailHog    | 1025 / 8025  | Local SMTP / email UI                  |
| MinIO      | 9000 / 9001  | Object storage / console               |
| DBGate     | 3000         | Database browser UI                    |

```bash
# Start infrastructure only
docker compose -f compose.base.yaml up -d

# Start full stack including app container
docker compose -f compose.container.yaml up -d

# Start default (all services)
docker compose up -d
```

### Running the Application Locally

```bash
# 1. Start infrastructure
docker compose -f compose.base.yaml up -d

# 2. Copy and configure environment
cp .env.example .env.local
# Edit .env.local — set DB credentials, RabbitMQ, Redis, JWT key paths, etc.

# 3. Run with local profile (enables spring-boot-devtools)
mvn spring-boot:run -Plocal -Dspring.profiles.active=local

# App runs on: http://localhost:8080
# Swagger UI:  http://localhost:8080/swagger-ui.html
# Actuator:    http://localhost:8081/actuator/health
# Prometheus:  http://localhost:8081/actuator/prometheus
```

### Environment Variables Reference

Key variables — see `.env.example` for the full list:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=iamservice
DB_USERNAME=svc_iam_dba
DB_PASSWORD=svc_iam_dba

# JWT (RS256 key pair — generate with openssl)
JWT_PRIVATE_KEY_PATH=classpath:keys/private.pem
JWT_PUBLIC_KEY_PATH=classpath:keys/public.pem

# Platform mode
ROLLOUT_MODE=MULTI_TENANT          # or SINGLE_TENANT

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=svc_iam_rmq
RABBITMQ_PASSWORD=svc_iam_rmq

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Object storage (MinIO)
OBJECTSTORAGE_ENDPOINT=http://localhost:9000
OBJECTSTORAGE_ACCESS_KEY=iqkv
OBJECTSTORAGE_SECRET_KEY=iqkv_password

# OAuth2 SSO (set per-provider)
OAUTH2_ENABLED_PROVIDERS=google,github
OAUTH2_GOOGLE_CLIENT_ID=
OAUTH2_GOOGLE_CLIENT_SECRET=
OAUTH2_GITHUB_CLIENT_ID=
OAUTH2_GITHUB_CLIENT_SECRET=
```

## 🎯 Agent Decision Framework

### User Confirmation Policy

**CRITICAL RULE: Always ask for user confirmation before applying changes.**

```yaml
before_making_changes:
    1. analyze: "Understand the request and identify required changes"
    2. explain: "Describe what changes will be made and why"
    3. assess_impact: "Evaluate impact, effort, and risk"
    4. present_options: "Offer alternatives if applicable"
    5. wait_for_approval: "STOP and wait for explicit user confirmation"
    6. apply_changes: "Only after user approves"
    7. verify: "Run tests and confirm changes work"

exceptions:
    - read_only_operations: "Reading files, searching, analyzing"
    - information_requests: "Answering questions, explaining concepts"
    - recommendations: "Suggesting approaches without implementing"

never_auto_apply:
    - code_changes: "Any modification to source files"
    - configuration_changes: "application.yml, pom.xml, etc."
    - dependency_updates: "Adding or updating dependencies"
    - schema_changes: "Liquibase migrations"
    - security_changes: "SecurityConfig, JWT config, auth logic"
    - deletions: "Removing files or code"
```

### When to Intervene

**High Priority (Immediate — Still Requires Approval):**

- 🚨 Security vulnerabilities (auth bypass, token leakage, missing authorization)
- 🔴 Build failures blocking development
- 💥 Critical bugs in signin, token refresh, or tenant isolation

**Medium Priority (Plan and Execute — Requires Approval):**

- 📉 Test coverage drop below 60%
- ❌ ArchUnit violations (onion architecture broken)
- 📦 Dependency updates needed

**Low Priority (Continuous Improvement — Requires Approval):**

- 📝 Missing OpenAPI documentation on endpoints
- ♻️ Refactoring opportunities
- 🧪 Test coverage improvements toward 80% target

### Decision Matrix

```yaml
impact_assessment:
    critical: "Auth bypass, data leakage, tenant isolation broken"
    high: "Signin broken, token refresh failing, signup broken"
    medium: "Non-critical feature broken, degraded performance"
    low: "Internal improvement, no user-visible impact"

effort_estimation:
    small: "Single service method or mapper query"
    medium: "New bounded context feature (service + mapper + controller + tests)"
    large: "Cross-context feature or schema migration with data backfill"
    xlarge: "Platform-wide change (rollout mode, multi-tenancy model)"

risk_evaluation:
    low: "New feature in isolated context, easy rollback"
    medium: "Auth flow change, new Liquibase migration"
    high: "SecurityConfig change, JWT claim modification"
    critical: "Token format change (breaks existing tokens), schema rename"
```

### Common Patterns and Solutions

**Pattern: Adding a new feature to an existing bounded context**

```
1. Add Liquibase changeSet to the correct changelog (system/ or tenant/)
2. Add MyBatis mapper method to the @Mapper interface
3. Add XML mapping in src/main/resources/mappers/<context>/
4. Implement or extend the *Service / *ServiceImpl
5. Add endpoint to the *RestResource with full OpenAPI annotations
6. Add unit tests (MockitoExtension, constructor injection, AAA pattern)
7. Run: mvn test -Dcheckstyle.skip=true
8. Run: mvn checkstyle:check
```

**Pattern: Adding a new bounded context**

```
1. Create package: com.iqkv.foundation.iamservice.<context>/
2. Add domain model class(es)
3. Add @Mapper interface + XML mapper file
4. Add *Service interface + *ServiceImpl
5. Add *RestResource controller
6. Add *Dtos.java with nested request/response records
7. Add Liquibase changeSet for new tables
8. Register new public endpoints in SecurityConfig if needed
9. Add unit tests in src/test/.../  <context>/
10. Verify ArchUnit still passes: mvn test -Dtest=TechnicalStructureTest
```

**Pattern: Adding an OAuth2/OIDC provider**

```
1. Add Spring OAuth2 client registration in application.yml
   (spring.security.oauth2.client.registration.<provider>)
2. Add provider config to iqkv.auth.oauth2.providers.<provider>
3. Add OAUTH2_<PROVIDER>_CLIENT_ID/SECRET to .env.example
4. Update OAUTH2_ENABLED_PROVIDERS documentation
5. Handle provider-specific user info mapping in the oauth2 package
```

---

This document reflects the actual implementation of `foundation-iam-service`. Keep it updated when adding new bounded contexts, changing the technology stack, or introducing new architectural patterns.
