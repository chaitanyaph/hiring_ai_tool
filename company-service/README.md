# Cadence Company Service

Part of the **Cadence AI Hiring Platform** microservices suite. Owns `company_db`
exclusively, per the platform's database-per-service strategy.

Company Service is responsible **only** for company management: company profile,
departments, offices, and the team-invitation lifecycle. It does **not** manage
authentication, passwords, JWT, login, sessions, job postings, candidates,
notifications, or billing -- those stay in their respective services.

---

## 1. Functional Requirements

| # | Requirement |
|---|---|
| FR1 | Company profile CRUD -- unique name/slug, validated website/email/phone, soft delete |
| FR2 | Departments per company -- CRUD, soft delete, name unique within company |
| FR3 | Offices per company -- CRUD, soft delete, exactly one primary office enforced |
| FR4 | Team invitations -- create/list/get/update/cancel/resend, secure token, configurable expiry, no duplicate pending invite per email+company |
| FR5 | Publish domain events to Kafka for every write (Company/Department/Office/Invitation lifecycle) |
| FR6 | Consume `UserCreated` and `InvitationAccepted` events to close the invitation loop |
| FR7 | Redis-cache company profile and department/office lists, evicted on write |
| FR8 | Paginated, filterable, sortable list endpoints |
| FR9 | Full OpenAPI documentation |

## 2. Architecture Decisions

- **UUID PKs** (`CHAR(36)`) -- safe across services, no sequence coordination.
- **Soft delete** (`is_deleted` + `deleted_at`) instead of hard `DELETE`, auto-filtered
  via Hibernate `@SQLRestriction` on every query.
- **Optimistic locking** (`@Version`) on every mutable entity.
- **Uniqueness + soft delete tension**: MySQL has no partial/filtered unique index.
  DB-level `UNIQUE` is only used where soft-delete never interacts with it
  (`company_name`, `company_slug`, `invite_token`). Department-name-within-company
  uniqueness is enforced at the **service layer** against non-deleted rows only.
- **"One primary office per company"** is a transactional service-layer invariant
  (unset the previous primary in the same transaction), not a DB constraint --
  MySQL cannot declaratively express "at most one TRUE per group" without triggers.
- **Kafka producer is `@Async`**: `KafkaTemplate.send()` blocks the calling thread
  while resolving broker metadata *before* returning a future, so wrapping the
  future alone does not make a publish non-blocking. Every `publishXxx` method on
  `CompanyEventProducer` is `@Async` so a Kafka outage never stalls a request.
- **Kafka consumer uses a `JsonMessageConverter`**, not a fixed `JsonDeserializer`
  type, since the same consumer group deserializes two different event shapes
  (`UserCreatedEvent`, `InvitationAcceptedEvent`) produced by a different service
  (Auth) that doesn't share this service's Java type headers.
- **Feign clients are interface-only stubs** (`NotificationServiceClient`,
  `AuthServiceClient`) -- nothing in this module calls them synchronously today;
  all cross-service triggers go through Kafka.
- **`createdBy`/`updatedBy`** are plain strings sourced from an `X-User-Id` request
  header, since there's no shared auth-context library between services yet.

## 3. Database Schema

```
companies (1) ──< departments (many)
companies (1) ──< offices (many)
companies (1) ──< team_invitations (many)
departments (1) ──< team_invitations (many, nullable FK)
```

Full DDL: [`src/main/resources/db/migration/V1__init_company_schema.sql`](src/main/resources/db/migration/V1__init_company_schema.sql).

| Table | Key columns |
|---|---|
| `companies` | id, company_name (unique), company_slug (unique), industry, website, company_email, company_phone, headquarters, description, company_logo, subscription_plan, status, is_deleted, audit fields, version |
| `departments` | id, company_id (FK), department_name, description, status, is_deleted, audit fields, version |
| `offices` | id, company_id (FK), office_name, country, state, city, address, postal_code, timezone, latitude, longitude, is_primary_office, status, is_deleted, audit fields, version |
| `team_invitations` | id, company_id (FK), department_id (FK, nullable), email, first_name, last_name, role, invite_token (unique), expiry_date, status, created_by, accepted_at, created_at, updated_at, version |

