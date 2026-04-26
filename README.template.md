# Foundation IAM Service 🔐

<!-- TEMPLATE: Copy relevant sections into README.md and replace placeholders. Remove guidance blocks when done. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title to your service name and add a logo if desired.
2. Add badges (build, license) under the title.
3. Fill each section with your actual service content.
4. Update the API table to reflect actual endpoints and auth requirements.
5. Update the environment variables table to match your `application.yml` bindings.
6. Update the project structure tree if your bounded contexts differ.
7. Remove this guidance block after customizing.

</details>

- Add your service logo.
- Write a short introduction — what the service does and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: `![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-nodejs-project.yml?label=CI)`
- License: `![License](https://img.shields.io/github/license/ORG/REPO)`
- Java: `![Java](https://img.shields.io/badge/java-25-blue)`
- Spring Boot: `![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)`

</details>

## About

The Foundation IAM Service is the Identity and Access Management backbone of the IQKV platform. It owns user accounts, tenant lifecycle, memberships, and token issuance. Key responsibilities:

- Issues and validates RS256 JWT access tokens (15 min) and refresh tokens (7 days)
- Manages multi-tenant and single-tenant rollout modes via `ROLLOUT_MODE`
- Enforces per-tenant membership authorities: `TENANT_OWNER`, `ADMIN`, `MEMBER`
- Handles signup, signin, signout, token refresh, and global session revocation
- Provides invitation-based onboarding with token-gated accept flows
- Manages password reset and email verification flows
- Publishes canonical `platform.rollout-mode` via `/actuator/info` for gateway and billing service

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

| Method   | Path                | Auth        | Description                         |
| -------- | ------------------- | ----------- | ----------------------------------- |
| `GET`    | `/admin/users`      | JWT `ADMIN` | List users (paginated)              |
| `GET`    | `/admin/users/{id}` | JWT `ADMIN` | Get user by UUID                    |
| `POST`   | `/admin/users`      | JWT `ADMIN` | Create user with temporary password |
| `PUT`    | `/admin/users/{id}` | JWT `ADMIN` | Replace user (full update)          |
| `PATCH`  | `/admin/users/{id}` | JWT `ADMIN` | Partially update user               |
| `DELETE` | `/admin/users/{id}` | JWT `ADMIN` | Delete user and all memberships     |

### Tenant — `/api/v1/iam/tenants`

| Method  | Path                                      | Auth                               | Description               |
| ------- | ----------------------------------------- | ---------------------------------- | ------------------------- |
| `GET`   | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER` + `X-Tenant-ID` | Get tenant details        |
| `PATCH` | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER` + `X-Tenant-ID` | Update tenant status      |
| `POST`  | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Retry failed provisioning |

### Invitations

| Method   | Path                                    | Auth                                       | Description                                    |
| -------- | --------------------------------------- | ------------------------------------------ | ---------------------------------------------- |
| `POST`   | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Send invitation email                          |
| `GET`    | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | List pending invitations                       |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}` | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Revoke invitation                              |
| `GET`    | `/invitations/{token}`                  | public                                     | Preview invitation (tenant name, role, expiry) |
| `POST`   | `/invitations/{token}/accept`           | public                                     | Accept invitation; returns token pair          |

> Auth legend: `public` = no token required; `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char alphanumeric tenantKey header required for tenant-scoped endpoints.

## Tech Stack

- Java 25 / Spring Boot 3.x
- MyBatis 3.x + PostgreSQL
- Liquibase (system + per-tenant schema migrations)
- RabbitMQ (async tenant provisioning events)
- JJWT RS256 (token issuance and validation)
- ShedLock (distributed scheduled cleanup jobs)
- Micrometer + Prometheus
- Thymeleaf (transactional email templates)
- springdoc-openapi (Swagger UI)

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0 (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/ORG/REPO.git
cd REPO

# Install git hooks
pnpm install

# Copy environment variables
cp .env.example .env.local
# Edit .env.local with your local values

# Start dependencies (PostgreSQL, RabbitMQ, MailHog)
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
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
| `DEFAULT_TENANT_NAME`  | `Default Organization`       | Default tenant display name                      |
| `INVITATION_TOKEN_TTL` | `PT72H`                      | Invitation token lifetime                        |

> Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd` and fill in values for each environment.

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
docker build -t ORG/REPO:latest .

# Run full stack (service + dependencies)
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                   | Description                                                         |
| -------------------------- | ------------------------------------------------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes; includes `PlatformModeHealthIndicator` |
| `GET /actuator/info`       | Build info + canonical `platform.rollout-mode`                      |
| `GET /actuator/metrics`    | Application metrics                                                 |
| `GET /actuator/prometheus` | Prometheus scrape endpoint                                          |
| `GET /swagger-ui.html`     | API documentation                                                   |

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
├── security/         # JWT authentication filter, claim names
├── signup/           # SignupStrategy (multi-tenant / single-tenant)
├── tenant/           # Tenant lifecycle, provisioning, bootstrap strategies
├── tenancy/          # MyBatis schema interceptor, TenantContext, Liquibase runner
├── user/             # User profile, admin CRUD
├── shared/           # Common exceptions, utilities, value objects
└── infrastructure/   # Spring config, security, persistence, messaging, metrics
```

---

<details>
  <summary><strong>✅ Pre-publish checklist (remove in final README)</strong></summary>

- [ ] Title updated and logo added
- [ ] Badges added (CI, license)
- [ ] About section completed
- [ ] API table reflects actual endpoints and auth requirements
- [ ] Tech stack updated (remove unused entries, add missing ones)
- [ ] Environment variables table matches `application.yml` bindings
- [ ] Project structure tree updated to match actual packages
- [ ] Links verified (docs, external resources)
- [ ] Guidance blocks removed before publishing

</details>

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

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
