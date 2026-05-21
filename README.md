> ## 🤔 What is this service all about?
>
> - Multi-tenant Identity and Access Management microservice for the IQ Key Value platform.
> - A single user account can belong to multiple tenants with different authortities in each.
> - Make the project easy to maintain with **8 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...

---

# 🔐 IQ Key Value IAM Service

Multi-tenant Identity and Access Management microservice. Handles the full identity lifecycle — signup, authentication, token management, tenant provisioning, and brute-force protection.

## About

The IAM service is the identity backbone of the platform:

- **Self-service onboarding** — a single signup call creates the user account and provisions a new tenant in one step; the caller receives a `tenantKey` and polls for `ACTIVE` status
- **Signup by invitation** — existing `TENANT_OWNER` or `ADMIN` sends a time-limited invite (72 h, default authority `MEMBER`); new users are created on accept with email pre-verified; existing users verify identity by password
- **Multi-tenancy** — users are global identities; isolation is enforced through `TenantMembership` records that carry per-tenant authorities (`TENANT_OWNER`, `ADMIN`, `MEMBER`)
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

| Method | Path                              | Auth                   | Description                                                                         |
| ------ | --------------------------------- | ---------------------- | ----------------------------------------------------------------------------------- |
| `POST` | `/auth/signup`                    | public                 | Register user + create tenant (multi-tenant) or join default tenant (single-tenant) |
| `GET`  | `/auth/signup/status/{tenantKey}` | public                 | Poll tenant provisioning status after signup                                        |
| `POST` | `/auth/signin`                    | public + `X-Tenant-ID` | Sign in; returns RS256 access + refresh token pair                                  |
| `POST` | `/auth/exchange`                  | JWT                    | Exchange a Bearer access token for a new tenant-scoped token pair                   |
| `POST` | `/auth/admin/signin`              | public                 | Platform admin sign-in; returns platform-scoped token pair (null `tenant_id`)       |
| `POST` | `/auth/admin/refresh`             | public                 | Refresh platform-scoped token pair                                                  |
| `POST` | `/auth/refresh`                   | public                 | Exchange refresh token for a new token pair                                         |
| `POST` | `/auth/signout`                   | JWT                    | Revoke current token (JTI denylist)                                                 |
| `POST` | `/auth/signout-all`               | JWT                    | Invalidate all sessions via `last_global_signout_at`                                |
| `POST` | `/auth/validate`                  | JWT                    | Validate token and return user context (gateway introspection)                      |

### Platform Admin Account — `/api/v1/iam/auth/admin/me`

| Method  | Path                      | Auth                 | Description                                                               |
| ------- | ------------------------- | -------------------- | ------------------------------------------------------------------------- |
| `GET`   | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Get own platform operator profile                                         |
| `PATCH` | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Update own first/last name                                                |
| `POST`  | `/auth/admin/me/password` | JWT `PLATFORM_ADMIN` | Change own password (requires current password; invalidates all sessions) |

### User — `/api/v1/iam/users`

| Method   | Path                               | Auth                      | Description                                                               |
| -------- | ---------------------------------- | ------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/users/me`                        | JWT + `X-Tenant-ID`       | Get current user profile                                                  |
| `PATCH`  | `/users/me`                        | JWT + `X-Tenant-ID`       | Update current user profile                                               |
| `DELETE` | `/users/me`                        | JWT + `X-Tenant-ID`       | Remove current user's membership from tenant                              |
| `POST`   | `/users/me/password`               | JWT + `X-Tenant-ID`       | Change own password (requires current password; invalidates all sessions) |
| `POST`   | `/users/tenants`                   | public (credential-gated) | Discover tenants for a user                                               |
| `GET`    | `/users/me/memberships`            | JWT                       | List current user's tenant memberships (for org/tenant picker UIs)        |
| `POST`   | `/users/email/verify`              | public                    | Verify email address via one-time token                                   |
| `POST`   | `/users/email/resend-verification` | public                    | Resend email verification (rate-limited)                                  |
| `POST`   | `/users/password/forgot`           | public                    | Initiate password reset (rate-limited)                                    |
| `POST`   | `/users/password/reset`            | public                    | Complete password reset with token                                        |

### User Admin — `/api/v1/iam/admin/users`

| Method   | Path                            | Auth                 | Description                                        |
| -------- | ------------------------------- | -------------------- | -------------------------------------------------- |
| `GET`    | `/admin/users`                  | JWT `PLATFORM_ADMIN` | List users (paginated, filterable)                 |
| `GET`    | `/admin/users/count`            | JWT `PLATFORM_ADMIN` | Total user count                                   |
| `GET`    | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Get user by UUID                                   |
| `POST`   | `/admin/users`                  | JWT `PLATFORM_ADMIN` | Create user with random temporary password         |
| `PUT`    | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Replace user (full update)                         |
| `PATCH`  | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Partially update user                              |
| `DELETE` | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Delete user and all memberships                    |
| `GET`    | `/admin/users/{id}/authorities` | JWT `PLATFORM_ADMIN` | Get user platform authorities                      |
| `PUT`    | `/admin/users/{id}/authorities` | JWT `PLATFORM_ADMIN` | Replace user platform authorities                  |
| `GET`    | `/admin/users/{id}/memberships` | JWT `PLATFORM_ADMIN` | Get user tenant memberships                        |
| `POST`   | `/admin/users/{id}/password`    | JWT `PLATFORM_ADMIN` | Force-set user password (invalidates all sessions) |

### Tenant — `/api/v1/iam/tenants`

| Method  | Path                                      | Auth                               | Description               |
| ------- | ----------------------------------------- | ---------------------------------- | ------------------------- |
| `GET`   | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER` + `X-Tenant-ID` | Get tenant details        |
| `PATCH` | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER` + `X-Tenant-ID` | Update tenant status      |
| `POST`  | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Retry failed provisioning |

### Tenant Admin — `/api/v1/iam/admin/tenants`

| Method   | Path                                                      | Auth                 | Description                                 |
| -------- | --------------------------------------------------------- | -------------------- | ------------------------------------------- |
| `GET`    | `/admin/tenants`                                          | JWT `PLATFORM_ADMIN` | List tenants (paginated, filterable)        |
| `GET`    | `/admin/tenants/count`                                    | JWT `PLATFORM_ADMIN` | Total tenant count                          |
| `GET`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Get tenant by key                           |
| `PUT`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Rename tenant                               |
| `PATCH`  | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Partially update tenant                     |
| `DELETE` | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Delete tenant and all associated data       |
| `GET`    | `/admin/tenants/{tenantKey}/members`                      | JWT `PLATFORM_ADMIN` | List tenant members (paginated, filterable) |
| `GET`    | `/admin/tenants/{tenantKey}/members/count`                | JWT `PLATFORM_ADMIN` | Total member count for tenant               |
| `GET`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Get member's tenant authorities             |
| `PUT`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Replace member's tenant authorities         |

### Invitations

| Method   | Path                                    | Auth                                       | Description                                         |
| -------- | --------------------------------------- | ------------------------------------------ | --------------------------------------------------- |
| `POST`   | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Send invitation email                               |
| `GET`    | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | List pending invitations                            |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}` | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Revoke invitation                                   |
| `GET`    | `/invitations/{token}`                  | public                                     | Preview invitation (tenant name, authority, expiry) |
| `POST`   | `/invitations/{token}/accept`           | public                                     | Accept invitation; returns token pair               |

