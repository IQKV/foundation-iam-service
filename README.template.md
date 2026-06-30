# Foundation IAM Service 🔐

Identity and Access Management microservice for the Key Value Platform. Owns user accounts, tenant lifecycle, memberships, and RS256 token issuance. Handles the full identity lifecycle — signup, authentication, token management, tenant provisioning, and brute-force protection.

## About

The IAM service handles the full identity lifecycle for a SaaS platform:

- **Self-service onboarding** — signup creates the user account and adds them as a `MEMBER` to the hidden platform tenant; tenants are created separately via a dedicated endpoint after signup
- **Signup by invitation** — existing `TENANT_OWNER` or `ADMIN` sends a time-limited invite (72 h, default authority `MEMBER`); new users are created on accept with email pre-verified; existing users verify identity by password
- **Multi-tenancy** — users are global identities; isolation is enforced through `TenantMembership` records that carry per-tenant authorities (`TENANT_OWNER`, `ADMIN`, `MEMBER`)
- **JWT RS256 authentication** — 15-minute access tokens and 7-day refresh tokens, both carrying `tenant_id`; every request is validated against a JTI denylist and a global signout timestamp
- **Tenant lifecycle** — owners can rename, suspend, delete, or retry failed provisioning; a ShedLock-guarded reaper job cleans up stuck `PROVISIONING` tenants automatically
- **Member management** — tenant owners can change member authority (`TENANT_OWNER` ↔ `MEMBER`), ban/unban members, and transfer ownership with guardrails (can't remove last owner, can't ban yourself)
- **Brute-force protection** — failed login attempts are tracked per email; accounts are temporarily locked after 5 attempts for 15 minutes; platform admins can unlock users manually
- **Token revocation** — single-session signout (JTI denylist) and global signout (`last_global_signout_at`) are both supported
- **Password reset** — time-limited reset tokens (1 hour TTL) with rate limiting (3 requests per 15-minute window)
- **Magic link authentication** — passwordless sign-in with time-limited tokens (configurable TTL), resend functionality, and rate limiting
- **OAuth2/OIDC authentication** — social sign-in via Google, GitHub, and Microsoft plus tenant-specific OIDC providers; browser flows use server-side PKCE and Redis-backed signed state
- **Account linking** — authenticated users can link/unlink external identities; platform admins can inspect and force-unmerge linked identities for remediation
- **Tenant SSO configuration** — tenant owners and platform admins can manage custom OIDC client settings per tenant
- **Avatar uploads** — two-phase S3 upload flow: initiate generates presigned PUT URL, client uploads directly to S3, confirm persists avatar URL; old avatars automatically deleted
- **In-app notifications** — transactional events (signup, password reset, invitation, etc.) are persisted as `UserNotification` records and pushed in real time via WebSocket (STOMP/SockJS)
- **Site-wide announcements** — platform admins create multi-lingual announcements; publishing triggers an async fan-out that creates per-user notifications in batches and broadcasts to all connected clients via WebSocket
- **Email notifications** — Thymeleaf-rendered transactional emails via SMTP
- **Platform admin actions** — platform admins can ban/unban/unlock users, manage platform authorities, and more
- **Platform rollout mode** — publishes canonical `platform.rollout-mode` via `/actuator/info` for gateway and billing service consistency checks
- **Plan feature enforcement** — `PlanFeatureGuard` resolves the caller's active plan features from a local cache (populated at startup from the billing service) and throws `PlanFeatureNotAvailableException` when a gated endpoint is called by a plan that does not include the required feature; the `plan_code` JWT claim is the only runtime input required

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/iam`

### Authentication

| Method | Path                              | Auth                   | Description                                                       |
| ------ | --------------------------------- | ---------------------- | ----------------------------------------------------------------- |
| `POST` | `/auth/signup`                    | public                 | Register user and add as MEMBER to platform tenant                |
| `GET`  | `/auth/signup/status/{tenantKey}` | public                 | Poll tenant provisioning status after signup                      |
| `POST` | `/auth/signin`                    | public + `X-Tenant-ID` | Sign in; returns RS256 access + refresh token pair                |
| `POST` | `/auth/exchange`                  | JWT                    | Exchange a Bearer access token for a new tenant-scoped token pair |
| `POST` | `/auth/admin/signin`              | public                 | Platform admin sign-in (platform-scoped token, null `tenant_id`)  |
| `POST` | `/auth/admin/refresh`             | public                 | Refresh platform-scoped token pair                                |
| `POST` | `/auth/refresh`                   | public                 | Rotate access + refresh tokens                                    |
| `POST` | `/auth/signout`                   | JWT                    | Revoke current session (JTI denylist)                             |
| `POST` | `/auth/signout-all`               | JWT                    | Invalidate all sessions via `last_global_signout_at`              |
| `POST` | `/auth/validate`                  | JWT                    | Validate token and return user context (gateway introspection)    |

### Magic Link Authentication

| Method | Path                        | Auth   | Description                                                                |
| ------ | --------------------------- | ------ | -------------------------------------------------------------------------- |
| `POST` | `/auth/magic-link/initiate` | public | Initiate magic link authentication (email sent if user exists; always 204) |
| `POST` | `/auth/magic-link/resend`   | public | Resend magic link (if token exists; always 204)                            |
| `POST` | `/auth/magic-link/exchange` | public | Exchange magic link token for RS256 access + refresh token pair            |

### OAuth2/OIDC Authentication

| Method   | Path                           | Auth   | Description                                                                        |
| -------- | ------------------------------ | ------ | ---------------------------------------------------------------------------------- |
| `GET`    | `/auth/oauth2/providers`       | public | List enabled OAuth2/OIDC providers that resolve to active client registrations     |
| `GET`    | `/auth/oauth2/authorize`       | public | Start browser-based OAuth2/OIDC login flow with signed state + server-side PKCE    |
| `GET`    | `/auth/oauth2/callback`        | public | Handle provider callback and redirect to the configured frontend callback URI      |
| `POST`   | `/auth/oauth2/exchange`        | public | Exchange provider authorization code + PKCE verifier for IAM access/refresh tokens |
| `GET`    | `/auth/oauth2/link/{provider}` | JWT    | Start account-linking flow for the current user                                    |
| `GET`    | `/auth/oauth2/link/callback`   | public | Handle provider callback for account linking                                       |
| `DELETE` | `/auth/oauth2/link/{provider}` | JWT    | Unlink an external provider from the current account                               |
| `GET`    | `/auth/oauth2/identities`      | JWT    | List linked external identities for the current user                               |

### Platform Admin Account

| Method  | Path                      | Auth                 | Description                                                               |
| ------- | ------------------------- | -------------------- | ------------------------------------------------------------------------- |
| `GET`   | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Get own platform operator profile                                         |
| `PATCH` | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Update own first/last name                                                |
| `POST`  | `/auth/admin/me/password` | JWT `PLATFORM_ADMIN` | Change own password (requires current password; invalidates all sessions) |

### User Profile

| Method   | Path                       | Auth                      | Description                                                               |
| -------- | -------------------------- | ------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/users/me`                | JWT + `X-Tenant-ID`       | Get own profile                                                           |
| `PATCH`  | `/users/me`                | JWT + `X-Tenant-ID`       | Update own profile                                                        |
| `DELETE` | `/users/me`                | JWT + `X-Tenant-ID`       | Remove own membership from current tenant                                 |
| `POST`   | `/users/me/password`       | JWT + `X-Tenant-ID`       | Change own password (requires current password; invalidates all sessions) |
| `POST`   | `/users/me/avatar`         | JWT + `X-Tenant-ID`       | Initiate avatar upload (returns presigned S3 URL)                         |
| `POST`   | `/users/me/avatar/confirm` | JWT + `X-Tenant-ID`       | Confirm avatar upload (persists avatar URL after S3 upload)               |
| `DELETE` | `/users/me/avatar`         | JWT + `X-Tenant-ID`       | Delete current user's avatar                                              |
| `POST`   | `/users/tenants`           | public (credential-gated) | Discover tenants by credentials                                           |
| `GET`    | `/users/me/memberships`    | JWT                       | List current user's tenant memberships                                    |

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