## 4. Folder Structure

```
company-service/
├── src/main/java/com/cadence/companyservice/
│   ├── client/          NotificationServiceClient, AuthServiceClient (Feign, interface-only)
│   ├── config/          RedisConfig, KafkaProducerConfig, KafkaConsumerConfig, SwaggerConfig
│   ├── constant/         TeamRole, InvitationStatus, KafkaTopics
│   ├── controller/      Company, Department, Office, TeamInvitation controllers
│   ├── dto/request/     Inbound payloads with Jakarta Validation
│   ├── dto/response/    Outbound payloads incl. PagedResponse<T>
│   ├── entity/          BaseAuditEntity + Company, Department, Office, TeamInvitation
│   ├── exception/       Custom exceptions + GlobalExceptionHandler
│   ├── kafka/event/     Event payload POJOs (produced + consumed)
│   ├── kafka/producer/  CompanyEventProducer (async, fire-and-forget)
│   ├── kafka/consumer/  CompanyEventConsumer (reconciles invitation acceptance)
│   ├── mapper/          MapStruct interfaces (entity <-> DTO)
│   ├── repository/      Spring Data JPA repositories
│   ├── service/         Interfaces
│   └── service/impl/    Implementations
├── src/main/resources/
│   ├── application.yml, application-docker.yml
│   └── db/migration/V1__init_company_schema.sql
├── src/test/java/...    JUnit5 + Mockito unit tests, MockMvc slice test, Testcontainers integration test
├── Dockerfile
└── docker-compose.yml   company-db, redis, kafka, zookeeper, eureka, company-service
```

## 5. REST API Reference

Full interactive docs at `/swagger-ui.html` once running.

### Companies (`/api/v1/companies`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/companies` | Create a company (called during COMPANY_ADMIN registration) |
| GET | `/api/v1/companies/{id}` | Get company details |
| PUT | `/api/v1/companies/{id}` | Update company details |
| DELETE | `/api/v1/companies/{id}` | Soft-delete a company |

### Departments

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/companies/{companyId}/departments` | Create a department |
| GET | `/api/v1/companies/{companyId}/departments` | List departments (`?page=&size=&sort=`) |
| GET | `/api/v1/departments/{id}` | Get a department |
| PUT | `/api/v1/departments/{id}` | Update a department |
| DELETE | `/api/v1/departments/{id}` | Soft-delete a department |

### Offices

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/companies/{companyId}/offices` | Create an office |
| GET | `/api/v1/companies/{companyId}/offices` | List offices (`?page=&size=&sort=`) |
| GET | `/api/v1/offices/{id}` | Get an office |
| PUT | `/api/v1/offices/{id}` | Update an office |
| DELETE | `/api/v1/offices/{id}` | Soft-delete an office |

### Team Invitations

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/companies/{companyId}/team-invitations` | Invite a teammate |
| GET | `/api/v1/companies/{companyId}/team-invitations` | List invitations (`?page=&size=&sort=`) |
| GET | `/api/v1/team-invitations/{id}` | Get an invitation |
| PUT | `/api/v1/team-invitations/{id}` | Update role/department of a pending invitation |
| DELETE | `/api/v1/team-invitations/{id}` | Cancel a pending invitation |
| POST | `/api/v1/team-invitations/{token}/resend` | Resend (issues a fresh token + expiry) |

### Example -- Create a company
```http
POST /api/v1/companies
Content-Type: application/json
X-User-Id: ananya@acmecorp.com

