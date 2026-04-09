# IAM Service

Multi-tenant Identity and Access Management microservice. A single user account can belong to multiple tenants with different roles in each.

![CI](https://img.shields.io/github/actions/workflow/status/IQKV/foundation-iam-service/ci.yml?label=CI)
![License](https://img.shields.io/github/license/IQKV/foundation-iam-service)

## About

The IAM service handles the full identity lifecycle for a SaaS platform:

- **Self-service onboarding** — a single signup call creates the user account and provisions a new tenant in one step; the caller receives a `tenantKey` and polls for `ACTIVE` status
- **Multi-tenancy** — users are global identities; isolation is enforced through `TenantMembership` records that carry per-tenant roles (`TENANT_OWNER`, `ADMIN`, `MEMBER`)
- **JWT RS256 authentication** — 15-minute access tokens and 7-day refresh tokens, both carrying `tenant_id`; every request is validated against a JTI denylist and a global signout timestamp
- **Tenant lifecycle** — owners can suspend, delete, or retry failed provisioning; a ShedLock-guarded reaper job cleans up stuck `PROVISIONING` tenants automatically
- **Brute-force protection** — failed login attempts are tracked per email; accounts are temporarily locked after a configurable threshold
- **Token revocation** — single-session signout (JTI denylist) and global signout (`last_global_signout_at`) are both supported

## API

Base path: `/api/v1/iam`

| Method  | Path                                      | Auth               | Description                              |
| ------- | ----------------------------------------- | ------------------ | ---------------------------------------- |
| `POST`  | `/auth/signup`                            | public             | Register user and create tenant          |
| `POST`  | `/auth/signin`                            | `X-Tenant-ID`      | Sign in, receive token pair              |
| `POST`  | `/auth/refresh`                           | `X-Tenant-ID`      | Rotate access + refresh tokens           |
| `POST`  | `/auth/signout`                           | JWT                | Revoke current session                   |
| `POST`  | `/auth/signout-all`                       | JWT                | Revoke all sessions globally             |
| `POST`  | `/auth/validate`                          | JWT                | Validate token for gateway introspection |
| `POST`  | `/users/tenants`                          | public             | Discover tenants by credentials          |
| `GET`   | `/users/me`                               | JWT                | Get own profile                          |
| `PATCH` | `/users/me`                               | JWT                | Update own profile                       |
| `GET`   | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER` | Get tenant status                        |
| `PATCH` | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER` | Transition tenant status                 |
| `POST`  | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER` | Retry failed provisioning                |

## Tech Stack

- Java 21 / Spring Boot 3.4
- MyBatis 3.x (no JPA) + PostgreSQL
- Liquibase for schema migrations
- RabbitMQ for async tenant provisioning
- JJWT (RS256 signing)
- ShedLock for distributed scheduled jobs
- Micrometer + Prometheus

## Running Locally

**Prerequisites:** JDK 21, Docker

```bash
# Start dependencies
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
```

## Building

```bash
# Compile and test
./mvnw clean verify -Dcheckstyle.skip=true

# Production image
./mvnw clean package -Pproduction
docker build -t iam-service .
```

## Documentation

- [API Documentation](docs/api/README.md)
- [Architecture Overview](docs/architecture/README.md)
- [Deployment Guide](docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