### In-App Notifications

| Method   | Path                                | Auth | Description                                                               |
| -------- | ----------------------------------- | ---- | ------------------------------------------------------------------------- |
| `GET`    | `/users/notifications`              | JWT  | Paginated list; optional `isRead` filter; response includes `unreadCount` |
| `PATCH`  | `/users/notifications`              | JWT  | Bulk partial update — e.g. mark all as read (`{ "isRead": true }`)        |
| `DELETE` | `/users/notifications`              | JWT  | Delete all notifications for the current user                             |
| `GET`    | `/users/notifications/unread/count` | JWT  | Unread notification count (badge)                                         |
| `PATCH`  | `/users/notifications/{id}`         | JWT  | Partial update on a single notification (`{ "isRead": true }`)            |
| `DELETE` | `/users/notifications/{id}`         | JWT  | Delete a single notification                                              |

Real-time delivery via WebSocket: connect to `/api/v1/iam/ws` (STOMP/SockJS) and subscribe to
`/user/{userId}/queue/notifications` for per-user pushes or `/topic/announcements` for global broadcasts.

### Announcements (Public)

| Method | Path             | Auth   | Description                                     |
| ------ | ---------------- | ------ | ----------------------------------------------- |
| `GET`  | `/announcements` | public | Get active site-wide announcements for a locale |

