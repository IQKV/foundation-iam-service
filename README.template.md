# Foundation IAM Service 🔐

Identity and Access Management microservice for the Key Value Platform. Owns user accounts, tenant lifecycle, memberships, and RS256 token issuance. Handles the full identity lifecycle — signup, authentication, token management, tenant provisioning, and brute-force protection.

## About

The IAM service handles the full identity lifecycle for a SaaS platform:

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

### Authentication

| Method | Path                              | Auth          | Description                                                       |
| ------ | --------------------------------- | ------------- | ----------------------------------------------------------------- |
| `POST` | `/auth/signup`                    | public        | Register user and create tenant                                   |
| `GET`  | `/auth/signup/status/{tenantKey}` | public        | Poll tenant provisioning status after signup                      |
| `POST` | `/auth/signin`                    | `X-Tenant-ID` | Sign in, receive token pair                                       |
| `POST` | `/auth/exchange`                  | JWT           | Exchange a Bearer access token for a new tenant-scoped token pair |
| `POST` | `/auth/admin/signin`              | public        | Platform admin sign-in (platform-scoped token, null `tenant_id`)  |
| `POST` | `/auth/admin/refresh`             | public        | Refresh platform-scoped token pair                                |
| `POST` | `/auth/refresh`                   | public        | Rotate access + refresh tokens                                    |
| `POST` | `/auth/signout`                   | JWT           | Revoke current session                                            |
| `POST` | `/auth/signout-all`               | JWT           | Revoke all sessions globally                                      |
| `POST` | `/auth/validate`                  | JWT           | Validate token for gateway introspection                          |

### Platform Admin Account

| Method  | Path                      | Auth                 | Description                                                               |
| ------- | ------------------------- | -------------------- | ------------------------------------------------------------------------- |
| `GET`   | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Get own platform operator profile                                         |
| `PATCH` | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Update own first/last name                                                |
| `POST`  | `/auth/admin/me/password` | JWT `PLATFORM_ADMIN` | Change own password (requires current password; invalidates all sessions) |

### User Profile

| Method   | Path                    | Auth                      | Description                                                               |
| -------- | ----------------------- | ------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/users/me`             | JWT + `X-Tenant-ID`       | Get own profile                                                           |
| `PATCH`  | `/users/me`             | JWT + `X-Tenant-ID`       | Update own profile                                                        |
| `DELETE` | `/users/me`             | JWT + `X-Tenant-ID`       | Remove own membership from current tenant                                 |
| `POST`   | `/users/me/password`    | JWT + `X-Tenant-ID`       | Change own password (requires current password; invalidates all sessions) |
| `POST`   | `/users/tenants`        | public (credential-gated) | Discover tenants by credentials                                           |
| `GET`    | `/users/me/memberships` | JWT                       | List current user's tenant memberships                                    |

### Password Reset

| Method | Path                     | Auth   | Description                            |
| ------ | ------------------------ | ------ | -------------------------------------- |
| `POST` | `/users/password/forgot` | public | Initiate password reset (rate-limited) |
| `POST` | `/users/password/reset`  | public | Complete password reset with token     |

### Email Verification

| Method | Path                               | Auth   | Description                              |
| ------ | ---------------------------------- | ------ | ---------------------------------------- |
| `POST` | `/users/email/verify`              | public | Verify email address with one-time token |
| `POST` | `/users/email/resend-verification` | public | Resend verification email (rate-limited) |

### Tenant Management

| Method   | Path                                      | Auth                                       | Description                 |
| -------- | ----------------------------------------- | ------------------------------------------ | --------------------------- |
| `GET`    | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER` + `X-Tenant-ID`         | Get tenant status           |
| `PATCH`  | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER` + `X-Tenant-ID`         | Transition tenant status    |
| `POST`   | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER` + `X-Tenant-ID`         | Retry failed provisioning   |
| `POST`   | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Send invitation email       |
| `GET`    | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | List pending invitations    |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}`   | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Revoke a pending invitation |

### Invitations (public)

| Method | Path                          | Auth   | Description                                |
| ------ | ----------------------------- | ------ | ------------------------------------------ |
| `GET`  | `/invitations/{token}`        | public | Preview invitation — token resolves tenant |
| `POST` | `/invitations/{token}/accept` | public | Accept invitation — token resolves tenant  |

### Platform Admin — Users

| Method   | Path                            | Auth                 | Description                                        |
| -------- | ------------------------------- | -------------------- | -------------------------------------------------- |
| `GET`    | `/admin/users`                  | JWT `PLATFORM_ADMIN` | List all users (paginated, filterable)             |
| `GET`    | `/admin/users/count`            | JWT `PLATFORM_ADMIN` | Total user count                                   |
| `GET`    | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Get user by ID                                     |
| `POST`   | `/admin/users`                  | JWT `PLATFORM_ADMIN` | Create user                                        |
| `PUT`    | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Replace user (full update)                         |
| `PATCH`  | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Partially update user                              |
| `DELETE` | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Delete user and all memberships                    |
| `GET`    | `/admin/users/{id}/authorities` | JWT `PLATFORM_ADMIN` | Get user platform authorities                      |
| `PUT`    | `/admin/users/{id}/authorities` | JWT `PLATFORM_ADMIN` | Replace user platform authorities                  |
| `GET`    | `/admin/users/{id}/memberships` | JWT `PLATFORM_ADMIN` | Get user tenant memberships                        |
| `POST`   | `/admin/users/{id}/password`    | JWT `PLATFORM_ADMIN` | Force-set user password (invalidates all sessions) |

### Platform Admin — Tenants

| Method   | Path                                                      | Auth                 | Description                                 |
| -------- | --------------------------------------------------------- | -------------------- | ------------------------------------------- |
| `GET`    | `/admin/tenants`                                          | JWT `PLATFORM_ADMIN` | List all tenants (paginated, filterable)    |
| `GET`    | `/admin/tenants/count`                                    | JWT `PLATFORM_ADMIN` | Total tenant count                          |
| `GET`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Get tenant by key                           |
| `PUT`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Rename tenant                               |
| `PATCH`  | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Partially update tenant                     |
| `DELETE` | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Delete tenant and all associated data       |
| `GET`    | `/admin/tenants/{tenantKey}/members`                      | JWT `PLATFORM_ADMIN` | List tenant members (paginated, filterable) |
| `GET`    | `/admin/tenants/{tenantKey}/members/count`                | JWT `PLATFORM_ADMIN` | Total member count for tenant               |
| `GET`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Get member's tenant authorities             |
| `PUT`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Replace member's tenant authorities         |

### Platform Admin — Invitations

| Method   | Path                       | Auth                 | Description                                                 |
| -------- | -------------------------- | -------------------- | ----------------------------------------------------------- |
| `GET`    | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | List invitations across all tenants (paginated, filterable) |
| `GET`    | `/admin/invitations/count` | JWT `PLATFORM_ADMIN` | Total invitation count (with optional filters)              |
| `GET`    | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Get invitation by ID                                        |
| `POST`   | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | Propose invitation for any active tenant                    |
| `DELETE` | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Revoke invitation                                           |

> Auth legend: `public` = no token required; `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char alphanumeric tenantKey header required for tenant-scoped endpoints.

