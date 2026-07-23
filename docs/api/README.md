## 📜 API Documentation

Base path: `/api/v1/iam`

All endpoints require a valid RS256 JWT issued by this service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

---

### Authentication

| Method | Path                              | Auth                   | Description                                                       |
| ------ | --------------------------------- | ---------------------- | ----------------------------------------------------------------- |
| `POST` | `/auth/signup`                    | public                 | Register user and add as MEMBER to platform tenant                |
| `GET`  | `/auth/signup/status/{tenantKey}` | public                 | Poll tenant provisioning status                                   |
| `POST` | `/auth/signin`                    | public + `X-Tenant-ID` | Sign in, receive RS256 token pair                                 |
| `POST` | `/auth/exchange`                  | JWT                    | Exchange a Bearer access token for a new tenant-scoped token pair |
| `POST` | `/auth/admin/signin`              | public                 | Platform admin sign-in (platform-scoped token, null `tenant_id`)  |
| `POST` | `/auth/admin/refresh`             | public                 | Refresh platform-scoped token pair                                |
| `POST` | `/auth/refresh`                   | public                 | Rotate access + refresh tokens                                    |
| `POST` | `/auth/signout`                   | JWT                    | Revoke current session (JTI denylist)                             |
| `POST` | `/auth/signout-all`               | JWT                    | Revoke all sessions globally                                      |
| `POST` | `/auth/validate`                  | JWT                    | Validate token for gateway introspection                          |

### Magic Link Authentication

| Method | Path                        | Auth   | Description                                                                |
| ------ | --------------------------- | ------ | -------------------------------------------------------------------------- |
| `POST` | `/auth/magic-link/initiate` | public | Initiate magic link authentication (email sent if user exists; always 204) |
| `POST` | `/auth/magic-link/resend`   | public | Resend magic link (if token exists; always 204)                            |
| `POST` | `/auth/magic-link/exchange` | public | Exchange magic link token for RS256 access + refresh token pair            |