### Tenant Management

| Method   | Path                                                       | Auth                                                | Description                                                           |
| -------- | ---------------------------------------------------------- | --------------------------------------------------- | --------------------------------------------------------------------- |
| `POST`   | `/tenants`                                                 | JWT                                                 | Create new tenant (owner is caller)                                   |
| `GET`    | `/tenants/{tenantKey}`                                     | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Get tenant details                                                    |
| `PATCH`  | `/tenants/{tenantKey}`                                     | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Rename tenant                                                         |
| `PATCH`  | `/tenants/{tenantKey}/status`                              | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Transition tenant status                                              |
| `POST`   | `/tenants/{tenantKey}/retry-provisioning`                  | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Retry failed provisioning                                             |
| `GET`    | `/tenants/{tenantKey}/members`                             | JWT `TENANT_OWNER`/`ADMIN`/`MEMBER` + `X-Tenant-ID` | List tenant members (paginated)                                       |
| `GET`    | `/tenants/{tenantKey}/members/count`                       | JWT `TENANT_OWNER`/`ADMIN`/`MEMBER` + `X-Tenant-ID` | Count tenant members                                                  |
| `GET`    | `/tenants/{tenantKey}/members/{userId}/authorities`        | JWT `TENANT_OWNER`/`ADMIN`/`MEMBER` + `X-Tenant-ID` | Get member's tenant authorities                                       |
| `PUT`    | `/tenants/{tenantKey}/members/{userId}/authorities`        | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Replace member's tenant authorities                                   |
| `DELETE` | `/tenants/{tenantKey}/members/{userId}`                    | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Remove member from tenant                                             |
| `POST`   | `/tenants/{tenantKey}/members/{userId}/ban`                | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Ban member from tenant; invalidates all sessions for this tenant      |
| `POST`   | `/tenants/{tenantKey}/members/{userId}/unban`              | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Unban member from tenant                                              |
| `POST`   | `/tenants/{tenantKey}/members/{userId}/transfer-ownership` | JWT `TENANT_OWNER` + `X-Tenant-ID`                  | Transfer tenant ownership to another member; old owner becomes MEMBER |

### Tenant SSO Configuration

| Method   | Path           | Auth                                | Description                             |
| -------- | -------------- | ----------------------------------- | --------------------------------------- |
| `GET`    | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Get current tenant custom OIDC settings |
| `PUT`    | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Create or replace tenant OIDC settings  |
| `DELETE` | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Delete tenant OIDC settings             |

### Invitations

