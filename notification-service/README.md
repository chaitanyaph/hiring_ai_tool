# Notification Service

Shared infrastructure microservice: email dispatch (Spring Mail + Thymeleaf), in-app notifications, configurable email templates, delivery tracking/history, retry + Dead Letter Queue, notification preferences, and audit/debug logs. Consumed by every other service in the platform via Kafka; owns no business domain of its own.

## Critical scope note (read first)

**`offer-management-service` does not exist on disk** — only 10 sibling services are actually built. Offer domain logic (`OfferAccepted`/`OfferRejected`) lives inside `application-service` itself; there is no `OfferGenerated`/`OfferWithdrawn` publisher anywhere. The requested "Offer Management Service" Feign client is therefore **not built** — offer events are consumed directly from `application-service`'s own topics, which already carry `applicationId`/`candidateId`/`jobId` (enough to enrich via the Candidate/Application clients already in place).

## Real vs. requested Kafka events

Of the 28 event names in the original spec, research against every sibling service's actual `KafkaTopics.java` found:
- **~13 map to a real, correctly-topic'd event** (some renamed — e.g. `RecruiterInvited` → company-service's `TeamInvitationCreated`).
- **~4 exist but only under a different topic than the aspirational name implies** (e.g. `CandidateShortlisted` is really on `ai-interview.candidate.shortlisted`, not the `shortlisting.candidate.shortlisted` some other services *expected* but nothing publishes).
- **~11 do not exist anywhere in the codebase**: `RecruiterRemoved`, `AIInterviewInvitationCreated`, `AIInterviewReminder`, `CodingAssessmentAssigned`, `CodingAssessmentReminder`, `TechnicalInterviewReminder`, `HRInterviewReminder`, `OfferGenerated`, `OfferWithdrawn`, `BackgroundVerificationStarted`, `BackgroundVerificationCompleted`.

**Only the real, correctly-topic'd events are consumed** (see `constants/KafkaTopics.java` and the 8 consumer classes under `kafka/consumer/`) — subscribing to a topic with no publisher would silently sit empty forever, which is worse than not subscribing at all. The 11 nonexistent events' templates are still seeded (`V2__seed_notification_templates.sql`, `trigger_event = 'NONE'`) so they're ready — manually sendable via the admin API — the moment an upstream publisher exists.

## Database Design — `notification_db`

Consolidated from the suggested 9 tables to **6**:

| Table | Notes |
|---|---|
| `notification_template` | Email-only (no in-app template UI exists anywhere in the Figma — in-app content is code-generated per event). |
| `notification` | In-app notification instance; absorbs `notification_status` as an enum column. |
| `email_queue` | Absorbs `notification_history` + `notification_delivery` + the rest of `notification_status` — one status-machine row per email serves as the send queue AND history/delivery record; the Queue/History/Scheduled/Failed Figma tabs are all just filters over this one table. |
| `email_attachment` | Stored inline as `LONGBLOB` — no object-storage (MinIO/S3) client was requested for this service. |
| `notification_preference` | One row per (userId, category); matches the candidate Settings screen's 4 categories exactly. |
| `notification_log` | Append-only raw log lines — backs the "Notification logs" tab. |

## Figma reality check

The recruiter-side `#sec-notifications` page is **not a personal inbox** — it's an admin email-ops console (Dashboard/Templates/History/Logs/Scheduled/Failed tabs). The recruiter bell icon just fires a fixed toast (`"No new alerts right now"`), never opening a real dropdown. Only the candidate dashboard's `#csec-notifications` shows a personal notification list, and even that is currently static (no read/unread visual, no archive, no delete, no search/filter, no bulk actions, no pagination). This service still builds the **full** personal-notification CRUD API generically for any authenticated role, because the text spec's "In-App Notifications" section explicitly requires Read/Unread/Archive/Delete/Mark-All-Read across Recruiter/Candidate/HR/Hiring Manager/Technical Interviewer — necessary supporting infrastructure for what the candidate's list is a real (if currently simplified) instance of, not an invented screen.

## Known enrichment gap

`interview-management-service` reserved an internal/machine-to-machine endpoint wildcard in its `SecurityConstants` but never implemented a controller under it (confirmed by research — a dead placeholder). `InterviewManagementServiceClient` therefore calls an **auth-protected** endpoint that will typically 401 for this service's un-authenticated backend calls. Every call site wraps this in a safe try/catch that degrades to the fields already present on the triggering Kafka event (which usually include `scheduledDate`/`scheduledTime`/`roundType` already) — a 401 here never blocks a send. For `InterviewRescheduled`/`InterviewCancelled` events (which carry only `interviewId`, no candidate contact info), recipient resolution is recovered from the original "interview scheduled" email already queued for the same `interviewId` (`email_queue` reused as an implicit cache) — if no such prior email exists, the event is logged as a warning and skipped rather than guessing a recipient.

## Retry Strategy & Dead Letter Queue

- **Email send retry** (`email_queue.attempts`/`maxAttempts`=5/`nextRetryAt`): exponential backoff 1m → 5m → 15m → 1h → 6h, driven by `EmailDispatchWorker` (`@Scheduled`, 30s poll). Manual (`POST /api/v1/email-queue/retry/{id}`) and bulk (`POST /api/v1/email-queue/retry-bulk`, matching the Figma's "Retry selected" bulk bar) retry both reset attempts to 0.
- **Kafka-level DLQ** (distinct from email retry — this is about failing to even *process* a message): `DefaultErrorHandler` + `FixedBackOff(2000, 3)` + `DeadLetterPublishingRecoverer` republish to `<topic>.DLT` after 3 failed attempts, configured once at the container-factory level so every listener gets it for free.
- **No real ESP webhook exists** — a successful send is marked `SENT` only; `DELIVERED`/`OPENED`/`BOUNCED` transitions would require a real email provider's webhook integration, not built in this pass (flagged, not faked).

## OpenFeign Communication

| Client | Endpoint | Purpose |
|---|---|---|
| `CandidateServiceClient` | `GET /api/v1/candidates/{id}/summary` | name/email enrichment |
| `CompanyServiceClient` | `GET /api/v1/companies/{id}` | company name enrichment |
| `ApplicationServiceClient` | `GET /internal/application/job/{jobId}` | candidate/job enrichment (job-scoped list, no single-application internal endpoint exists) |
| `InterviewManagementServiceClient` | `GET /api/v1/recruiter/interviews/{id}` | flagged auth-protected endpoint, see above |

## Build environment note

This offline build environment has no network access to Maven Central. `spring-boot-starter-mail` was already cached at the exact parent version (3.3.4); `spring-boot-starter-thymeleaf` was only cached at 3.3.5, so its version is pinned explicitly in `pom.xml` (a same-minor-line patch, not a downgrade risk).

## Running locally

```
docker compose up -d
```

Or standalone: `mvn spring-boot:run` (requires MySQL on `3306`, Redis on `6379`, Kafka on `9092`, and real SMTP credentials via `SMTP_USERNAME`/`SMTP_PASSWORD` env vars to actually send mail — see `application.yml`).
