# Cadence Auth Service — Module 1: Authentication & Authorization

Part of the **Cadence AI Hiring Platform** microservices suite. Owns `auth_db`
exclusively, per the platform's database-per-service strategy. This is the
first service in Phase 1 of the roadmap (Gateway, Config Server, Eureka,
Auth, Company, Job, Candidate, Resume, Notification).

---

## 1. Functional Requirements

| # | Requirement |
|---|---|
| FR1 | Register candidates, recruiters, and company admins with role-based defaults |
| FR2 | Email/password login issuing short-lived JWT access tokens + rotating refresh tokens |
| FR3 | "Remember me" — extends refresh token lifetime from 7 to 30 days |
| FR4 | Refresh token rotation with reuse detection (theft protection) |
| FR5 | Google OAuth2 login (social sign-in), auto-provisioning candidate accounts |
| FR6 | Role-Based Access Control with a separate Permission layer (fine-grained) |
| FR7 | Forgot / reset password via emailed, single-use, time-boxed token |
| FR8 | Change password while authenticated |
| FR9 | Email verification at registration, with resend capability |
| FR10 | TOTP-based Multi-Factor Authentication (RFC 6238), with recovery codes |
| FR11 | Account lockout after N consecutive failed logins, with auto-unlock window |
| FR12 | Logout (single device or all devices), with immediate token revocation |
| FR13 | Session/device visibility and per-device revocation |
| FR14 | Full audit trail of security-relevant events |
| FR15 | Kafka events published for downstream services (Notification, Analytics) |

## 2. Non-Functional Requirements (selected)

- Stateless authentication → horizontal scaling behind Kubernetes without sticky sessions.
- p95 login latency budget: DB write path only for refresh token + audit log (async); JWT verification is in-memory on every other service.
- Passwords: BCrypt strength 12. Tokens: SHA-256 hashed at rest, opaque random 512-bit raw values.
- No account enumeration: `forgot-password` and `resend-verification` return identical responses whether or not the email exists.

---

## 3. Database Design (`auth_db`)

### 3.1 Entity Diagram (textual ERD)

```
 users ───< user_roles >─── roles ───< role_permissions >─── permissions
   │
   ├──< refresh_tokens
   ├──< password_reset_tokens
   ├──< email_verification_tokens
   ├──1:1 mfa_secrets
   ├──< user_sessions
   └──< audit_logs
```

### 3.2 Tables

| Table | Purpose | Key columns |
|---|---|---|
| `users` | Core identity | id (UUID PK), email (unique), password_hash, user_type, status, failed_login_attempts, account_locked_until, mfa_enabled, email_verified, auth_provider, company_id (logical FK → company_db) |
| `roles` | RBAC roles | id, name (unique) |
| `permissions` | Fine-grained capabilities | id, name (unique) |
| `role_permissions` | Role↔Permission join | role_id, permission_id |
| `user_roles` | User↔Role join | user_id, role_id |
| `refresh_tokens` | Rotating refresh tokens | token_hash (unique, SHA-256), expires_at, revoked, replaced_by (rotation chain), remember_me |
| `password_reset_tokens` | One-time reset links | token_hash, expires_at, used |
| `email_verification_tokens` | One-time verify links | token_hash, expires_at, used |
| `mfa_secrets` | TOTP secrets + recovery codes | user_id (unique), secret_key, confirmed, recovery_codes (hashed, CSV) |
| `user_sessions` | Device/session metadata | user_id, refresh_token_id, device_info, ip_address, is_active |
| `audit_logs` | Security event trail | user_id, event_type, ip_address, user_agent, metadata (jsonb) |

Full DDL with seed roles/permissions: [`src/main/resources/db/migration/V1__init_auth_schema.sql`](src/main/resources/db/migration/V1__init_auth_schema.sql), applied automatically by Flyway on boot.

**Why these design choices:**
- **UUID PKs** — safe to generate anywhere, no cross-service sequence coordination, no ID collisions when data is later referenced by other microservices.
- **Hashed tokens only** — a DB leak alone can never be replayed as a valid refresh/reset/verification token.
- **Separate Role/Permission tables** instead of a role string on `users` — supports the platform's real requirement (Company Admin vs Recruiter vs Candidate each need different, evolving permission sets) without code changes when permissions are added.
- **`replaced_by` rotation chain** on refresh tokens — lets us detect and respond to token theft (reuse of an already-rotated token revokes the entire session family).

---

## 4. Folder Structure

