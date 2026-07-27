# Cadence Job Service

Part of the **Cadence AI Hiring Platform** microservices suite. Owns `job_db`
exclusively. Manages the complete lifecycle of a job posting -- and only that.
Authentication, recruiters-as-people, candidates, resumes, interviews,
notifications and billing all belong to other services.

---

## 1. Functional Requirements

| # | Requirement |
|---|---|
| FR1 | Full job lifecycle: Draft -> Published -> Paused/Closed -> Archived, plus Restore and system-driven Expired |
| FR2 | 4-step creation wizard, each step independently saveable; full validation only enforced at Publish |
| FR3 | Per-job customizable hiring pipeline, seeded from platform defaults (11 stages), reorderable/renameable/toggle-able |
| FR4 | Reusable job templates ("Templates" button on the Jobs screen) |
| FR5 | Company-scoped search/filter/sort/paginate across title, department, location, status, recruiter, hiring manager |
| FR6 | Dashboard aggregates: total/published/draft/archived, recently created, closing soon |
| FR7 | Role-gated access -- 5 roles can write, Hiring Manager is view-only unless granted `JOB_EDIT`, Interviewers/Candidates excluded entirely |
| FR8 | Strict company isolation -- every query scoped to the JWT's `companyId`, never a client-supplied one |
| FR9 | Publishes 7 Kafka lifecycle events |
| FR10 | Full audit trail: every status transition (`job_status_history`) and every mutation (`job_audit`) |

## 2. UI -> Backend Mapping (from the attached Figma)

| Figma element | Backend |
|---|---|
| Wizard Step 1: title, department, openings, location, work type, employment type, description, "Generate draft with AI" | `jobs` + `job_description`; AI button is an explicit future placeholder, no LLM call wired in |
| Wizard Step 2 (Requirements) | `job_requirements` + `job_skills` (REQUIRED/PREFERRED) + `job_benefits` |
| Wizard Step 3 (Hiring stages) | `job_pipeline_stage`, full-replace semantics per save (add/delete/reorder/rename/enable-disable) |
| Wizard Step 4 (Review & Publish) | Aggregate GET + the Publish state transition, which is the actual validation gate |
| "18 open positions across 4 departments" | Dashboard aggregate: published count + distinct department count |
| Filter tabs (All/Published/Draft/Archived + counts) | `/jobs/counts` |
| Table row action set changing per status (Published: Edit/Archive; Draft: Edit/Publish; Archived: Restore) | The `JobStatus` state machine -- valid actions are a function of current status, enforced server-side |
| "Templates" button | `job_template` -- save/list/create-draft-from-template |

## 3. Business Workflow

```
Create (any step saveable as Draft) -> Publish (full-validation gate)
  -> status=PUBLISHED, JobPublished event
  -> [Pause <-> Published] / [Close] / [Archive] / [Restore -> Draft]
  -> deadline passes with no manual close -> EXPIRED (hourly scheduled sweep)
```

State machine: `DRAFT -> PUBLISHED`; `PUBLISHED <-> PAUSED`; `PUBLISHED/PAUSED -> CLOSED`;
`PUBLISHED/PAUSED/CLOSED/EXPIRED -> ARCHIVED`; `ARCHIVED -> DRAFT` (Restore);
`PUBLISHED/PAUSED -> EXPIRED` (system). Anything else returns 409.

## 4. Database Schema

```
jobs (1) ──1:1── job_description
jobs (1) ──1:1── job_requirements
jobs (1) ──< job_skills (REQUIRED/PREFERRED)
jobs (1) ──< job_benefits
jobs (1) ──< job_pipeline_stage (ordered)
jobs (1) ──< job_assignment (unique per job+role -- recruiter, hiring manager)
jobs (1) ──< job_status_history (append-only)
jobs (1) ──< job_audit (append-only)
companies (external) ──< job_template (per-company reusable snapshots, stored as JSON)
```

Full DDL: [`src/main/resources/db/migration/V1__init_job_schema.sql`](src/main/resources/db/migration/V1__init_job_schema.sql).

`jobs.recruiter_id` / `jobs.hiring_manager_id` are **denormalized** from `job_assignment`
for fast search/filter/display without a join on every listing query --
`job_assignment` stays the authoritative record and audit trail of who's assigned and when.

## 5. Architecture Decisions

