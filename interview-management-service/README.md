# Interview Management Service

Owns every **manual, human-led** interview round that happens after a candidate clears AI Interview (`ai-interview-service`) and Coding Assessment (`coding-assessment-service`): Technical, Manager, Architect, HR, and company-defined Custom rounds. Owns round configuration, scheduling, panel assignment, feedback capture, recruiter decisioning, and the candidate's cross-stage hiring pipeline timeline.

Explicitly **not** this service's job: resume parsing, AI resume matching, AI interviews, coding assessments, candidate profiles, authentication, or email delivery.

## 1. Service Responsibility

See module breakdown below (§4). Ports: app `8091`, DB `3315` (`interview_management_db`), Redis `6388`, Kafka `9101`, Eureka `8770` — next free slot after `coding-assessment-service` (8090/3314/6387/9100/8769).

## 2. Business Workflow

1. Candidate clears AI Interview → `ai-interview.candidate.recommended` consumed → `candidate_timeline` row upserted (`AI_INTERVIEW`, done).
2. Candidate clears Coding Assessment → `assessment.coding.completed` consumed → `candidate_timeline` row upserted (`CODING_ASSESSMENT`, done, score).
3. Recruiter schedules a Technical/Manager/HR/Architect/Custom round (`modal-interview`) — panel assigned, placeholder meeting link generated, `InterviewScheduled` published.
4. Interviewer/recruiter submits feedback (`modal-submit-feedback`) → interview marked `COMPLETED`, `candidate_timeline` updated for that round's stage, this service's own rich `InterviewCompleted` event published **and** application-service's already-wired `interview.interview.completed` event published (aggregated score/feedback).
5. Recruiter records a decision: Move to HR / Next round / Select / Reject / Hold / Request another interview → activity logged, `CandidateMovedToHR`/`CandidateSelected`/`CandidateRejected` published where applicable.
6. Candidate views their own upcoming/past interviews and timeline read-only; can request a reschedule (logged, not auto-actioned — mirrors the Figma's `mockToast`-only behavior).

## 3. Database Design — `interview_management_db`

Consolidated from the suggested 11 tables down to **6** (see `V1__init_interview_management_schema.sql` for full column list and reasoning inline):

| Table | Absorbs |
|---|---|
| `interview_round` | Module 1 per-company round templates. |
| `interview` | Absorbs `interview_schedule`, `meeting_details`, `interview_status` — reschedule updates the row in place (same pattern as `candidate_assessment` in coding-assessment-service); status is a plain enum column, not a lookup table. |
| `interview_panelist` | Merges the suggested `interview_panel` + `interviewer_assignment` — no reusable named-panel concept exists anywhere in the Figma (the panel picker is a flat name-chip list). |
| `interview_feedback` | Absorbs `feedback_score` — one row holding all score columns, same precedent as every prior service. |
| `candidate_timeline` | One row per (applicationId, stage). |
| `interview_activity_log` | Append-only audit trail — carries the historical record a separate schedule/status table would otherwise have needed. |

## 4. Kafka Event Flow

**Consumed:**
- `ai-interview.candidate.recommended` → `CandidateRecommendedEvent` → upserts `AI_INTERVIEW` timeline stage.
- `assessment.coding.completed` → `CodingAssessmentCompletedEvent` → upserts `CODING_ASSESSMENT` timeline stage.

**Published, forward-scaffolded** (`interview-management.*`, no consumer exists anywhere yet — same posture every sibling service took for its own not-yet-consumed events): `InterviewScheduled`, `InterviewRescheduled`, `InterviewCancelled`, `InterviewCompleted` (rich), `CandidateMovedToHR`, `CandidateRejected`, `CandidateSelected`.

**Published into an already-live consumer** (the one genuine integration win in this build): `interview.interview.completed`, matching application-service's own pre-built `InterviewCompletedEvent{applicationId, interviewType, score, feedback}` exactly — its javadoc literally names this service as the intended publisher for TECHNICAL/MANAGER/HR rounds. The 3-dimension Figma feedback score is averaged and scaled to 0-100 for the `score` field; strengths/weaknesses are concatenated for `feedback`. `ARCHITECT`/`CUSTOM` round types (which don't exist in application-service's own `InterviewType` enum) are mapped down to `TECHNICAL` for this bridge only — flagged, not silently assumed correct.

## 5. OpenFeign Communication

| Client | Endpoint | Purpose |
|---|---|---|
| `CandidateServiceClient` | `GET /api/v1/candidates/{id}/summary` | name/email enrichment |
| `ApplicationServiceClient` | `GET /internal/application/job/{jobId}` | applications for a job |
| `JobServiceClient` | `GET /api/v1/internal/jobs/{jobId}` | job title enrichment |
| `CompanyServiceClient` | `GET /api/v1/companies/{id}` | company name enrichment |

**No internal endpoint exists on application-service to advance `ApplicationStatus` to REJECTED/HIRED, or to record a technical/manager/HR score** (confirmed by research — only `resume-score`/`interview-score`(=AI)/`coding-score`/`overall-score` exist, no status-transition endpoint). This service therefore cannot synchronously force an application's status; `CandidateSelected`/`CandidateRejected`/`CandidateMovedToHR` are published for a future consumer only — the same "no consumer yet" gap already flagged for `CandidateRecommended` in `ai-interview-service`.

**No Notification Service exists** — `notifyCandidateByEmail` is stored/validated only; no email is actually sent, same gap flagged in the last two services.

## 6. Architecture Decisions / Flagged Gaps

- **Round types**: the Figma's schedule modal only offers AI/Technical/HR (§A2, §A11) — Manager/Architect/Custom appear nowhere in the mockup, even though the text spec and application-service's own `InterviewType` enum (`AI, TECHNICAL, MANAGER, HR`) both acknowledge Manager rounds. This service's `RoundType` enum supports all 5 (`TECHNICAL, MANAGER, ARCHITECT, HR, CUSTOM`) per the text spec's Module 1, since a per-company `interview_round` list is exactly what would make the schedule modal's currently-static 3-option dropdown dynamic — a necessary supporting API, not an invented screen.
- **Feedback dimensions**: only Communication/Technical/Culture fit are collected by the Figma's `modal-submit-feedback` (§A4). `codingSkillsScore`/`problemSolvingScore`/`systemDesignScore`/`leadershipScore` are supported server-side per the text spec's fuller Module 5 model but are nullable and not reachable from the current UI.
- **Recommendation enum**: `PROCEED`/`HOLD`/`REJECT` — the Figma's exact 3 values, not the text spec's `HIRE`/`STRONG_HIRE`/`REJECT`/`HOLD` set.
- **No `INTERVIEWER` platform role**: the Figma's team-permission screen has one, but no sibling service's `PlatformRole` defines it (confirmed by research). Panel assignment/feedback authorization is done by checking `interview_panelist` membership OR a recruiting role, not a dedicated role.
- **No real calendar view**: confirmed absent from the Figma (§A12) — `#sec-interviews` is a flat day-grouped list, not a grid. Not built.
- **No real Google Meet/Calendar integration**: `meetingLink` is a plausible-looking placeholder string generated server-side when `autoGenerateMeetLink` is true, not a real Calendar API call — same "flag, don't fake" posture as `coding-assessment-service`'s Judge0 note.
- **Timeline coverage**: only `AI_INTERVIEW`, `CODING_ASSESSMENT`, `TECHNICAL_INTERVIEW`, `MANAGER_INTERVIEW`, `HR_INTERVIEW` stages are populated (from the 2 consumed events + this service's own actions). `APPLICATION`/`AI_RESUME_SCREENING` stages are not populated — no Feign client to resume-parser-service or an event source for them exists in this service's scope.

## 7. Security Architecture

Identical `SecurityConfig`/`JwtAuthenticationFilter`/`CurrentUser` package copied from `coding-assessment-service` (package renamed only). `@PreAuthorize` on write endpoints restricted to recruiting roles minus `HIRING_MANAGER` (view-only convention, same as every prior service). Feedback submission additionally requires the caller to be an assigned panelist for that interview OR a recruiting role. Candidate endpoints require `CANDIDATE` role + ownership checks against `candidateId`.

## 8. Folder Structure

Identical skeleton to `coding-assessment-service` (`constants/`, `dto/request/`, `dto/response/`, `entity/`, `exception/`, `feign/` + `feign/dto/`, `kafka/event/` + `producer/` + `consumer/`, `mapper/`, `repository/`, `security/`, `service/` + `service/impl/`, `controller/`, `config/`), package root `com.cadence.interviewmanagementservice`.

## Running locally

```
docker compose up -d
```

Or standalone: `mvn spring-boot:run` (requires MySQL on `3306`, Redis on `6379`, Kafka on `9092` — see `application.yml` for override env vars).