```
auth-service/
├── src/main/java/com/cadence/authservice/
│   ├── config/          SecurityConfig, RedisConfig, SwaggerConfig, Kafka config, JPA auditing
│   ├── constant/        Enums & literal constants (RoleName, KafkaTopics, SecurityConstants...)
│   ├── controller/      AuthController, MfaController, SessionController, RoleController, AuditLogController
│   ├── dto/request/     Inbound payloads with Bean Validation
│   ├── dto/response/    Outbound payloads
│   ├── entity/          JPA entities mapped 1:1 to the tables above
│   ├── exception/       Custom exceptions + GlobalExceptionHandler
│   ├── kafka/event/     Event payload POJOs
│   ├── kafka/producer/  AuthEventProducer
│   ├── mapper/          MapStruct interfaces (entity ↔ DTO)
│   ├── repository/      Spring Data JPA repositories
│   ├── security/        JWT provider/filter, UserDetails, OAuth2 success handler, entry points
│   ├── service/         Interfaces
│   ├── service/impl/    Implementations
│   ├── util/            Token generation, TOTP, request metadata helpers
│   └── validation/      @StrongPassword custom Bean Validation constraint
├── src/main/resources/
│   ├── application.yml, application-docker.yml
│   └── db/migration/V1__init_auth_schema.sql
├── src/test/java/...    JUnit5 + Mockito unit tests, MockMvc slice test
├── postman/              Postman collection
├── Dockerfile
└── docker-compose.yml    auth-db, redis, kafka, zookeeper, eureka, auth-service
```

---

## 5. REST API Reference

Base path: `/api/v1/auth`. Full interactive docs at `/swagger-ui.html` once running.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Register a new user |
| POST | `/login` | Public | Login; returns tokens or `mfaRequired` |
| POST | `/mfa/verify-login` | Public (session-scoped) | Complete login with TOTP code |
| POST | `/refresh-token` | Public (token-scoped) | Rotate access/refresh token pair |
| POST | `/logout` | Bearer | Revoke current or all sessions |
| POST | `/forgot-password` | Public | Request password reset email |
| POST | `/reset-password` | Public | Reset password with emailed token |
| POST | `/change-password` | Bearer | Change password (requires current password) |
| GET | `/verify-email?token=` | Public | Verify email address |
| POST | `/resend-verification` | Public | Resend verification email |
| GET | `/me` | Bearer | Current user profile |
| POST | `/mfa/setup` | Bearer | Begin TOTP enrollment (QR + secret + recovery codes) |
| POST | `/mfa/confirm` | Bearer | Confirm enrollment with a code |
| POST | `/mfa/disable` | Bearer | Disable MFA (requires password) |
| GET | `/sessions` | Bearer | List active devices/sessions |
| DELETE | `/sessions/{id}` | Bearer | Revoke one session |
| DELETE | `/sessions` | Bearer | Revoke all sessions |
| GET | `/roles` | ADMIN/COMPANY_ADMIN | List roles + permissions |
| POST | `/roles/{userId}/assign` | ADMIN/COMPANY_ADMIN | Assign roles |
| POST | `/roles/{userId}/revoke` | ADMIN/COMPANY_ADMIN | Revoke roles |
| GET | `/audit-logs/me` | Bearer | Paginated personal audit history |
| GET | `/oauth2/authorization/google` | Public | Kick off Google OAuth2 login |

### Example — Login (password only)
```http
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "rahul@email.com", "password": "Str0ng!Pass", "rememberMe": false }
```
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "mfaRequired": false,
    "tokens": { "accessToken": "eyJ...", "refreshToken": "8f2a...", "tokenType": "Bearer", "expiresInSeconds": 900 },
    "user": { "id": "...", "email": "rahul@email.com", "roles": ["ROLE_CANDIDATE"], ... }
  }
}
```

### Example — Login when MFA is enabled
```json
{ "success": true, "message": "MFA verification required",
  "data": { "mfaRequired": true, "mfaSessionToken": "9c1e...", "tokens": null, "user": null } }
