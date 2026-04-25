# Project Name 🔐

<!-- TEMPLATE: This README.template.md is a starter template. Copy parts into your real README.md and replace placeholders. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title above to your service name and optionally add a logo right below it.
2. Add badges (build, license) under the title.
3. Fill each section below with your actual service content.
4. Update the API table to reflect your actual endpoints and auth requirements.
5. Update the environment variables table to match your `application.yml` bindings.
6. Update the project structure tree if your bounded contexts differ.
7. Remove this guidance block after you finish customizing.

</details>

- Add your service logo.
- Write a short introduction — what the service does and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: <code>![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-nodejs-project.yml?label=CI)</code>
- License: <code>![License](https://img.shields.io/github/license/ORG/REPO)</code>
- Java: <code>![Java](https://img.shields.io/badge/java-21-blue)</code>
- Spring Boot: <code>![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)</code>

</details>

## About

Describe the service's responsibilities in plain language. Focus on what it owns, not how it works internally. Good prompts:

- What domain does it manage?
- What are the key business rules it enforces?
- What does it produce for other services (tokens, events, headers)?

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/your-service`

| Method   | Path             | Auth        | Description        |
| -------- | ---------------- | ----------- | ------------------ |
| `POST`   | `/resource`      | public      | Create a resource  |
| `GET`    | `/resource/{id}` | JWT         | Get resource by ID |
| `PATCH`  | `/resource/{id}` | JWT `ROLE`  | Update resource    |
| `DELETE` | `/resource/{id}` | JWT `ADMIN` | Delete resource    |

> Replace with your actual endpoints. Document the auth requirement for each — `public`, `X-Tenant-ID`, `JWT`, `JWT ROLE_NAME`, or `JWT ROLE_NAME + X-Tenant-ID` for tenant-scoped management endpoints.

## Tech Stack

- Java 25 / Spring Boot 4.x
- MyBatis 3.x + PostgreSQL (or JPA — update as needed)
- Liquibase for schema migrations
- RabbitMQ for async messaging (if applicable)
- JJWT RS256 (if this service issues tokens)
- ShedLock for distributed scheduled jobs (if applicable)
- Micrometer + Prometheus

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

# Start dependencies
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable               | Default                      | Description                           |
| ---------------------- | ---------------------------- | ------------------------------------- |
| `DB_HOST`              | `localhost`                  | PostgreSQL host                       |
| `DB_PORT`              | `5432`                       | PostgreSQL port                       |
| `DB_NAME`              | `service`                    | Database name                         |
| `DB_USERNAME`          | `service`                    | Database user                         |
| `DB_PASSWORD`          | `service`                    | Database password                     |
| `RABBITMQ_HOST`        | `localhost`                  | RabbitMQ host (if used)               |
| `RABBITMQ_PORT`        | `5672`                       | RabbitMQ AMQP port                    |
| `MAIL_HOST`            | `localhost`                  | SMTP host (if used)                   |
| `MAIL_PORT`            | `587`                        | SMTP port                             |
| `JWT_PRIVATE_KEY_PATH` | `classpath:keys/private.pem` | RS256 private key (if issuing tokens) |
| `JWT_PUBLIC_KEY_PATH`  | `classpath:keys/public.pem`  | RS256 public key                      |
| `APP_BASE_URL`         | `http://localhost:3000`      | Frontend base URL                     |

> Add or remove rows to match your `application.yml` environment variable bindings. Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd`.

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

| Endpoint                   | Description                 |
| -------------------------- | --------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes |
| `GET /actuator/metrics`    | Application metrics         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint  |
| `GET /swagger-ui.html`     | API documentation           |

## Project Structure

```
src/main/java/com/example/yourservice/
├── bounded-context-1/  # e.g. authentication — signin, signup, token flows
├── bounded-context-2/  # e.g. user — profile management
├── bounded-context-3/  # e.g. tenant — lifecycle, provisioning
├── infrastructure/     # Spring config, security, persistence setup
└── shared/             # Common exceptions, utilities, value objects
```

> Update bounded context names to match your actual package structure.

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
- **Messaging**: RabbitMQ for async domain events (e.g. tenant provisioning); ShedLock guards scheduled cleanup jobs
- **Security**: Spring Security + JJWT RS256; JTI denylist for token revocation; brute-force lockout per identity
- **Multi-tenancy**: Per-tenant schema isolation managed by Liquibase; `TenantMembership` carries per-tenant authorities (`TENANT_OWNER`, `ADMIN`, `MEMBER`); tenant-scoped management endpoints require a JWT scoped to the target tenant via `X-Tenant-ID` header
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; IAM publishes canonical mode via `/actuator/info` under `platform.rollout-mode`; service fails readiness on invalid/missing mode
- **Single-tenant mode**: Strategy pattern (`SignupStrategy`, `TenantBootstrapStrategy`) branches behavior at startup and signup; `SingleTenantBootstrapStrategy` idempotently provisions the default tenant on `ApplicationReadyEvent`; `SingleTenantSignupStrategy` joins users to the default tenant with `MEMBER` authority — no tenant creation, no `TENANT_OWNER` grant
- **Invitations**: Token-based signup-by-invitation flow; token resolves tenant context so accept endpoints are tenant-agnostic; `authority` defaults to `MEMBER`; ShedLock-guarded reaper expires stale tokens
- **Email**: Thymeleaf-rendered transactional emails via Spring Mail; MailHog for local testing
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes; `PlatformModeHealthIndicator` exposes rollout mode in `/actuator/health`; `PlatformModeInfoContributor` publishes canonical `platform.rollout-mode` in `/actuator/info`
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (60% gate), ArchUnit, oxfmt, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
