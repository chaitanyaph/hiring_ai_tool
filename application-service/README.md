# Cadence Application Service

The heart of the hiring workflow. Owns the **Application Lifecycle** end-to-end: current status, current stage, the full audit trail of every transition, every AI/human score received, recruiter notes, and the recruiter/candidate-facing APIs that read and drive all of it. It does **not** own authentication, candidate profiles, jobs, companies, resume parsing, AI matching, AI interviews, notifications, coding assessments, or offer generation -- those are separate services this one orchestrates around, never duplicates.

## 1. Functional Requirements

- Candidate applies to a job; validated against Job Service (PUBLISHED, deadline) and Candidate Service (profile complete, resume uploaded) before an Application row is ever created.
- Every one of the 20 lifecycle statuses and 10 pipeline stages is tracked with full history (`application_status_history`, `application_stage_history`) -- nothing overwrites silently.
- AI/human scores (resume match, AI interview, coding, overall) are recorded both as the latest value on the row (fast reads) and as an append-only log (`application_scores`, full audit).
- Recruiters can search/filter/sort/paginate their company's applications, assign a recruiter and/or hiring manager, change status, add internal notes, and view the timeline/history.
- Candidates can apply, withdraw, track status, view the timeline, and accept/decline a released offer.
- 7 Kafka events published for other services to react to; 7 Kafka events consumed from other (future) services, each driving a real, validated status transition -- see Business Workflow below.
- 4 internal REST endpoints let other services report a score synchronously instead of via Kafka.

## 2. Non-Functional Requirements

- **Consistency**: every status/stage change is guarded by `ApplicationStatus.canTransitionTo()` -- an illegal transition (e.g. skipping straight to HIRED) is rejected with 409, never silently coerced.
- **Resilience**: Kafka consumers never crash on a malformed or out-of-order event -- they log a warning and no-op if the application is missing or in an unexpected status, since redelivery/duplication is a normal fact of life for an event-driven system.
- **Auditability**: `application_status_history` + `application_stage_history` + `application_scores` + `application_events` together mean nothing about an application's journey is ever lost, even superseded values.
- **Isolation**: `application_db` shares nothing with any other service's database -- `company_id`/`job_id`/`candidate_id`/`resume_id`/`assigned_recruiter_id`/`assigned_hiring_manager_id` are plain ids, validated via Feign at the moment they matter (apply-time), never joined.
- **Security**: every recruiter query is scoped to the caller's own `companyId` from the JWT; a candidate query is scoped to their own `userId`. A cross-company lookup returns the same 404 a non-existent application would.

## 3. Database Design

| Table | Purpose |
|---|---|
| `applications` | The aggregate root -- current status/stage/scores/assignments + a candidate/job display snapshot |
| `application_status_history` | Append-only: every status transition, who triggered it, why |
| `application_stage_history` | Append-only: every stage transition (derived from status, recorded separately for the pipeline board) |
| `application_scores` | Append-only: every score ever reported, by type and source |
| `application_notes` | Internal recruiter-only notes, soft-deletable |
| `application_events` | Raw JSON audit trail of every Kafka event *consumed* from another service |

## 4. ER Diagram

See the rendered diagram above. Summary: `applications` is the aggregate root with a 1:many relationship (via `application_id`) to all 5 child tables. `company_id`, `job_id`, `candidate_id`, `resume_id` and `assigned_recruiter_id`/`assigned_hiring_manager_id` are **id-only references** into Company/Job/Candidate/Auth Service's own databases -- no foreign key ever crosses that boundary, per the platform's database-per-service rule.

## 5. Folder Structure

```
src/main/java/com/cadence/applicationservice/
  constant/       ApplicationStatus (state machine), ApplicationStage, Priority, ScoreType,
                  InterviewType, KafkaTopics, PlatformRole, SecurityConstants
  entity/         BaseAuditEntity + 6 JPA entities
  repository/     Spring Data repos + ApplicationSpecifications (dynamic search)
  security/       JWT validation, CurrentUser, JwtAuthenticationFilter
  exception/      ErrorCode, ApplicationServiceException hierarchy, GlobalExceptionHandler
  dto/            request/ + response/
  mapper/         ApplicationMapper (MapStruct)
  client/         JobServiceClient, CandidateServiceClient, CompanyServiceClient, AuthServiceClient (stub)
  kafka/event/    7 published + 7 consumed event POJOs
  kafka/producer/ ApplicationEventProducer (@Async)
  kafka/consumer/ ApplicationEventConsumer
  config/         SecurityConfig, KafkaProducerConfig, KafkaConsumerConfig, RedisConfig, SwaggerConfig
  service/        ApplicationService, ApplicationLifecycleEventService, InternalScoreService (+ impl/)
  controller/     ApplicationController, InternalApplicationController
```