```

### Example — Error shape (validation)
```json
{
  "timestamp": "2026-07-05T10:15:00Z", "status": 400, "error": "Bad Request",
  "errorCode": "VALIDATION_FAILED", "message": "One or more fields are invalid",
  "path": "/api/v1/auth/register",
  "fieldErrors": [ { "field": "password", "message": "Password must be at least 8 characters..." } ]
}
```

Postman collection: [`postman/Auth-Service.postman_collection.json`](postman/Auth-Service.postman_collection.json)

---

## 6. Security Architecture

- **Stateless JWT** (HS256, 15-min access token) carrying `sub` (userId), `email`, `roles`, `permissions`, `companyId`. Verified in-process by `JwtAuthenticationFilter` on every request — no shared session store needed for authorization decisions.
- **Refresh tokens** are opaque, DB-backed, rotated on every use, and hashed at rest — never JWTs themselves. This lets us revoke a specific device instantly, which a pure-JWT refresh token cannot do without a blacklist.
- **Logout revocation gap** is closed with a Redis blacklist (`auth:blacklist:{token}`, TTL = token's remaining life) so a logged-out access token stops working immediately instead of drifting until natural expiry.
- **RBAC + fine-grained permissions**: JWT carries both `ROLE_*` authorities and individual permission names (e.g. `JOB_CREATE`), so controllers can use either `hasRole('RECRUITER')` or `hasAuthority('JOB_CREATE')` depending on how granular the check needs to be.
- **Account lockout**: configurable max attempts (default 5) → timed lock (default 30 min), decoupled from any single login endpoint so the rule applies uniformly regardless of client.
- **MFA (TOTP, RFC 6238)**: password check and OTP challenge are two distinct steps; a password alone never issues a usable token when MFA is enabled. The intermediate `mfaSessionToken` lives in Redis with a 5-minute TTL.
- **Password reset / change** both revoke every existing refresh token for the user — closing the window for a stolen session token after a credential compromise.

---

## 7. Kafka Events (published by this service)

| Topic | Trigger | Consumed by (per platform architecture) |
|---|---|---|
| `auth.user.registered` | New registration | Notification Service (verification email), Analytics |
| `auth.user.logged-in` | Successful login | Analytics Service |
| `auth.password.reset-requested` | Forgot-password | Notification Service |
| `auth.password.changed` | Password changed/reset | Notification Service, Analytics |
| `auth.account.locked` | Lockout triggered | Notification Service, Security/Analytics |

All producers use `acks=all`, 3 retries, and idempotent producer settings; publish failures are logged but never block the auth flow itself (see `AuthEventProducer`).

---

## 8. Redis Usage

1. JWT blacklist on logout — `auth:blacklist:{token}` (TTL = remaining token life)
2. MFA session tokens between password check and OTP challenge — `auth:mfa-session:{token}` (TTL 5 min)
3. (Extension point) login rate limiting per IP/email

---

## 9. Testing Strategy

- **Unit tests** (JUnit5 + Mockito): `AuthServiceImplTest` (registration, lockout, MFA gate, successful login), `PasswordServiceImplTest` (forgot/reset flows, token expiry/reuse), `JwtTokenProviderTest` (signing, tampering, claims), `StrongPasswordValidatorTest` (parametrized policy checks).
- **Integration/slice test**: `AuthControllerTest` (MockMvc, validates request mapping, status codes, and the response envelope shape).
- **Not included here but recommended before production**: a `@SpringBootTest` + Testcontainers (Postgres + Redis + Kafka) suite exercising the full register → verify → login → refresh → logout lifecycle against real infrastructure.

Run tests: `mvn test`

---

## 10. Docker & Deployment

- `Dockerfile`: multi-stage Maven build → `eclipse-temurin:21-jre-alpine` runtime, non-root user, `/actuator/health` container healthcheck.
- `docker-compose.yml`: brings up `auth-db` (Postgres 16), `redis`, `kafka` + `zookeeper`, a Eureka server, and the service itself, wired together on one bridge network — enough to run the module standalone for local development.
- Kubernetes-ready: stateless pods (no local session state), externalized config via env vars / Config Server, `/actuator/health` and `/actuator/prometheus` wired for liveness/readiness probes and metrics scraping.

```bash
docker compose up --build
```

---

## 11. Future Enhancements

- WebAuthn/passkey support alongside TOTP MFA.
- Adaptive/risk-based authentication (step-up MFA on new device or geo anomaly), using the `user_sessions`/`audit_logs` history already captured.
- Rate limiting on `/login` and `/forgot-password` per IP+email using the Redis infrastructure already in place.
- SCIM or bulk-invite flows for company admins onboarding a recruiting team.
- Short-lived, service-to-service JWTs (client-credentials grant) for internal microservice calls, separate from the user-facing token type.
- Externalize the JWT signing key to AWS KMS / HashiCorp Vault instead of a config value.

---

## Local run (without Docker)

```bash
# Requires local Postgres (auth_db), Redis, and Kafka reachable at the hosts in application.yml
mvn spring-boot:run
# Swagger UI:      http://localhost:8081/swagger-ui.html
# Health:          http://localhost:8081/actuator/health
```