{ "companyName": "Acme Corp", "industry": "Software", "headquarters": "Pune, Maharashtra" }
```
```json
{
  "success": true,
  "message": "Company created",
  "data": {
    "id": "b3f1...", "companyName": "Acme Corp", "companySlug": "acme-corp",
    "subscriptionPlan": "FREE", "status": "ACTIVE", "version": 0
  }
}
```

### Example -- Validation error
```json
{
  "timestamp": "2026-07-06T00:00:00Z", "status": 400, "error": "Bad Request",
  "errorCode": "VALIDATION_FAILED", "message": "One or more fields are invalid",
  "path": "/api/v1/companies",
  "fieldErrors": [ { "field": "companyName", "message": "Company name is required" } ]
}
```

## 6. Kafka Events

### Published

| Topic | Trigger |
|---|---|
| `company.company.created` / `.updated` | Company create/update |
| `company.department.created` / `.updated` / `.deleted` | Department writes |
| `company.office.created` / `.updated` / `.deleted` | Office writes |
| `company.team-invitation.created` | New invite (consumed by Notification Service to send the email) |
| `company.team-invitation.cancelled` | Invite cancelled |

### Consumed

| Topic | Effect |
|---|---|
| `auth.user.created` | Marks the matching PENDING invitation (by company+email) as ACCEPTED |
| `auth.invitation.accepted` | Marks the matching PENDING invitation (by token) as ACCEPTED |

All producers use `acks=all`, 3 retries, idempotent producer settings, and publish
asynchronously (`@Async`) so a Kafka outage never blocks a request.

## 7. Redis Cache

| Cache | Evicted on |
|---|---|
| `companyProfile` (key: companyId) | Company update/delete |
| `departmentList` (key: companyId) | Any department create/update/delete for that company |
| `officeList` (key: companyId) | Any office create/update/delete for that company |

## 8. Testing Strategy

- **Unit tests** (JUnit5 + Mockito): `CompanyServiceImplTest`, `DepartmentServiceImplTest`,
  `OfficeServiceImplTest` (primary-office enforcement), `TeamInvitationServiceImplTest`
  (token generation, duplicate-pending guard, resend-revives-expired, accept idempotency).
- **Controller slice test** (MockMvc): `CompanyControllerTest`.
- **Integration test** (Testcontainers + real MySQL 8): `CompanyIntegrationTest` --
  exercises Flyway migrations, unique constraints, soft-delete filtering, and FK
  cascades against an actual database rather than H2, since those DB-specific
  behaviors are exactly what differ between engines.

Run tests: `mvn test`

## 9. Docker & Deployment

```bash
docker compose up --build
```

Brings up `company-db` (MySQL 8), `redis`, `kafka` + `zookeeper`, a Eureka server,
and the service itself. Ports are offset from auth-service's (`3307`, `6380`,
`9093`, `8762`, `8083`) so both services' compose stacks can run side by side.

## 10. Local Run (without Docker)

```bash
# Requires local MySQL, Redis, and Kafka reachable at the hosts in application.yml
mvn spring-boot:run
# Swagger UI: http://localhost:8083/swagger-ui.html
# Health:     http://localhost:8083/actuator/health
```

## 11. Senior Architect Review -- Improvement Areas

**Scalability**
- The department/office uniqueness checks (`exists...` then `save`) are two
  round-trips with a race window under concurrent requests; the DB-level unique
  constraint on `companies` closes this for company name/slug, but department
  names rely purely on the service-layer check. Under real concurrent load,
  add an application-level distributed lock (Redis) or accept-and-retry-on-
  constraint-violation pattern for departments too.
- Pagination is offset-based (`Pageable`); for very large invitation/department
  lists at scale, consider keyset (cursor) pagination to avoid `OFFSET` cost.

**Security**
- `createdBy`/`updatedBy`/`X-User-Id` is currently a trusted, unauthenticated
  header -- fine behind a Gateway that has already validated a JWT and injects
  this header itself, but this service must never be reachable directly from
  the public internet once a Gateway exists.
- Invite tokens are UUIDs (122 bits of randomness) -- adequate, but consider
  hashing them at rest (same pattern as auth-service's refresh/reset tokens)
  so a DB leak alone can't be replayed, rather than storing the raw token.

**Maintainability**
- The Feign clients are unused stubs today; once Notification/Auth Service
  contracts stabilize, replace the interface-only shape with real DTOs instead
  of `Map<String, Object>`.
- `TeamInvitation` intentionally doesn't extend `BaseAuditEntity` (its own
  status lifecycle already models "deleted" as CANCELLED) -- keep this
  distinction in mind before any future refactor tries to unify all entities
  under one base class.
