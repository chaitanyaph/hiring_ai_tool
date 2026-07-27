# Cadence Candidate Service

Owns the candidate's self-service world: profile (10-step wizard), resume storage metadata, job applications and saved jobs. It does **not** own authentication (Auth Service), job postings (Job Service), companies (Company Service), or any AI scoring/interviewing (future AI services) -- those are referenced here only by id, or captured as a point-in-time snapshot.

## 1. Functional Requirements

- Candidate profile wizard: Basic Info, Resume, Education, Experience, Skills, Projects, Certifications, Languages, Job Preferences, Portfolio -- each step independently saveable, full-replace semantics for list-based steps (add/edit/delete/reorder in one call).
- Profile completion percentage, recomputed after every wizard-step write from 10 real signals -- never hardcoded.
- Apply to a job (validated against Job Service: must be PUBLISHED, one application per candidate per job), withdraw an application, and track its 9-stage pipeline (Applied -> Resume Screening -> AI Resume Match -> AI Interview -> Coding Assessment -> Technical Interview -> HR Interview -> Offer, with Rejected/Withdrawn as terminal side-branches).
- Save/unsave jobs for later.
- A single dashboard endpoint aggregating profile completion, AI resume score, application/saved-job counts and the 5 most recent of each -- backs the candidate home screen.
- A stage-change endpoint for recruiting-side roles (not candidates) to move an application forward, company-scoped so one company can never touch another's applications.

## 2. Why It's Not Generic CRUD

- The 10 wizard steps are backed by 10 independent update methods, not one big "update candidate" endpoint -- matching how the actual UI autosaves per-step and lets a candidate leave the wizard mid-way without losing earlier steps.
- Application status is a real state machine (`ApplicationStatus.canTransitionTo`), not a free-text field -- a candidate can never see "Offer" before "HR Interview" happened, and a REJECTED/WITHDRAWN application can never be reopened.
- Job/company data is **snapshotted** onto the Application row at apply-time (title, location, employment type, company name) via Feign, so an application stays fully readable even if the job is later archived or Job/Company Service is briefly unreachable -- this mirrors Job Service's own denormalization of recruiter/hiring-manager onto the job row for the same reason.
- The stage-change endpoint returns the identical 404 whether the application doesn't exist or belongs to a different company -- never a 403 -- so cross-company existence is never leaked (same pattern Job Service uses for cross-company job access).

## 3. Database Schema (candidate_db, MySQL 8)

| Table | Purpose |
|---|---|
| `candidates` | 1:1 with an Auth Service userId (id is **not** auto-generated -- it *is* the userId) |
| `candidate_education` / `candidate_experience` / `candidate_projects` / `candidate_certifications` | Full audit + soft-delete, ordered lists (wizard steps 3/4/6/7) |
| `candidate_skills` / `candidate_languages` | Simple tag tables, full-replace on every write (wizard steps 5/8) |
| `candidate_job_preferences` / `candidate_portfolio_links` | 1:1 rows, wizard steps 9/10 |
| `saved_jobs` | Bookmarks, unique per (candidate, job) |
| `applications` | One per (candidate, job), carries the job/company snapshot + current `status` |
| `application_status_history` | Append-only trail of every stage transition |

## 4. Architecture Decisions