## 6. Business Workflow -- How Every Status Is Actually Reached

The product spec's workflow narrative implies several transitions happen "automatically" back-to-back (e.g. resume parsed -> matching starts immediately). Rather than inventing extra enum values not in the spec's 20-status list, this service records **two real, audited transitions** for each such automatic hop:

| Trigger | Transition(s) recorded |
|---|---|
| `apply()` | `APPLIED` -> `RESUME_PARSING` (parsing starts the instant `ApplicationCreated` is published) |
| Kafka `ResumeParsed` | `RESUME_PARSING` -> `RESUME_PARSED` -> `AI_MATCHING` |
| Kafka `ResumeMatched` | `AI_MATCHING` -> `AI_MATCHED` (sets `resumeMatchScore`) |
| Kafka `CandidateShortlisted` | `AI_MATCHED` -> `SHORTLISTED` -> `AI_INTERVIEW_PENDING` |
| Kafka `InterviewCompleted` (AI) | `AI_INTERVIEW_PENDING` -> `AI_INTERVIEW_COMPLETED` -> `CODING_ASSESSMENT_PENDING` (sets `aiInterviewScore`) |
| Kafka `CodingAssessmentCompleted` | `CODING_ASSESSMENT_PENDING` -> `CODING_ASSESSMENT_COMPLETED` -> `TECHNICAL_INTERVIEW` (sets `codingScore`) |
| Kafka `InterviewCompleted` (TECHNICAL/MANAGER/HR) | `TECHNICAL_INTERVIEW` -> `MANAGER_INTERVIEW` -> `HR_INTERVIEW` -> `BACKGROUND_VERIFICATION` |
| Kafka `BackgroundVerificationCompleted` (passed) | no status change -- stays at `BACKGROUND_VERIFICATION`, awaiting the Offer Service |
| Kafka `BackgroundVerificationCompleted` (failed) | -> `REJECTED` |
| Kafka `OfferReleased` | `BACKGROUND_VERIFICATION` -> `OFFER_RELEASED` |
| Candidate accepts offer | `OFFER_RELEASED` -> `OFFER_ACCEPTED` -> `HIRED` |
| Candidate declines offer | `OFFER_RELEASED` -> `OFFER_DECLINED` |
| Recruiter rejects (any active stage) | -> `REJECTED` |
| Candidate withdraws (any non-terminal stage) | -> `WITHDRAWN` |

`overallScore` is a real derived average of whichever of resumeMatchScore/aiInterviewScore/codingScore have been reported so far, recomputed on every update -- never fabricated. The `/internal/.../overall-score` endpoint exists for a future service that computes its own weighted score and wants to override that default.

## 7. Architecture Decisions

