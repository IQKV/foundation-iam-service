## 📜 API Documentation

Base path: `/api/v1/iam`

All endpoints require a valid RS256 JWT issued by this service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

---

### Authentication

| Method | Path                | Auth          | Description                              |
| ------ | ------------------- | ------------- | ---------------------------------------- |
| `POST` | `/auth/signup`      | public        | Register user and create tenant          |
| `POST` | `/auth/signin`      | `X-Tenant-ID` | Sign in, receive token pair              |
| `POST` | `/auth/refresh`     | `X-Tenant-ID` | Rotate access + refresh tokens           |
| `POST` | `/auth/signout`     | JWT           | Revoke current session (JTI denylist)    |
| `POST` | `/auth/signout-all` | JWT           | Revoke all sessions globally             |
| `POST` | `/auth/validate`    | JWT           | Validate token for gateway introspection |

`POST /auth/signup` creates a global user account, a new tenant, and a `TENANT_OWNER` membership in one step.
Returns `201` with `tenantStatus=PROVISIONING`; poll `GET /tenants/{tenantKey}` until `ACTIVE`.

---

### User Profile

| Method   | Path             | Auth   | Description                               |
| -------- | ---------------- | ------ | ----------------------------------------- |
| `GET`    | `/users/me`      | JWT    | Get own profile                           |
| `PATCH`  | `/users/me`      | JWT    | Update own profile (firstName, lastName)  |
| `DELETE` | `/users/me`      | JWT    | Remove own membership from current tenant |
| `POST`   | `/users/tenants` | public | Discover tenants by credentials           |

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

### Tenant Management

| Method   | Path                                      | Auth                                          | Description                 |
| -------- | ----------------------------------------- | --------------------------------------------- | --------------------------- |
| `GET`    | `/tenants/{tenantKey}`                    | JWT `TENANT_OWNER`                            | Get tenant status           |
| `PATCH`  | `/tenants/{tenantKey}/status`             | JWT `TENANT_OWNER`                            | Transition tenant status    |
| `POST`   | `/tenants/{tenantKey}/retry-provisioning` | JWT `TENANT_OWNER`                            | Retry failed provisioning   |
| `POST`   | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | Send invitation email       |
| `GET`    | `/tenants/{tenantKey}/invitations`        | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | List pending invitations    |
| `DELETE` | `/tenants/{tenantKey}/invitations/{id}`   | JWT `TENANT_OWNER` or `ADMIN` + `X-Tenant-ID` | Revoke a pending invitation |

`POST /tenants/{tenantKey}/retry-provisioning` is only valid when the tenant is in `PROVISIONING_FAILED` state.

---

### Invitations (public)

| Method | Path                          | Auth   | Description                                |
| ------ | ----------------------------- | ------ | ------------------------------------------ |
| `GET`  | `/invitations/{token}`        | public | Preview invitation — token resolves tenant |
| `POST` | `/invitations/{token}/accept` | public | Accept invitation — token resolves tenant  |

Invitation tokens are time-limited (default 72 hours). Expired, revoked, or non-existent tokens return `404`.

For new users: `firstName`, `lastName`, and `password` are required on accept.
For existing users: only `password` is required (used to verify identity).

---

### Platform Admin

| Method   | Path                | Auth | Description                     |
| -------- | ------------------- | ---- | ------------------------------- |
| `GET`    | `/admin/users`      | JWT  | List all users (paginated)      |
| `GET`    | `/admin/users/{id}` | JWT  | Get user by ID                  |
| `POST`   | `/admin/users`      | JWT  | Create user                     |
| `PUT`    | `/admin/users/{id}` | JWT  | Replace user (full update)      |
| `PATCH`  | `/admin/users/{id}` | JWT  | Partially update user           |
| `DELETE` | `/admin/users/{id}` | JWT  | Delete user and all memberships |

---

### JWKS

| Method | Path                     | Auth   | Description                                   |
| ------ | ------------------------ | ------ | --------------------------------------------- |
| `GET`  | `/.well-known/jwks.json` | public | RSA public key set for RS256 JWT verification |

Consumed by the gateway and any downstream service that validates JWTs locally.

---

### Interactive Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running locally.