- **JWT validation only, no issuance** -- same shared HS256 secret as Auth/Company/Job Service. `CurrentUser` carries `userId`, `email`, `role`, and an optional `companyId` (present only for recruiting-side tokens, used solely to scope the stage-change endpoint).
- **Lazy profile creation** -- there is no Kafka consumer creating a blank Candidate row on registration. The wizard's Basic Info step (Step 1) doubles as "create if absent, else update" -- avoids a speculative cross-service coupling to an Auth Service event shape that could drift, and matches the natural UX (Step 1 is always first).
- **Resume storage** -- stored on local disk under `app.candidate.resume-storage-path` for this iteration (a real, working implementation, not a stub) with the metadata (`resumeUrl`, `resumeFilename`) on the Candidate row. Swap the storage call for S3/GCS in production; nothing else in the service needs to change.
- **Feign clients** -- `JobServiceClient.getJob` (validate PUBLISHED + snapshot) and `CompanyServiceClient.getCompany` (resolve company name) are both real, single-purpose calls with an actual call site. `AuthServiceClient` is an interface-only stub (Auth Service doesn't expose a "get any user by id" endpoint yet) -- documented as unused scaffolding, not pretended to work.
- **Redis** -- profile and dashboard reads are cached (`candidateProfile` 30 min, `candidateDashboard` 5 min); every wizard-step write evicts both explicitly via `CacheManager` rather than fragile SpEL keys, since a `@SQLRestriction`-filtered re-fetch after a delete can silently return nothing.
- **Kafka** -- every publish method is `@Async` (`KafkaTemplate.send()` blocks resolving broker metadata before returning a future otherwise) so a Kafka outage never stalls a candidate's write.

## 5. REST API Reference

All endpoints require `Authorization: Bearer <JWT>`.

### Profile (role: CANDIDATE, self-service)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/candidates/me` | Full profile |
| PUT | `/api/v1/candidates/me/basic-info` | Step 1 (creates profile on first call) |
| POST | `/api/v1/candidates/me/resume` | Step 2 (multipart) |
| PUT | `/api/v1/candidates/me/education` | Step 3 (full replace) |
| PUT | `/api/v1/candidates/me/experience` | Step 4 (full replace) |
| PUT | `/api/v1/candidates/me/skills` | Step 5 (full replace) |
| PUT | `/api/v1/candidates/me/projects` | Step 6 (full replace) |
| PUT | `/api/v1/candidates/me/certifications` | Step 7 (full replace) |
| PUT | `/api/v1/candidates/me/languages` | Step 8 (full replace) |
| PUT | `/api/v1/candidates/me/job-preferences` | Step 9 |
| PUT | `/api/v1/candidates/me/portfolio` | Step 10 |

### Applications
| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/v1/applications` | CANDIDATE | Apply to a job |
| GET | `/api/v1/applications?filter=` | CANDIDATE | `all\|active\|offer\|rejected` |
| GET | `/api/v1/applications/{id}` | CANDIDATE | Detail + full stage history |
| POST | `/api/v1/applications/{id}/withdraw` | CANDIDATE | Withdraw |
| POST | `/api/v1/applications/{id}/stage` | recruiting roles | Advance the pipeline stage |

### Saved jobs & dashboard
| Method | Path | Role |
|---|---|---|
| POST / DELETE `/{jobId}` / GET | `/api/v1/saved-jobs` | CANDIDATE |
| GET | `/api/v1/dashboard` | CANDIDATE |

## 6. Kafka Events (published)

`candidate.profile.created`, `candidate.profile.updated`, `candidate.resume.uploaded`, `candidate.application.submitted`, `candidate.application.withdrawn`, `candidate.application.stage-changed`, `candidate.job.saved`, `candidate.job.unsaved` -- consumed today by nothing in this codebase; scaffolded for the future AI Resume Parsing and Notification services, matching the same forward-looking pattern Company/Job Service already established.

## 7. Testing

16 unit/service tests (JUnit 5 + Mockito + AssertJ), covering: profile creation vs. update event branching, the full-replace skill upsert, apply-time duplicate/not-published guards, the graceful degrade when Company Service is unreachable, withdraw's terminal-state guard, and the stage-change endpoint's cross-company 404 + invalid-transition guard. All passing.

A Testcontainers MySQL integration test is the natural next step, same as Company/Job Service -- not included yet for the same reason (no reliably reachable local Docker daemon during this build).

## 8. Docker & Local Run

```
docker compose up --build
```

Spins up candidate-db (3309), redis (6382), kafka (9095), eureka (8764), candidate-service (8085).

Without Docker: a local MySQL/Redis/Kafka reachable per `application.yml`, then `mvn spring-boot:run`. Swagger UI at `http://localhost:8085/swagger-ui.html`.

## 9. Senior Architect Review -- Improvement Areas

- **Scalability**: profile reads are cache-first; the 10-step wizard's full-replace writes are O(existing + incoming) per call, fine at candidate scale but would benefit from batched saves if a candidate ever has hundreds of entries (they won't).
- **Security**: every self-service query is scoped to `userId` off the JWT, never a client-supplied id; the one cross-role endpoint (stage-change) is company-scoped and returns an identical 404 for both "doesn't exist" and "wrong company" to avoid leaking existence.
- **Maintainability**: job/company data is snapshotted rather than joined live, so this service degrades gracefully and independently of Job/Company Service's uptime -- the trade-off is that a snapshot can go stale (e.g. a job retitled after applying); acceptable since an application is a record of what was applied to, not a live mirror.
- **Next real gap**: resume parsing (AI-extracted skills/experience auto-filling the wizard) doesn't exist yet -- `resumeParsedAt`/`aiResumeScore` columns are already in place for whenever that service lands, so no schema migration will be needed to wire it in.