| Method   | Path                                    | Auth                                       | Description                                         |
| -------- | --------------------------------------- | ------------------------------------------ | --------------------------------------------------- |
| `POST`   | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Send invitation email                               |
| `GET`    | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | List pending invitations                            |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}` | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Revoke invitation                                   |
| `GET`    | `/invitations/{token}`                  | public                                     | Preview invitation (tenant name, authority, expiry) |
| `POST`   | `/invitations/{token}/accept`           | public                                     | Accept invitation; returns token pair               |

### Locales

| Method | Path       | Auth   | Description            |
| ------ | ---------- | ------ | ---------------------- |
| `GET`  | `/locales` | public | Get all active locales |

### Platform Admin — Users

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
| `POST`   | `/admin/users/{id}/ban`         | JWT `PLATFORM_ADMIN` | Ban user globally                                  |
| `POST`   | `/admin/users/{id}/unban`       | JWT `PLATFORM_ADMIN` | Unban user globally                                |
| `POST`   | `/admin/users/{id}/unlock`      | JWT `PLATFORM_ADMIN` | Unlock user by resetting failed login attempts     |

### Platform Admin — Tenants

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

### Platform Admin — Invitations

| Method   | Path                       | Auth                 | Description                                                 |
| -------- | -------------------------- | -------------------- | ----------------------------------------------------------- |
| `GET`    | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | List invitations across all tenants (paginated, filterable) |
| `GET`    | `/admin/invitations/count` | JWT `PLATFORM_ADMIN` | Total invitation count (with optional filters)              |
| `GET`    | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Get invitation by UUID                                      |
| `POST`   | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | Propose invitation for any active tenant                    |
| `DELETE` | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Revoke invitation                                           |

### Platform Admin — Announcements

| Method   | Path                                | Auth                 | Description                                         |
| -------- | ----------------------------------- | -------------------- | --------------------------------------------------- |
| `POST`   | `/admin/announcements`              | JWT `PLATFORM_ADMIN` | Create announcement with multi-lingual translations |
| `PUT`    | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Update announcement and its translations            |
| `DELETE` | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Delete announcement                                 |
| `POST`   | `/admin/announcements/{id}/publish` | JWT `PLATFORM_ADMIN` | Trigger async fan-out to all users                  |
| `GET`    | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Get announcement by UUID                            |
| `GET`    | `/admin/announcements`              | JWT `PLATFORM_ADMIN` | List all announcements (paginated)                  |

### Platform Admin — OIDC

| Method   | Path                                         | Auth                 | Description                                         |
| -------- | -------------------------------------------- | -------------------- | --------------------------------------------------- |
| `GET`    | `/admin/oidc/users/{userId}/identities`      | JWT `PLATFORM_ADMIN` | List linked OIDC identities for a user              |
| `DELETE` | `/admin/oidc/users/{userId}/identities/{id}` | JWT `PLATFORM_ADMIN` | Force-unmerge a linked identity from a user account |

> Auth legend: `public` = no token required; `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char alphanumeric tenantKey header required for tenant-scoped endpoints.

JWKS endpoint (public, consumed by the gateway): `GET /.well-known/jwks.json`

## Tech Stack

- Java 25 / Spring Boot 4.1
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase (system + per-tenant schema migrations)
- RabbitMQ (async tenant provisioning events, notifications, announcements)
- Redis (OIDC state/PKCE storage, lockouts, ephemeral security state)
- MinIO 9.x S3-compatible client (avatar uploads, presigned URLs)
- JJWT 0.13 RS256 (token issuance and validation)
- Spring Security OAuth2 Client (OAuth2/OIDC providers and tenant SSO)
- ShedLock 7.x (distributed scheduled cleanup jobs)
- Spring WebSocket + STOMP/SockJS (real-time in-app notifications)
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

# Start infrastructure dependencies (PostgreSQL, RabbitMQ, Redis, MailHog)
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