- **UUID PKs**, **soft delete** (`is_deleted`/`deleted_at`, auto-filtered via `@SQLRestriction`), **optimistic locking** (`@Version`) -- same pattern as Auth/Company Service.
- **Security**: Job Service validates JWTs issued by Auth Service using the **same shared HS256 secret** -- it never issues tokens itself, only verifies them. Roles arrive as either `ROLE_X` or bare `X`; normalized in `JwtAuthenticationFilter` since that prefix convention isn't guaranteed stable across services.
- **Company scoping is never client-supplied.** Every service method takes the authenticated `CurrentUser` and scopes to `currentUser.getCompanyId()` -- a job in another company returns the *same* 404 a truly-missing job would, never a 403, so existence can't be probed.
- **Hiring Manager view-only, unless granted `JOB_EDIT`**: enforced in the service layer (`requireWriteAccess`), not `@PreAuthorize` alone, since it depends on a per-user permission claim on top of the role.
- **Search/filter uses JPA Specifications**, not derived query methods -- 8 independently-combinable optional filters would otherwise mean an explosion of method names.
- **Kafka producer is `@Async`** -- same lesson as Auth/Company Service: `KafkaTemplate.send()` blocks resolving broker metadata *before* returning a future, so the method itself must run off the request thread.
- **Job templates store a JSON snapshot**, not a parallel normalized schema -- a template is just data to pre-fill a new draft with, never queried on its own fields.
- **Deadline expiry is a scheduled sweep** (hourly), not a request-time check -- nothing in the UI triggers the Published/Paused -> Expired transition, so it has to run independently of any API call.

## 6. Folder Structure

```
job-service/
├── src/main/java/com/cadence/jobservice/
│   ├── client/          CompanyServiceClient (department lookup), AuthServiceClient (stub)
│   ├── config/          SecurityConfig, RedisConfig, KafkaProducerConfig, SwaggerConfig
│   ├── constant/        JobStatus (state machine), WorkType, EmploymentType, SkillType,
│   │                    PlatformRole, PipelineStageDefaults, KafkaTopics, SecurityConstants
│   ├── controller/      JobController, JobDashboardController, JobTemplateController
│   ├── dto/request/     Wizard-step request DTOs, search criteria
│   ├── dto/response/    JobSummaryResponse (listing row), JobDetailResponse (aggregate), PagedResponse<T>
│   ├── entity/          BaseAuditEntity + Job + 9 related tables
│   ├── exception/       Custom exceptions + GlobalExceptionHandler
│   ├── kafka/event/     7 published event POJOs
│   ├── kafka/producer/  JobEventProducer (async, fire-and-forget)
│   ├── mapper/          MapStruct interfaces
│   ├── repository/      Spring Data JPA + JobSpecifications for dynamic search
│   ├── security/        JwtTokenValidator, JwtAuthenticationFilter, CurrentUser, CurrentUserProvider
│   └── service/impl/    JobServiceImpl, JobTemplateServiceImpl, JobExpiryScheduler
├── src/main/resources/
│   ├── application.yml, application-docker.yml
│   └── db/migration/V1__init_job_schema.sql
├── src/test/java/...    JUnit5 + Mockito unit tests
├── Dockerfile
└── docker-compose.yml
```

## 7. REST API Reference

Full interactive docs at `/swagger-ui.html` (Bearer JWT auth wired into Swagger UI).

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/jobs` | Create draft (wizard Step 1) |
| PUT | `/api/v1/jobs/{id}/basic-info` | Update basic info / autosave |
| PUT | `/api/v1/jobs/{id}/requirements` | Update requirements (Step 2) |
| PUT | `/api/v1/jobs/{id}/pipeline-stages` | Update hiring stages (Step 3), full replace |
| GET | `/api/v1/jobs/{id}` | Full job detail (Step 4 review, or general view) |
| POST | `/api/v1/jobs/{id}/publish` | Publish (validates readiness) |
| POST | `/api/v1/jobs/{id}/pause` \| `/resume` \| `/close` \| `/archive` \| `/restore` | Lifecycle transitions |
| DELETE | `/api/v1/jobs/{id}` | Delete (DRAFT only) |
| POST | `/api/v1/jobs/{id}/duplicate` | Duplicate/clone into a new draft |
| PUT | `/api/v1/jobs/{id}/assignment` | Assign recruiter/hiring manager |
| GET | `/api/v1/jobs?title=&departmentId=&location=&status=&employmentType=&recruiterId=&hiringManagerId=&createdFrom=&createdTo=&page=&size=&sort=` | Search/filter/sort/paginate |
| GET | `/api/v1/jobs/counts` | Listing header + filter-tab counts |
| GET | `/api/v1/jobs/dashboard` | Dashboard aggregates |
| POST | `/api/v1/job-templates/from-job/{jobId}` | Save a job as a template |
| GET | `/api/v1/job-templates` | List templates |
| POST | `/api/v1/job-templates/{id}/create-draft` | Start a new draft from a template |
| DELETE | `/api/v1/job-templates/{id}` | Delete a template |

### Example -- Create draft
```http
POST /api/v1/jobs
Authorization: Bearer <token from Auth Service>
Content-Type: application/json