JWKS endpoint (public, consumed by the gateway): `GET /.well-known/jwks.json`

## Tech Stack

- Java 25 / Spring Boot 4.0
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase for schema migrations
- RabbitMQ for async tenant provisioning
- JJWT 0.13 (RS256 signing)
- ShedLock 7.x for distributed scheduled jobs
- Thymeleaf for email templates
- Micrometer + Prometheus

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

# Run the service from your IDE or CLI (load .env.local first to activate the local Spring profile)
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
| `DB_PORT`              | `5432`                       | PostgreSQL port (host-mapped from Docker)        |
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
| `INVITATION_TOKEN_TTL` | `PT72H`                      | Invitation token TTL (ISO-8601 duration)         |

Copy `.env.example` to `.env.local` (or `.env.uat` / `.env.prd`) and fill in production values.

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

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:25-jdk-alpine`, the runtime stage uses `eclipse-temurin:25-jre-alpine` with a non-root `appuser` and layered JAR extraction for optimal cache reuse.

## Monitoring

| Endpoint                     | Description                                                         |
| ---------------------------- | ------------------------------------------------------------------- |
| `GET /actuator/health`       | Liveness + readiness probes; includes `PlatformModeHealthIndicator` |
| `GET /actuator/info`         | Build info + canonical `platform.rollout-mode`                      |
| `GET /actuator/metrics`      | Application metrics                                                 |
| `GET /actuator/prometheus`   | Prometheus scrape endpoint                                          |
| `GET /swagger-ui.html`       | API documentation                                                   |
| `GET /.well-known/jwks.json` | Public JWK Set for JWT validation                                   |

A Grafana dashboard (`docker/grafana/`) provides real-time visibility into service health and JVM metrics using Prometheus as the data source.

## Project Structure

```
src/main/java/com/iqkv/foundation/iamservice/
├── authentication/     # Signin, signup, token refresh, signout flows
├── denylist/           # JTI denylist for token revocation
├── email/              # Thymeleaf email templates and sending
├── infrastructure/     # Spring config, security, MyBatis, RabbitMQ setup
├── invitation/         # Signup by invitation — send, preview, accept, revoke, reaper job
├── lockout/            # Brute-force login attempt tracking
├── membership/         # TenantMembership — per-tenant authorities
├── passwordreset/      # Password reset token lifecycle
├── platformadmin/      # Platform operator self-service account
├── security/           # JWT RS256 signing/validation, JWKS endpoint
├── shared/             # Common exceptions, utilities, value objects
├── signup/             # SignupStrategy (multi-tenant / single-tenant)
├── tenancy/            # Liquibase multi-tenant schema management
├── tenant/             # Tenant lifecycle (provisioning, status transitions)
└── user/               # User profile management
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