| Variable                         | Default                               | Description                                      |
| -------------------------------- | ------------------------------------- | ------------------------------------------------ |
| `ROLLOUT_MODE`                   | `MULTI_TENANT`                        | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT` |
| `DB_HOST`                        | `localhost`                           | PostgreSQL host                                  |
| `DB_PORT`                        | `5432`                                | PostgreSQL port                                  |
| `DB_NAME`                        | `iam`                                 | Database name                                    |
| `DB_USERNAME`                    | `svc_iam_dba`                         | Database user                                    |
| `DB_PASSWORD`                    | `svc_iam_dba`                         | Database password                                |
| `RABBITMQ_HOST`                  | `localhost`                           | RabbitMQ host                                    |
| `RABBITMQ_PORT`                  | `5672`                                | RabbitMQ AMQP port                               |
| `RABBITMQ_USERNAME`              | `svc_iam_rmq`                         | RabbitMQ user                                    |
| `RABBITMQ_PASSWORD`              | `svc_iam_rmq`                         | RabbitMQ password                                |
| `REDIS_HOST`                     | `localhost`                           | Redis host                                       |
| `REDIS_PORT`                     | `6379`                                | Redis port                                       |
| `REDIS_PASSWORD`                 | _(empty)_                             | Redis password                                   |
| `REDIS_DATABASE`                 | `0`                                   | Redis database index                             |
| `REDIS_TIMEOUT`                  | `PT2S`                                | Redis operation timeout                          |
| `MAIL_HOST`                      | `localhost`                           | SMTP host                                        |
| `MAIL_PORT`                      | `587`                                 | SMTP port                                        |
| `MAIL_FROM`                      | `noreply@iqkv.com`                    | Sender address                                   |
| `OBJECTSTORAGE_ENDPOINT`         | `http://localhost:9000`               | S3-compatible storage endpoint (MinIO/AWS S3)    |
| `OBJECTSTORAGE_ACCESS_KEY`       | `minioadmin`                          | S3 access key                                    |
| `OBJECTSTORAGE_SECRET_KEY`       | `minioadmin`                          | S3 secret key                                    |
| `OBJECTSTORAGE_BUCKET_NAME`      | `iam-avatars`                         | S3 bucket name for avatar uploads                |
| `OBJECTSTORAGE_REGION`           | `us-east-1`                           | S3 region                                        |
| `OBJECTSTORAGE_SSL`              | `false`                               | Use SSL for S3 connections                       |
| `JWT_PRIVATE_KEY_PATH`           | `classpath:keys/private.pem`          | RS256 private key                                |
| `JWT_PUBLIC_KEY_PATH`            | `classpath:keys/public.pem`           | RS256 public key                                 |
| `APP_BASE_URL`                   | `http://localhost:3000`               | Frontend base URL (used in email links)          |
| `OAUTH2_ENABLED_PROVIDERS`       | `google,github`                       | Enabled built-in OAuth2/OIDC providers           |
| `OAUTH2_BASE_URL`                | `http://localhost:8080`               | Public IAM base URL used in OAuth redirect URIs  |
| `OAUTH2_POST_LOGIN_REDIRECT_URI` | `http://localhost:3000/auth/callback` | Frontend callback URI for browser OAuth flows    |
| `OAUTH2_AUTO_LINK_ENABLED`       | `true`                                | Allow verified-email auto-linking for OIDC       |
| `OIDC_ENCRYPTION_KEY`            | _(empty)_                             | AES-256-GCM key for tenant OIDC client secrets   |
| `OAUTH2_STATE_TTL`               | `PT10M`                               | Signed OAuth state / PKCE verifier lifetime      |
| `OAUTH2_STATE_TTL_MAX`           | `PT15M`                               | Maximum allowed OAuth state lifetime             |
| `OAUTH2_GOOGLE_CLIENT_ID`        | _(empty)_                             | Google OAuth client ID                           |
| `OAUTH2_GOOGLE_CLIENT_SECRET`    | _(empty)_                             | Google OAuth client secret                       |
| `OAUTH2_GITHUB_CLIENT_ID`        | _(empty)_                             | GitHub OAuth client ID                           |
| `OAUTH2_GITHUB_CLIENT_SECRET`    | _(empty)_                             | GitHub OAuth client secret                       |
| `OAUTH2_MICROSOFT_CLIENT_ID`     | _(empty)_                             | Microsoft OAuth client ID                        |
| `OAUTH2_MICROSOFT_CLIENT_SECRET` | _(empty)_                             | Microsoft OAuth client secret                    |
| `OAUTH2_MICROSOFT_TENANT_ID`     | `common`                              | Microsoft tenant identifier for issuer discovery |
| `DEFAULT_TENANT_KEY`             | _(empty)_                             | Default tenant key for `SINGLE_TENANT` mode      |
| `DEFAULT_TENANT_NAME`            | `Acme Corp.`                          | Default tenant display name                      |
| `INVITATION_TOKEN_TTL`           | `PT72H`                               | Invitation token lifetime (ISO-8601 duration)    |

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

