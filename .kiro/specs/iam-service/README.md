# Identity and Access Management — Spec Overview

## Current Scope

Multi-tenant Spring Boot 3.x / Java 21 IAM microservice with:

- User registration + tenant provisioning (async via RabbitMQ)
- JWT RS256 authentication with tenant_id and email_verified claims
- Account lockout, token denylist, signout-all
- Email verification via secure token (signup flow)
- Multi-tenant membership model (GitHub orgs pattern)
- MyBatis 3.x persistence, Liquibase migrations, ShedLock scheduled jobs

## Email Templates (Current)

```
resources/templates/email/
└── signup/
    ├── verify-email.html     Sent on registration and resend-verification
    └── email-verified.html   Sent after successful email verification
```

i18n via `messages*.properties` (en, es, it, ru). Add a new `messages_<lang>.properties` to support additional languages — no code changes required.

## Email Templates (Planned — Future Iterations)

| Template                              | Trigger                        | Iteration |
| ------------------------------------- | ------------------------------ | --------- |
| `password-reset/initiate.html`        | User requests password reset   | v2        |
| `password-reset/confirmed.html`       | Password successfully changed  | v2        |
| `invitation/invitation-email.html`    | User invited to join a tenant  | v3        |
| `invitation/invitation-accepted.html` | Invitee accepts the invitation | v3        |

## Out of Scope (Current Iteration)

- Password reset flow (`POST /users/password/forgot`, `POST /users/password/reset`)
- Tenant member invitation flow
- Hard-delete / schema cleanup for DELETED tenants
- Separate notification microservice (using in-process `NotificationConsumer` for now)

## Spec Files

| File              | Purpose                                                     |
| ----------------- | ----------------------------------------------------------- |
| `requirements.md` | Functional requirements and acceptance criteria             |
| `design.md`       | Architecture, domain model, component design, code examples |
| `tasks.md`        | Ordered implementation tasks                                |
