# Foundation IAM Service 🔐

Identity and Access Management microservice for the Key Value Platform. Owns user accounts, tenant lifecycle, memberships, and RS256 token issuance. Handles the full identity lifecycle — signup, authentication, token management, tenant provisioning, and brute-force protection.

## About

The IAM service is the identity backbone of the platform:

- **Self-service onboarding** — a single signup call creates the user account and provisions a new tenant in one step; the caller receives a `tenantKey` and polls for `ACTIVE` status
- **Signup by invitation** — existing `TENANT_OWNER` or `ADMIN` sends a time-limited invite (72 h, default authority `MEMBER`); new users are created on accept with email pre-verified; existing users verify identity by password
- **Multi-tenancy** — users are global identities; isolation is enforced through `TenantMembership` records that carry per-tenant authorities (`TENANT_OWNER`, `PLATFORM_ADMIN`, `MEMBER`)
- **JWT RS256 authentication** — 15-minute access tokens and 7-day refresh tokens, both carrying `tenant_id`; every request is validated against a JTI denylist and a global signout timestamp
- **Tenant lifecycle** — owners can suspend, delete, or retry failed provisioning; a ShedLock-guarded reaper job cleans up stuck `PROVISIONING` tenants automatically
- **Brute-force protection** — failed login attempts are tracked per email; accounts are temporarily locked after 5 attempts for 15 minutes
- **Token revocation** — single-session signout (JTI denylist) and global signout (`last_global_signout_at`) are both supported
- **Password reset** — time-limited reset tokens (1 hour TTL) with rate limiting (3 requests per 15-minute window)
- **Email notifications** — Thymeleaf-rendered transactional emails via SMTP
- **Platform rollout mode** — publishes canonical `platform.rollout-mode` via `/actuator/info` for gateway and billing service consistency checks

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/iam`

### Authentication — `/api/v1/iam/auth`

| Method | Path                | Auth                   | Description                                                                         |
| ------ | ------------------- | ---------------------- | ----------------------------------------------------------------------------------- |
| `POST` | `/auth/signup`      | public                 | Register user + create tenant (multi-tenant) or join default tenant (single-tenant) |
| `POST` | `/auth/signin`      | public + `X-Tenant-ID` | Sign in; returns RS256 access + refresh token pair                                  |
| `POST` | `/auth/refresh`     | public                 | Exchange refresh token for a new token pair                                         |
| `POST` | `/auth/signout`     | JWT                    | Revoke current token (JTI denylist)                                                 |
| `POST` | `/auth/signout-all` | JWT                    | Invalidate all sessions via `last_global_signout_at`                                |
| `POST` | `/auth/validate`    | JWT                    | Validate token and return user context (gateway introspection)                      |

### User — `/api/v1/iam/users`

| Method   | Path                               | Auth                      | Description                                  |
| -------- | ---------------------------------- | ------------------------- | -------------------------------------------- |
| `GET`    | `/users/me`                        | JWT + `X-Tenant-ID`       | Get current user profile                     |
| `PATCH`  | `/users/me`                        | JWT + `X-Tenant-ID`       | Update current user profile                  |
| `DELETE` | `/users/me`                        | JWT + `X-Tenant-ID`       | Remove current user's membership from tenant |
| `POST`   | `/users/tenants`                   | public (credential-gated) | Discover tenants for a user                  |
| `POST`   | `/users/email/verify`              | public                    | Verify email address via one-time token      |
| `POST`   | `/users/email/resend-verification` | public                    | Resend email verification                    |
| `POST`   | `/users/password/forgot`           | public                    | Initiate password reset (rate-limited)       |
| `POST`   | `/users/password/reset`            | public                    | Complete password reset with token           |

### User Admin — `/api/v1/iam/admin/users`

| Method   | Path                | Auth                 | Description                         |
| -------- | ------------------- | -------------------- | ----------------------------------- |
| `GET`    | `/admin/users`      | JWT `PLATFORM_ADMIN` | List users (paginated)              |
| `GET`    | `/admin/users/{id}` | JWT `PLATFORM_ADMIN` | Get user by UUID                    |
| `POST`   | `/admin/users`      | JWT `PLATFORM_ADMIN` | Create user with temporary password |
| `PUT`    | `/admin/users/{id}` | JWT `PLATFORM_ADMIN` | Replace user (full update)          |
| `PATCH`  | `/admin/users/{id}` | JWT `PLATFORM_ADMIN` | Partially update user               |
| `DELETE` | `/admin/users/{id}` | JWT `PLATFORM_ADMIN` | Delete user and all memberships     |

### Tenant — `/api/v1/iam/tenants`

| Method  | Path                                      | Auth                               | Description               |
| ------- | ----------------------------------------- | ---------------------------------- | ------------------------- |
| `GET`   | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER` + `X-Tenant-ID` | Get tenant details        |
| `PATCH` | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER` + `X-Tenant-ID` | Update tenant status      |
| `POST`  | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Retry failed provisioning |

### Invitations

| Method   | Path                                    | Auth                                                | Description                                    |
| -------- | --------------------------------------- | --------------------------------------------------- | ---------------------------------------------- |
| `POST`   | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` + `X-Tenant-ID` | Send invitation email                          |
| `GET`    | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` + `X-Tenant-ID` | List pending invitations                       |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` + `X-Tenant-ID` | Revoke invitation                              |
| `GET`    | `/invitations/{token}`                  | public                                              | Preview invitation (tenant name, role, expiry) |
| `POST`   | `/invitations/{token}/accept`           | public                                              | Accept invitation; returns token pair          |