## Project Structure

```
src/main/java/com/iqkv/foundation/iamservice/
├── announcement/     # Site-wide announcements, fan-out service, admin + public REST resources
├── authentication/   # Signin, signup, token refresh, signout, validate
├── denylist/         # JTI denylist for token revocation
├── email/            # Email verification token + reaper job
├── invitation/       # Token-based invite flow + reaper job
├── locale/           # Supported locales
├── lockout/          # Brute-force login lockout per identity
├── magiclink/        # Magic link passwordless authentication flow + reaper job
├── membership/       # TenantMembership, authorities (TENANT_OWNER, ADMIN, MEMBER)
├── notification/     # In-app user notifications (DB persistence + WebSocket push)
├── oauth2/           # OAuth2/OIDC login, account linking, tenant SSO, admin identity remediation
├── passwordreset/    # Forgot/reset password flow + reaper job
├── platformadmin/    # Platform operator self-service account
├── platformauthority/# Platform-level authorities (PLATFORM_ADMIN)
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
- **Messaging**: RabbitMQ for async domain events (tenant provisioning, notifications, announcements); ShedLock guards scheduled cleanup jobs (expired tokens, stuck tenants); dead-letter exchange (`iqkv.dlx`) routes failed messages to `iqkv.dlq`
- **Security**: Spring Security + JJWT RS256; JTI denylist for token revocation; `last_global_signout_at` for session-wide invalidation; brute-force lockout per identity
- **OAuth2/OIDC**: Built-in Google, GitHub, and Microsoft providers plus tenant-configurable OIDC; browser flows use signed JWT state, server-side PKCE, Redis-backed verifier storage, and verified-email provisioning/linking
- **Multi-tenancy**: Per-tenant schema isolation managed by Liquibase; `TenantMembership` carries per-tenant authorities (`TENANT_OWNER`, `ADMIN`, `MEMBER`); tenant-scoped endpoints require a JWT scoped to the target tenant via `X-Tenant-ID` header
- **In-app notifications**: `NotificationConsumer` listens on `iqkv.iam.notifications` — sends email, persists `UserNotification` to DB, and pushes to `/user/{userId}/queue/notifications` via STOMP; `AnnouncementConsumer` listens on `iqkv.iam.announcements` — streams all users via MyBatis cursor in batches of 1000, bulk-inserts notifications, and broadcasts to `/topic/announcements`
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; IAM publishes canonical mode via `/actuator/info` under `platform.rollout-mode`; service fails readiness on invalid/missing mode
- **Single-tenant mode**: Strategy pattern (`SignupStrategy`, `TenantBootstrapStrategy`) branches behavior at startup and signup; `SingleTenantBootstrapStrategy` idempotently provisions the default tenant on `ApplicationReadyEvent`; `SingleTenantSignupStrategy` joins users to the default tenant with `MEMBER` authority — no tenant creation, no `TENANT_OWNER` grant
- **Platform tenant**: Every user is automatically added to the hidden platform tenant (tenantKey = "platform") as a `MEMBER` — used for single-tenant mode and as a default workspace in multi-tenant mode; enables seamless single-to-multi tenant switching
- **Invitations**: Token-based signup-by-invitation flow; token resolves tenant context so accept endpoints are tenant-agnostic; `authority` defaults to `MEMBER`; ShedLock-guarded reaper expires stale tokens
- **Email**: Thymeleaf-rendered transactional emails via Spring Mail; MailHog for local testing
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes; `PlatformModeHealthIndicator` exposes rollout mode in `/actuator/health`; `PlatformModeInfoContributor` publishes canonical `platform.rollout-mode` in `/actuator/info`
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (60% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