{ "title": "Backend Engineer", "departmentId": "...", "numberOfOpenings": 3, "location": "Pune, India", "workType": "HYBRID", "employmentType": "FULL_TIME" }
```

### Example -- Publish rejected (validation)
```json
{
  "timestamp": "2026-07-06T00:00:00Z", "status": 400, "error": "Bad Request",
  "errorCode": "JOB_NOT_READY_TO_PUBLISH",
  "message": "Cannot publish -- missing: recruiter assignment, at least one enabled hiring stage",
  "path": "/api/v1/jobs/.../publish"
}
```

## 8. Kafka Events (published)

| Topic | Trigger |
|---|---|
| `job.job.created` | Draft created (including duplicate/from-template) |
| `job.job.updated` | Basic info, requirements, pipeline, assignment, or pause/resume changes |
| `job.job.published` | Publish transition |
| `job.job.closed` | Close transition |
| `job.job.archived` | Archive transition |
| `job.job.restored` | Restore transition |
| `job.job.deleted` | Draft deleted |

Async, `acks=all`, 3 retries, idempotent producer -- a Kafka outage never blocks a job write.

## 9. Redis Cache

| Cache | Evicted on |
|---|---|
| `jobDetail` (key: jobId) | Any lifecycle transition on that job |
| `jobCounts` (key: companyId, 5 min TTL) | Not explicitly evicted -- short TTL is deliberate since counts change frequently and don't need to be instantly consistent |

## 10. Testing Strategy

- **Unit tests** (JUnit5 + Mockito): `JobServiceImplTest` -- default pipeline seeding, publish-readiness validation (recruiter required, deadline not in the past, at least one enabled stage), invalid status transitions, company-isolation (cross-company access returns 404, not 403), Hiring-Manager-view-only enforcement (denied without `JOB_EDIT`, allowed with it), delete-only-if-Draft.
- Testcontainers/MySQL dependency is wired into `pom.xml` (matching Company Service's pattern) for a future full-stack integration test once your local Docker/disk-space situation allows running it.

Run tests: `mvn test`

## 11. Docker & Deployment

```bash
docker compose up --build
```

Ports are offset from auth-service (8081) and company-service (8083): job-service
runs on **8084**, with its own MySQL (`3308`), Redis (`6381`), Kafka (`9094`), and
Eureka (`8763`) so all three services' compose stacks can run side by side.

## 12. Local Run (without Docker)

```bash
# Requires local MySQL, Redis, and Kafka reachable at the hosts in application.yml
mvn spring-boot:run
# Swagger UI: http://localhost:8084/swagger-ui.html
```

**Getting a JWT to test with**: log in via auth-service (`POST /api/v1/auth/login`),
then pass its `accessToken` as `Authorization: Bearer <token>` here -- both services
share the same `app.jwt.secret`, so a token Auth Service issued validates here
without any network call between them.

## 13. Senior Architect Review -- Improvement Areas

**Scalability**
- `resolveDepartmentName` calls Company Service via Feign once per distinct
  department in a page of search results -- fine at current scale, but at high
  QPS this should move to a short-TTL local cache (or Redis) keyed by
  departmentId, since department names change far less often than job listings
  are viewed.
- The hourly expiry sweep does a full table scan per company implicitly via
  `findAllByStatusInAndApplicationDeadlineBefore` (unscoped by company, global) --
  fine at today's data volume; add a composite index or partition by company
  if the jobs table grows into the millions of rows.

**Security**
- `JwtAuthenticationFilter` currently accepts a token with **any** normalized
  role string as long as it parses -- role membership in `PlatformRole.ALLOWED_ROLES`
  is only checked by `@PreAuthorize` on the controller, not the filter itself.
  That's intentional (keeps the filter dumb and the policy in one place), but
  worth remembering if a new controller is added without the same annotation.
- The shared JWT secret being identical across services is simple and correct
  for HS256, but doesn't scale past a handful of services without key
  rotation coordination -- moving to RS256 with Auth Service holding the
  private key and every other service holding only the public key removes
  that coordination burden.

**Maintainability**
- `AuthServiceClient.getUserById` is an unused stub (Auth Service doesn't
  expose that endpoint yet) -- wire it in once it exists, to show recruiter/
  hiring manager names instead of bare UUIDs on the job detail view.
- The Testcontainers integration test scaffolding is present (`pom.xml`, BOM)
  but no test class was added this session due to a local Docker
  availability issue -- writing one mirroring Company Service's
  `CompanyIntegrationTest` (Flyway + unique constraints + soft-delete
  filtering against real MySQL) is the natural next step.