> Auth legend: `public` = no token required; `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char alphanumeric tenantKey header required for tenant-scoped endpoints.

JWKS endpoint (public, consumed by the gateway): `GET /.well-known/jwks.json`

## Tech Stack

- Java 25 / Spring Boot 4.0
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase (system + per-tenant schema migrations)
- RabbitMQ (async tenant provisioning events)
- JJWT 0.13 RS256 (token issuance and validation)
- ShedLock 7.x (distributed scheduled cleanup jobs)
- Thymeleaf (transactional email templates)
- Micrometer + Prometheus
- springdoc-openapi (Swagger UI)

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.2 (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/IQKV/foundation-iam-service.git
cd foundation-iam-service

# Install git hooks
pnpm install

# Copy environment variables
cp .env.example .env.local
# Edit .env.local — defaults work for local Docker setup

# Start dependencies (PostgreSQL on :5432, RabbitMQ on :5672/:15672, MailHog on :1025/:8025)
docker compose up -d

# Run the service
export $(grep -v '^#' .env.local | xargs)
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
# → MailHog:  http://localhost:8025
```

## Environment Variables

| Variable               | Default                      | Description                                      |
| ---------------------- | ---------------------------- | ------------------------------------------------ |
| `ROLLOUT_MODE`         | `MULTI_TENANT`               | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT` |
| `DB_HOST`              | `localhost`                  | PostgreSQL host                                  |
| `DB_PORT`              | `5432`                       | PostgreSQL port                                  |
| `DB_NAME`              | `iam`                        | Database name                                    |
| `DB_USERNAME`          | `iam`                        | Database user                                    |
| `DB_PASSWORD`          | `iam`                        | Database password                                |
| `RABBITMQ_HOST`        | `localhost`                  | RabbitMQ host                                    |
| `RABBITMQ_PORT`        | `5672`                       | RabbitMQ AMQP port                               |
| `RABBITMQ_USERNAME`    | `iam`                        | RabbitMQ user                                    |
| `RABBITMQ_PASSWORD`    | `iam`                        | RabbitMQ password                                |
| `MAIL_HOST`            | `localhost`                  | SMTP host                                        |
| `MAIL_PORT`            | `587`                        | SMTP port                                        |
| `MAIL_FROM`            | `noreply@iqkv.com`           | Sender address                                   |
| `JWT_PRIVATE_KEY_PATH` | `classpath:keys/private.pem` | RS256 private key                                |
| `JWT_PUBLIC_KEY_PATH`  | `classpath:keys/public.pem`  | RS256 public key                                 |
| `APP_BASE_URL`         | `http://localhost:3000`      | Frontend base URL (used in email links)          |
| `DEFAULT_TENANT_KEY`   | _(empty)_                    | Default tenant key for `SINGLE_TENANT` mode      |
| `DEFAULT_TENANT_NAME`  | `Acme Corp.`                 | Default tenant display name                      |
| `INVITATION_TOKEN_TTL` | `PT72H`                      | Invitation token lifetime (ISO-8601 duration)    |

> Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd` and fill in values per environment.

## Maven Commands

```bash
# Build and test (skip Checkstyle during development)
./mvnw clean verify -Dcheckstyle.skip=true

# Run tests only
./mvnw test -Dcheckstyle.skip=true

# Explicit Checkstyle check
./mvnw checkstyle:check

# Coverage report → target/site/jacoco/index.html
./mvnw jacoco:report

# Production build
./mvnw clean package -Pproduction
```

## Docker

```bash
# Build image
docker build -t iqkv/foundation-iam-service:latest .

# Run full stack (service + dependencies)
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                     | Description                                                         |
| ---------------------------- | ------------------------------------------------------------------- |
| `GET /actuator/health`       | Liveness + readiness probes; includes `PlatformModeHealthIndicator` |
| `GET /actuator/info`         | Build info + canonical `platform.rollout-mode`                      |
| `GET /actuator/metrics`      | Application metrics                                                 |
| `GET /actuator/prometheus`   | Prometheus scrape endpoint                                          |
| `GET /swagger-ui.html`       | API documentation                                                   |
| `GET /.well-known/jwks.json` | Public JWK Set for JWT validation                                   |

## Project Structure

```
src/main/java/com/iqkv/foundation/iamservice/
├── authentication/   # Signin, signup, token refresh, signout, validate
├── denylist/         # JTI denylist for token revocation
├── email/            # Email verification token + reaper job
├── invitation/       # Token-based invite flow + reaper job
├── lockout/          # Brute-force login lockout per identity
├── membership/       # TenantMembership, authorities (TENANT_OWNER, ADMIN, MEMBER)
├── passwordreset/    # Forgot/reset password flow + reaper job
├── security/         # JWT authentication filter, claim names, JWKS endpoint
├── signup/           # SignupStrategy (multi-tenant / single-tenant)
├── tenant/           # Tenant lifecycle, provisioning, bootstrap strategies
├── tenancy/          # MyBatis schema interceptor, TenantContext, Liquibase runner
├── user/             # User profile, admin CRUD
├── shared/           # Common exceptions, utilities, value objects
└── infrastructure/   # Spring config, security, persistence, messaging, metrics
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).

---

## 🧩 Boilerplate Architecture

- **Persistence**: MyBatis with XML mappers + PostgreSQL; Liquibase manages both system and per-tenant schema migrations; `is_default` column on `public.tenants` marks the single-mode default tenant with a partial unique index
- **Messaging**: RabbitMQ for async domain events (tenant provisioning); ShedLock guards scheduled cleanup jobs (expired tokens, stuck tenants)
- **Security**: Spring Security + JJWT RS256; JTI denylist for token revocation; `last_global_signout_at` for session-wide invalidation; brute-force lockout per identity
- **Multi-tenancy**: Per-tenant schema isolation managed by Liquibase; `TenantMembership` carries per-tenant authorities (`TENANT_OWNER`, `PLATFORM_ADMIN`, `MEMBER`); tenant-scoped endpoints require a JWT scoped to the target tenant via `X-Tenant-ID` header
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; IAM publishes canonical mode via `/actuator/info` under `platform.rollout-mode`; service fails readiness on invalid/missing mode
- **Single-tenant mode**: Strategy pattern (`SignupStrategy`, `TenantBootstrapStrategy`) branches behavior at startup and signup; `SingleTenantBootstrapStrategy` idempotently provisions the default tenant on `ApplicationReadyEvent`; `SingleTenantSignupStrategy` joins users to the default tenant with `MEMBER` authority — no tenant creation, no `TENANT_OWNER` grant
- **Invitations**: Token-based signup-by-invitation flow; token resolves tenant context so accept endpoints are tenant-agnostic; `authority` defaults to `MEMBER`; ShedLock-guarded reaper expires stale tokens
- **Email**: Thymeleaf-rendered transactional emails via Spring Mail; MailHog for local testing
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes; `PlatformModeHealthIndicator` exposes rollout mode in `/actuator/health`; `PlatformModeInfoContributor` publishes canonical `platform.rollout-mode` in `/actuator/info`
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (60% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