`POST /auth/magic-link/initiate` and `POST /auth/magic-link/resend` always return 204 to prevent email enumeration. Magic link tokens are time-limited (configurable via AuthProperties.MagicLink.tokenTtl, defaults to 24 hours; rate limited to configurable requests per configurable window. In single-tenant mode, the default tenant is automatically used; in multi-tenant mode, tenantKey must be provided.

---

### OAuth2/OIDC Authentication

| Method   | Path                                         | Auth   | Description                                                                        |
| -------- | -------------------------------------------- | ------ | ---------------------------------------------------------------------------------- |
| `GET`    | `/auth/oauth2/providers`                     | public | List enabled OAuth2/OIDC providers that resolve to active client registrations     |
| `GET`    | `/auth/oauth2/authorize`                     | public | Start browser-based OAuth2/OIDC login flow with signed state + server-side PKCE    |
| `GET`    | `/auth/oauth2/callback`                      | public | Handle provider callback and redirect to the configured frontend callback URI      |
| `POST`   | `/auth/oauth2/exchange`                      | public | Exchange provider authorization code + PKCE verifier for IAM access/refresh tokens |
| `GET`    | `/auth/oauth2/link/{provider}`               | JWT    | Start account-linking flow for the current user                                    |
| `GET`    | `/auth/oauth2/link/{provider}/authorize-url` | JWT    | Get the provider authorization URL for account linking (UI-driven redirect)        |
| `GET`    | `/auth/oauth2/link/callback`                 | public | Handle provider callback for account linking                                       |
| `DELETE` | `/auth/oauth2/link/{provider}`               | JWT    | Unlink an external provider from the current account                               |
| `GET`    | `/auth/oauth2/identities`                    | JWT    | List linked external identities for the current user                               |

`GET /auth/oauth2/authorize` accepts `provider` and optional `tenantKey` query parameters. The service signs the OAuth state, stores the PKCE verifier in Redis, and redirects the browser to the selected provider.

`GET /auth/oauth2/callback` and `GET /auth/oauth2/link/callback` are provider-facing redirect endpoints. They validate signed state, restore the PKCE verifier, exchange the authorization code, and redirect back to the configured frontend callback URI.

`POST /auth/oauth2/exchange` supports SPA or native-app flows that already obtained an authorization code and PKCE verifier.

Example request:

```json
{
    "provider": "google",
    "code": "<authorization-code>",
    "codeVerifier": "<pkce-code-verifier>",
    "redirectUri": "http://localhost:3000/auth/callback",
    "tenantKey": "platform"
}
```

`DELETE /auth/oauth2/link/{provider}` returns `204` when the provider is absent and rejects removal of the last sign-in method if the account has no password set.

---

`POST /auth/signup` creates a global user account and adds them as a `MEMBER` to the platform tenant.
Tenants are created afterwards via `POST /tenants`, which provisions the tenant and grants the caller `TENANT_OWNER`.

---

### Platform Admin Account

| Method  | Path                      | Auth                 | Description                                    |
| ------- | ------------------------- | -------------------- | ---------------------------------------------- |
| `GET`   | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Get own platform operator profile              |
| `PATCH` | `/auth/admin/me`          | JWT `PLATFORM_ADMIN` | Update own profile (firstName, lastName)       |
| `POST`  | `/auth/admin/me/password` | JWT `PLATFORM_ADMIN` | Change own password (invalidates all sessions) |

---

### User Profile

| Method   | Path                       | Auth                      | Description                                                               |
| -------- | -------------------------- | ------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/users/me`                | JWT + `X-Tenant-ID`       | Get own profile                                                           |
| `PATCH`  | `/users/me`                | JWT + `X-Tenant-ID`       | Update own profile (firstName, lastName)                                  |
| `DELETE` | `/users/me`                | JWT + `X-Tenant-ID`       | Remove own membership from current tenant                                 |
| `POST`   | `/users/me/password`       | JWT + `X-Tenant-ID`       | Change own password (requires current password; invalidates all sessions) |
| `POST`   | `/users/me/avatar`         | JWT + `X-Tenant-ID`       | Initiate avatar upload (returns presigned S3 URL)                         |
| `POST`   | `/users/me/avatar/confirm` | JWT + `X-Tenant-ID`       | Confirm avatar upload (persists avatar URL after S3 upload)               |
| `DELETE` | `/users/me/avatar`         | JWT + `X-Tenant-ID`       | Delete current user's avatar                                              |
| `POST`   | `/users/tenants`           | public (credential-gated) | Discover tenants by credentials                                           |
| `GET`    | `/users/me/memberships`    | JWT                       | List current user's tenant memberships                                    |

---

### Avatar Upload

The avatar upload feature uses a two-phase flow for secure, direct-to-S3 uploads:

1. **Initiate**: `POST /users/me/avatar` returns a presigned S3 PUT URL (valid for 15 minutes)
2. **Upload**: Client uploads the image file directly to the presigned URL
3. **Confirm**: `POST /users/me/avatar/confirm` persists the avatar URL in the user profile

**Initiate Upload**

```http
POST /api/v1/iam/users/me/avatar
Authorization: Bearer <jwt>
X-Tenant-ID: <tenant-key>
```

Response:

```json
{
    "presignedUploadUrl": "https://s3.amazonaws.com/bucket/avatars/user-id/timestamp.jpg?...",
    "objectKey": "avatars/user-id/timestamp.jpg",
    "expiresInMinutes": 15
}
```

**Upload to S3**

```http
PUT <presignedUploadUrl>
Content-Type: image/jpeg
Content-Length: <file-size>

<binary-image-data>
```

**Confirm Upload**

```http
POST /api/v1/iam/users/me/avatar/confirm
Authorization: Bearer <jwt>
X-Tenant-ID: <tenant-key>
Content-Type: application/json

{
  "objectKey": "avatars/user-id/timestamp.jpg"
}
```

Response:

```json
{
    "avatarUrl": "https://s3.amazonaws.com/bucket/avatars/user-id/timestamp.jpg"
}
```

**Delete Avatar**

```http
DELETE /api/v1/iam/users/me/avatar
Authorization: Bearer <jwt>
X-Tenant-ID: <tenant-key>
```

Returns `204 No Content`. Removes the avatar from both S3 and the user profile.

**Notes:**

- Presigned URLs expire after 15 minutes
- Object keys follow the pattern: `avatars/{userId}/{timestamp}.jpg`
- Old avatars are automatically deleted when a new one is uploaded
- The `avatarUrl` field is included in all user profile responses (`GET /users/me`)

---

### Password Reset

| Method | Path                     | Auth   | Description                            |
| ------ | ------------------------ | ------ | -------------------------------------- |
| `POST` | `/users/password/forgot` | public | Initiate password reset (rate-limited) |
| `POST` | `/users/password/reset`  | public | Complete password reset with token     |

`POST /users/password/forgot` always returns `200` to avoid email enumeration.
Reset tokens expire after 1 hour (configurable). Rate-limited to 3 requests per 15-minute window.

---

### Email Verification

| Method | Path                               | Auth   | Description                              |
| ------ | ---------------------------------- | ------ | ---------------------------------------- |
| `POST` | `/users/email/verify`              | public | Verify email address with one-time token |
| `POST` | `/users/email/resend-verification` | public | Resend verification email (rate-limited) |

---

### In-App Notifications

| Method   | Path                                | Auth | Description                                                         |
| -------- | ----------------------------------- | ---- | ------------------------------------------------------------------- |
| `GET`    | `/users/notifications`              | JWT  | Paginated list of notifications for the current user                |
| `PATCH`  | `/users/notifications`              | JWT  | Bulk partial update — body `{ "isRead": true }` marks all as read   |
| `DELETE` | `/users/notifications`              | JWT  | Delete all notifications for the current user                       |
| `GET`    | `/users/notifications/unread/count` | JWT  | Returns `{ "unreadCount": N }` — use for badge rendering            |
| `PATCH`  | `/users/notifications/{id}`         | JWT  | Partial update on a single notification — body `{ "isRead": true }` |
| `DELETE` | `/users/notifications/{id}`         | JWT  | Delete a single notification (ownership-enforced)                   |

`GET /users/notifications` query parameters:

- `limit` — page size, default `10`
- `offset` — zero-based offset, default `0`
- `isRead` — optional boolean filter; omit to return all

Response shape:

```json
{
    "items": [{ "id": "...", "type": "...", "severity": "INFO", "title": "...", "message": "...", "payload": {}, "isRead": false, "createdAt": "...", "readAt": null }],
    "totalElements": 42,
    "unreadCount": 7
}
```

`PATCH` body (single or bulk):

```json
{ "isRead": true }
```

Real-time delivery is available via WebSocket — see the [WebSocket section](#websocket) below.

---

### Announcements (Public)

| Method | Path             | Auth   | Description                                     |
| ------ | ---------------- | ------ | ----------------------------------------------- |
| `GET`  | `/announcements` | public | Get active site-wide announcements for a locale |

Accepts a `locale` query parameter (default `en-US`). Returns only `PUBLISHED` announcements.

---

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

`POST /tenants/{tenantKey}/retry-provisioning` is only valid when the tenant is in `PROVISIONING_FAILED` state.

---

### Tenant SSO Configuration

| Method   | Path           | Auth                                | Description                             |
| -------- | -------------- | ----------------------------------- | --------------------------------------- |
| `GET`    | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Get current tenant custom OIDC settings |
| `PUT`    | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Create or replace tenant OIDC settings  |
| `DELETE` | `/tenants/sso` | JWT `TENANT_OWNER`/`PLATFORM_ADMIN` | Delete tenant OIDC settings             |

Tenant SSO configuration is tenant-scoped and uses the current `X-Tenant-ID` context. Stored client secrets are encrypted at rest with `OIDC_ENCRYPTION_KEY`. The `clientSecret` is write-only and is never returned by `GET /tenants/sso`.

---

### Invitations (Tenant-scoped)

| Method   | Path                                    | Auth                                       | Description                 |
| -------- | --------------------------------------- | ------------------------------------------ | --------------------------- |
| `POST`   | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Send invitation email       |
| `GET`    | `/tenants/{tenantKey}/invitations`      | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | List pending invitations    |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}` | JWT `TENANT_OWNER`/`ADMIN` + `X-Tenant-ID` | Revoke a pending invitation |

---

### Invitations (Public)

| Method | Path                          | Auth   | Description                                |
| ------ | ----------------------------- | ------ | ------------------------------------------ |
| `GET`  | `/invitations/{token}`        | public | Preview invitation — token resolves tenant |
| `POST` | `/invitations/{token}/accept` | public | Accept invitation — token resolves tenant  |

Invitation tokens are time-limited (default 72 hours). Expired, revoked, or non-existent tokens return `404`.

For new users: `firstName`, `lastName`, and `password` are required on accept.
For existing users: only `password` is required (used to verify identity).

---

### Locales

| Method | Path       | Auth   | Description            |
| ------ | ---------- | ------ | ---------------------- |
| `GET`  | `/locales` | public | Get all active locales |

---

### Platform Admin — Users

| Method   | Path                            | Auth                 | Description                                        |
| -------- | ------------------------------- | -------------------- | -------------------------------------------------- |
| `GET`    | `/admin/users`                  | JWT `PLATFORM_ADMIN` | List all users (paginated, filterable)             |
| `GET`    | `/admin/users/count`            | JWT `PLATFORM_ADMIN` | Count users                                        |
| `GET`    | `/admin/users/{id}`             | JWT `PLATFORM_ADMIN` | Get user by UUID                                   |
| `POST`   | `/admin/users`                  | JWT `PLATFORM_ADMIN` | Create user with temporary password                |
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

---

### Platform Admin — Tenants

| Method   | Path                                                      | Auth                 | Description                                 |
| -------- | --------------------------------------------------------- | -------------------- | ------------------------------------------- |
| `GET`    | `/admin/tenants`                                          | JWT `PLATFORM_ADMIN` | List all tenants (paginated, filterable)    |
| `GET`    | `/admin/tenants/count`                                    | JWT `PLATFORM_ADMIN` | Count tenants                               |
| `GET`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Get tenant by key                           |
| `PUT`    | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Rename tenant                               |
| `PATCH`  | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Partially update tenant                     |
| `DELETE` | `/admin/tenants/{tenantKey}`                              | JWT `PLATFORM_ADMIN` | Delete tenant and all associated data       |
| `GET`    | `/admin/tenants/{tenantKey}/members`                      | JWT `PLATFORM_ADMIN` | List tenant members (paginated, filterable) |
| `GET`    | `/admin/tenants/{tenantKey}/members/count`                | JWT `PLATFORM_ADMIN` | Count tenant members                        |
| `GET`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Get member's tenant authorities             |
| `PUT`    | `/admin/tenants/{tenantKey}/members/{userId}/authorities` | JWT `PLATFORM_ADMIN` | Replace member's tenant authorities         |

---

### Platform Admin — Invitations

| Method   | Path                       | Auth                 | Description                                                 |
| -------- | -------------------------- | -------------------- | ----------------------------------------------------------- |
| `GET`    | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | List invitations across all tenants (paginated, filterable) |
| `GET`    | `/admin/invitations/count` | JWT `PLATFORM_ADMIN` | Count invitations (with optional filters)                   |
| `GET`    | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Get invitation by UUID                                      |
| `POST`   | `/admin/invitations`       | JWT `PLATFORM_ADMIN` | Propose invitation for any active tenant                    |
| `DELETE` | `/admin/invitations/{id}`  | JWT `PLATFORM_ADMIN` | Revoke invitation                                           |

---

### Platform Admin — Announcements

| Method   | Path                                | Auth                 | Description                                         |
| -------- | ----------------------------------- | -------------------- | --------------------------------------------------- |
| `POST`   | `/admin/announcements`              | JWT `PLATFORM_ADMIN` | Create announcement with multi-lingual translations |
| `PUT`    | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Update announcement and its translations            |
| `DELETE` | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Delete announcement                                 |
| `POST`   | `/admin/announcements/{id}/publish` | JWT `PLATFORM_ADMIN` | Trigger fan-out to all users                        |
| `GET`    | `/admin/announcements/{id}`         | JWT `PLATFORM_ADMIN` | Get announcement by UUID                            |
| `GET`    | `/admin/announcements`              | JWT `PLATFORM_ADMIN` | List all announcements (paginated)                  |

Publishing an announcement (`POST /admin/announcements/{id}/publish`) triggers an async fan-out via RabbitMQ:
the service streams all users in batches of 1000, creates a `UserNotification` record per user, and broadcasts
a real-time message to the `/topic/announcements` WebSocket topic.

---

### Platform Admin — Platform Health Notes

Operator-only scratch-pad for tracking incidents, maintenance windows, and operational observations.
Notes are strictly internal — they are never surfaced to tenants or end-users.

| Method   | Path                          | Auth                 | Description                                                            |
| -------- | ----------------------------- | -------------------- | ---------------------------------------------------------------------- |
| `GET`    | `/admin/platform-notes`       | JWT `PLATFORM_ADMIN` | List notes (paginated, filterable by search / severity / status)       |
| `GET`    | `/admin/platform-notes/count` | JWT `PLATFORM_ADMIN` | Count notes matching the supplied filters                              |
| `GET`    | `/admin/platform-notes/{id}`  | JWT `PLATFORM_ADMIN` | Get note by UUID                                                       |
| `POST`   | `/admin/platform-notes`       | JWT `PLATFORM_ADMIN` | Create note (status defaults to `OPEN`; actor recorded as `createdBy`) |
| `PUT`    | `/admin/platform-notes/{id}`  | JWT `PLATFORM_ADMIN` | Replace note (all fields required)                                     |
| `PATCH`  | `/admin/platform-notes/{id}`  | JWT `PLATFORM_ADMIN` | Partially update note (e.g. quick status transition)                   |
| `DELETE` | `/admin/platform-notes/{id}`  | JWT `PLATFORM_ADMIN` | Permanently delete note                                                |

**List query parameters** (`GET /admin/platform-notes`):

| Parameter  | Type                                                            | Description                        |
| ---------- | --------------------------------------------------------------- | ---------------------------------- |
| `page`     | integer (0-based)                                               | Page number, default `0`           |
| `size`     | integer                                                         | Page size, default `20`            |
| `search`   | string                                                          | Free-text search on title and body |
| `severity` | `INFO` \| `WARNING` \| `CRITICAL`                               | Filter by severity                 |
| `status`   | `OPEN` \| `RESOLVED` \| `ARCHIVED`                              | Filter by status                   |
| `sortBy`   | `title` \| `severity` \| `status` \| `createdAt` \| `updatedAt` | Sort field, default `createdAt`    |
| `sortDir`  | `asc` \| `desc`                                                 | Sort direction, default `desc`     |

**Note severity levels**: `INFO` (blue) → `WARNING` (orange) → `CRITICAL` (red)

**Note status lifecycle**: `OPEN` → `RESOLVED` or `ARCHIVED`; `ARCHIVED` is a soft-retain state for audit — prefer archiving over deleting when history matters.

> This endpoint also serves as the backend for the `platform-health-notes` addon in the platform admin UI, which demonstrates the full addon capability set (routing, tabs, forms, validation, data fetching, mutations) against a real secured API.

---

### Platform Admin — OIDC

| Method   | Path                                         | Auth                 | Description                                         |
| -------- | -------------------------------------------- | -------------------- | --------------------------------------------------- |
| `GET`    | `/admin/oidc/users/{userId}/identities`      | JWT `PLATFORM_ADMIN` | List linked OIDC identities for a user              |
| `DELETE` | `/admin/oidc/users/{userId}/identities/{id}` | JWT `PLATFORM_ADMIN` | Force-unmerge a linked identity from a user account |

The admin delete endpoint is a remediation path for incorrectly linked external identities. It intentionally bypasses the self-service unlink guard that protects the last usable sign-in method on an account.

---

### JWKS

| Method | Path                     | Auth   | Description                                   |
| ------ | ------------------------ | ------ | --------------------------------------------- |
| `GET`  | `/.well-known/jwks.json` | public | RSA public key set for RS256 JWT verification |

Consumed by the gateway and any downstream service that validates JWTs locally.

---

### WebSocket

The service exposes a STOMP-over-SockJS endpoint for real-time push notifications.

**Connection endpoint**: `ws://host/api/v1/iam/ws` (SockJS fallback enabled)

**Destinations**:

| Destination                          | Direction          | Description                                                                                         |
| ------------------------------------ | ------------------ | --------------------------------------------------------------------------------------------------- |
| `/user/{userId}/queue/notifications` | server → client    | Per-user notification pushed after a transactional event (signup, password reset, invitation, etc.) |
| `/topic/announcements`               | server → broadcast | Global broadcast when a site-wide announcement is published                                         |

Clients must authenticate the WebSocket handshake with a valid Bearer token.
The user destination prefix is `/user`; the application prefix is `/app`.

---

> Auth legend: `public` = no token required; `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char alphanumeric tenantKey header required for tenant-scoped endpoints.

---

### Interactive Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running locally.
OpenAPI spec is served at `http://localhost:8080/api-docs`.