### Invitation Admin — `/api/v1/iam/admin/invitations`

| Method   | Path                       | Auth                 | Description                                                 |
| -------- | -------------------------- | -------------------- | ----------------------------------------------------------- |
| `GET`    | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | List invitations across all tenants (paginated, filterable) |
| `GET`    | `/admin/invitations/count` | JWT `PLATFORM_ADMIN` | Total invitation count (with optional filters)              |
| `GET`    | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Get invitation by UUID                                      |
| `POST`   | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | Propose invitation for any active tenant                    |
| `DELETE` | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Revoke invitation                                           |

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

## Observability

The service provides comprehensive monitoring via Micrometer and Prometheus:

- **Custom Metrics**:
    - `iam.auth.outcome`: Authentication success/failure rates by reason and tenant.
    - `iam.auth.duration`: Latency percentiles for login and refresh operations.
    - `iam.security.event`: Security-sensitive triggers (lockouts, validation failures).
    - `iam.user.lifecycle`: Velocity of registrations and password resets.
    - `iam.tenant.provisioning`: Success rate and duration of tenant database migrations.
    - `iam.messaging.publish`: Health of RabbitMQ event publication.
- **Grafana Dashboards**: Pre-configured dashboards are available in `docker/grafana/provisioning/dashboards`:
    - **JVM**: Core JVM and Spring Boot health.
    - **IAM Service**: Custom business and security metrics.

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 11.0.8 (git hooks)
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

# Start infrastructure dependencies (PostgreSQL, RabbitMQ, MailHog)
docker compose up -d

# Run the service from your IDE or CLI
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
| `DB_USERNAME`          | `svc_iam_dba`                | Database user                                    |
| `DB_PASSWORD`          | `svc_iam_dba`                | Database password                                |
| `RABBITMQ_HOST`        | `localhost`                  | RabbitMQ host                                    |
| `RABBITMQ_PORT`        | `5672`                       | RabbitMQ AMQP port                               |
| `RABBITMQ_USERNAME`    | `svc_iam_rmq`                | RabbitMQ user                                    |
| `RABBITMQ_PASSWORD`    | `svc_iam_rmq`                | RabbitMQ password                                |
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

The project provides two Docker Compose configurations for different workflows:

### 1. Infrastructure-only (Local IDE Development)

Starts only the database, message broker, and mail server. The IAM service is expected to be run from your IDE or CLI.

```bash
docker compose up -d
```

### 2. Full Stack (Containerized Development)

Starts the entire stack including the IAM service container.

```bash
# Build image
docker build -t iqkv/foundation-iam-service:latest .

# Run everything
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
├── platformadmin/    # Platform operator self-service account
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
- **Multi-tenancy**: Per-tenant schema isolation managed by Liquibase; `TenantMembership` carries per-tenant authorities (`TENANT_OWNER`, `ADMIN`, `MEMBER`); tenant-scoped endpoints require a JWT scoped to the target tenant via `X-Tenant-ID` header
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; IAM publishes canonical mode via `/actuator/info` under `platform.rollout-mode`; service fails readiness on invalid/missing mode
- **Single-tenant mode**: Strategy pattern (`SignupStrategy`, `TenantBootstrapStrategy`) branches behavior at startup and signup; `SingleTenantBootstrapStrategy` idempotently provisions the default tenant on `ApplicationReadyEvent`; `SingleTenantSignupStrategy` joins users to the default tenant with `MEMBER` authority — no tenant creation, no `TENANT_OWNER` grant
- **Invitations**: Token-based signup-by-invitation flow; token resolves tenant context so accept endpoints are tenant-agnostic; `authority` defaults to `MEMBER`; ShedLock-guarded reaper expires stale tokens
- **Email**: Thymeleaf-rendered transactional emails via Spring Mail; MailHog for local testing
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes; `PlatformModeHealthIndicator` exposes rollout mode in `/actuator/health`; `PlatformModeInfoContributor` publishes canonical `platform.rollout-mode` in `/actuator/info`
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (60% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
