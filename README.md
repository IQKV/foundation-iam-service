> ## 🤔 What is this service all about?
>
> - Multi-tenant Identity and Access Management microservice for the IQ Key Value platform.
> - A single user account can belong to multiple tenants with different roles in each.
> - Make the project easy to maintain with **8 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...

---

# 🔐 IQ Key Value IAM Service

Multi-tenant Identity and Access Management microservice. Handles the full identity lifecycle — signup, authentication, token management, tenant provisioning, and brute-force protection.

## About

The IAM service handles the full identity lifecycle for a SaaS platform:

- **Self-service onboarding** — a single signup call creates the user account and provisions a new tenant in one step; the caller receives a `tenantKey` and polls for `ACTIVE` status
- **Signup by invitation** — existing TENANT_OWNER or ADMIN sends a time-limited invite (72 h, default authority `MEMBER`); new users are created on accept with email pre-verified; existing users verify identity by password
- **Multi-tenancy** — users are global identities; isolation is enforced through `TenantMembership` records that carry per-tenant authorities (`TENANT_OWNER`, `PLAFORM_OPERATOR`, `MEMBER`)
- **JWT RS256 authentication** — 15-minute access tokens and 7-day refresh tokens, both carrying `tenant_id`; every request is validated against a JTI denylist and a global signout timestamp
- **Tenant lifecycle** — owners can suspend, delete, or retry failed provisioning; a ShedLock-guarded reaper job cleans up stuck `PROVISIONING` tenants automatically
- **Brute-force protection** — failed login attempts are tracked per email; accounts are temporarily locked after 5 attempts for 15 minutes
- **Token revocation** — single-session signout (JTI denylist) and global signout (`last_global_signout_at`) are both supported
- **Password reset** — time-limited reset tokens (1 hour TTL) with rate limiting (3 requests per 15-minute window)
- **Email notifications** — Thymeleaf-rendered transactional emails via SMTP

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/iam`

### Authentication

| Method | Path                | Auth          | Description                              |
| ------ | ------------------- | ------------- | ---------------------------------------- |
| `POST` | `/auth/signup`      | public        | Register user and create tenant          |
| `POST` | `/auth/signin`      | `X-Tenant-ID` | Sign in, receive token pair              |
| `POST` | `/auth/refresh`     | `X-Tenant-ID` | Rotate access + refresh tokens           |
| `POST` | `/auth/signout`     | JWT           | Revoke current session                   |
| `POST` | `/auth/signout-all` | JWT           | Revoke all sessions globally             |
| `POST` | `/auth/validate`    | JWT           | Validate token for gateway introspection |

### User Profile

| Method   | Path             | Auth   | Description                               |
| -------- | ---------------- | ------ | ----------------------------------------- |
| `GET`    | `/users/me`      | JWT    | Get own profile                           |
| `PATCH`  | `/users/me`      | JWT    | Update own profile                        |
| `DELETE` | `/users/me`      | JWT    | Remove own membership from current tenant |
| `POST`   | `/users/tenants` | public | Discover tenants by credentials           |

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

| Method   | Path                                      | Auth                                                     | Description                 |
| -------- | ----------------------------------------- | -------------------------------------------------------- | --------------------------- |
| `GET`    | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER`                                       | Get tenant status           |
| `PATCH`  | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER`                                       | Transition tenant status    |
| `POST`   | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER`                                       | Retry failed provisioning   |
| `POST`   | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | Send invitation email       |
| `GET`    | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | List pending invitations    |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}`   | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | Revoke a pending invitation |

### Invitations (public)

| Method | Path                          | Auth   | Description                                |
| ------ | ----------------------------- | ------ | ------------------------------------------ |
| `GET`  | `/invitations/{token}`        | public | Preview invitation — token resolves tenant |
| `POST` | `/invitations/{token}/accept` | public | Accept invitation — token resolves tenant  |

### Platform Operator

| Method   | Path                   | Auth | Description                     |
| -------- | ---------------------- | ---- | ------------------------------- |
| `GET`    | `/operator/users`      | JWT  | List all users (paginated)      |
| `GET`    | `/operator/users/{id}` | JWT  | Get user by ID                  |
| `POST`   | `/operator/users`      | JWT  | Create user                     |
| `PUT`    | `/operator/users/{id}` | JWT  | Replace user (full update)      |
| `PATCH`  | `/operator/users/{id}` | JWT  | Partially update user           |
| `DELETE` | `/operator/users/{id}` | JWT  | Delete user and all memberships |

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

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0 (git hooks)
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

# Run the service (load .env.local first to activate the local Spring profile)
export $(grep -v '^#' .env.local | xargs)
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
# → MailHog:  http://localhost:8025
```

## Environment Variables

| Variable               | Default                      | Description                               |
| ---------------------- | ---------------------------- | ----------------------------------------- |
| `DB_HOST`              | `localhost`                  | PostgreSQL host                           |
| `DB_PORT`              | `5432`                       | PostgreSQL port (host-mapped from Docker) |
| `DB_NAME`              | `iam`                        | Database name                             |
| `DB_USERNAME`          | `iam`                        | Database user                             |
| `DB_PASSWORD`          | `iam`                        | Database password                         |
| `RABBITMQ_HOST`        | `localhost`                  | RabbitMQ host                             |
| `RABBITMQ_PORT`        | `5672`                       | RabbitMQ AMQP port                        |
| `RABBITMQ_USERNAME`    | `iam`                        | RabbitMQ user                             |
| `RABBITMQ_PASSWORD`    | `iam`                        | RabbitMQ password                         |
| `MAIL_HOST`            | `localhost`                  | SMTP host                                 |
| `MAIL_PORT`            | `587`                        | SMTP port                                 |
| `MAIL_FROM`            | `noreply@iqkv.com`           | Sender address                            |
| `JWT_PRIVATE_KEY_PATH` | `classpath:keys/private.pem` | RS256 private key                         |
| `JWT_PUBLIC_KEY_PATH`  | `classpath:keys/public.pem`  | RS256 public key                          |
| `APP_BASE_URL`         | `http://localhost:3000`      | Frontend base URL (used in email links)   |
| `INVITATION_TOKEN_TTL` | `PT72H`                      | Invitation token TTL (ISO-8601 duration)  |

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

```bash
# Build image
docker build -t iqkv/foundation-iam-service:latest .

# Run full stack (service + dependencies)
docker compose -f compose.container.yaml up -d
```

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:25-jdk-alpine`, the runtime stage uses `eclipse-temurin:25-jre-alpine` with a non-root `appuser` and layered JAR extraction for optimal cache reuse.

## Monitoring

| Endpoint                     | Description                       |
| ---------------------------- | --------------------------------- |
| `GET /actuator/health`       | Liveness + readiness probes       |
| `GET /actuator/metrics`      | Application metrics               |
| `GET /actuator/prometheus`   | Prometheus scrape endpoint        |
| `GET /swagger-ui.html`       | API documentation                 |
| `GET /.well-known/jwks.json` | Public JWK Set for JWT validation |

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
├── security/           # JWT RS256 signing/validation, JWKS endpoint
├── shared/             # Common exceptions, utilities, value objects
├── tenancy/            # Liquibase multi-tenant schema management
├── tenant/             # Tenant lifecycle (provisioning, status transitions)
└── user/               # User profile management
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