- **JWT validation only, no issuance** -- same shared HS256 secret as every other service. `CurrentUser` carries `userId`, `email`, `companyId` (null for candidate tokens), `role`, `permissions`.
- **Hiring Manager view-only unless granted `APPLICATION_EDIT`** -- mirrors Job Service's exact rule: a Hiring Manager can see the full pipeline but can't change status/assignments without the permission claim.
- **Candidate/company name and job title are snapshotted onto the Application row at apply() time** via Feign, so recruiter-side search (by candidate name/email/job title) doesn't require a live join back to Job/Candidate Service -- and the application stays fully readable even if those services are later unreachable or the job is deleted upstream.
- **Kafka consumers are defensive by design** -- every handler checks the application is in the exact expected status before transitioning; anything else is logged and skipped, not thrown, since Kafka delivery is at-least-once.
- **Two service interfaces, one implementation** -- `ApplicationService` (human-driven: apply/withdraw/assign/status-change) and `ApplicationLifecycleEventService` (system-driven: Kafka event handlers) are separate interfaces so the two trigger sources are never confused in calling code, even though one impl class shares their `transitionStatus`/history-recording helpers.
- **Redis is wired for eviction, not yet for reads** -- caching `getForRecruiter`/`getForCandidate` was deliberately skipped because the two return different shapes (recruiter sees notes, candidate doesn't); caching both under one key risked leaking recruiter notes to a candidate. The cache infrastructure (`RedisConfig`, `evictApplicationCache`) is in place for a future dashboard-aggregation cache that doesn't have this asymmetry.
- **A documented filter gap**: Department/Location/Experience filters from the product spec aren't implemented, since those are Job/Candidate Service concepts this service doesn't join across. Closing this would mean either a Job Service pre-filter returning a jobId list, or adding more snapshot columns if it becomes a real product need -- see `ApplicationSpecifications`.

## 8. REST API Reference

All endpoints require `Authorization: Bearer <JWT>` except `/internal/**` (trusted network, same trust model as Company Service).

### Candidate-facing
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/applications` | Apply to a job |
| GET | `/api/v1/applications/my` | My applications |
| GET | `/api/v1/applications/{id}` | Detail (shared with recruiters) |
| DELETE | `/api/v1/applications/{id}` | Withdraw |
| POST | `/api/v1/applications/{id}/accept-offer` | Accept a released offer -> HIRED |
| POST | `/api/v1/applications/{id}/reject-offer` | Decline a released offer |

### Recruiter-facing
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/applications/company/{companyId}` | Search/filter/sort/paginate |
| GET | `/api/v1/applications/job/{jobId}` | Applications for one job |
| PUT | `/api/v1/applications/{id}/status` | Change status (state-machine validated) |
| PUT | `/api/v1/applications/{id}/assign-recruiter` | |
| PUT | `/api/v1/applications/{id}/assign-hiring-manager` | |
| POST | `/api/v1/applications/{id}/notes` | Internal note |

### Shared (candidate owns it, or a recruiter from the same company)
| Method | Path |
|---|---|
| GET | `/api/v1/applications/{id}/timeline` |
| GET | `/api/v1/applications/{id}/history` |

### Internal (trusted network only)
| Method | Path |
|---|---|
| PUT | `/internal/application/{id}/resume-score` |
| PUT | `/internal/application/{id}/interview-score` |
| PUT | `/internal/application/{id}/coding-score` |
| PUT | `/internal/application/{id}/overall-score` |

## 9. Kafka Events

**Published**: `application.application.created`, `application.application.withdrawn`, `application.status.changed`, `application.recruiter.assigned`, `application.hiring-manager.assigned`, `application.offer.accepted`, `application.offer.rejected`.

**Consumed** (owned by other, future services): `resume.resume.parsed`, `matching.resume.matched`, `shortlisting.candidate.shortlisted`, `interview.interview.completed`, `assessment.coding.completed`, `verification.background.completed`, `offer.offer.released`.

## 10. Testing

20 unit tests (JUnit 5 + Mockito + AssertJ): apply-time validation guards (duplicate, job not published, deadline passed, resume missing, profile incomplete), the auto-advance-to-RESUME_PARSING on apply, withdraw's terminal-state guard, accept-offer's straight-to-HIRED path, the Hiring-Manager permission gate on status changes, invalid-transition rejection, cross-company 404s, and 3 of the 7 Kafka event handlers (resume parsed, AI interview completed, background verification failed) plus all 4 internal score endpoints. All passing.

A Testcontainers MySQL integration test is the natural next step, same documented gap as Company/Job/Candidate Service (no reliably reachable local Docker daemon during this build).

## 11. Docker & Local Run

```
docker compose up --build
```

Spins up application-db (3310), redis (6383), kafka (9096), eureka (8765), application-service (8086).

Without Docker: local MySQL/Redis/Kafka reachable per `application.yml`, then `mvn spring-boot:run`. Swagger UI at `http://localhost:8086/swagger-ui.html`.

## 12. Senior Architect Review -- Improvement Areas

- **Scalability**: `applications` is indexed on every column `ApplicationSpecifications` filters by (company/job/candidate/status/stage/recruiter/hiring-manager/priority) -- search stays index-backed as volume grows. The append-only history/score/event tables grow unbounded by design; a retention/archival job is the natural next addition once volume warrants it.
- **Security**: recruiter-side write actions require both a role check *and*, for Hiring Manager, an explicit permission claim -- least-privilege by default. Every lookup is scoped server-side to the JWT's own ids, never a client-supplied company/candidate id.
- **Maintainability**: the state machine lives in one place (`ApplicationStatus.canTransitionTo`) and both the human-driven and event-driven code paths call the same `transitionStatus()` helper -- there is exactly one way to move an application forward, which is what makes the audit trail trustworthy.
- **Next real gap**: this service assumes Resume Parsing/Matching/Shortlisting/AI-Interview/Coding-Assessment/Background-Verification/Offer services will eventually exist and publish the 7 consumed topics -- until they do, an application will sit at `RESUME_PARSING` forever in a real deployment. The internal score-update REST endpoints are the interim manual/synchronous path for exercising the rest of the lifecycle before those services are built.
