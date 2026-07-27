# Cadence Resume Parser Service

Owns two responsibilities: (1) extracting structured candidate data from an already-uploaded resume PDF, and (2) comparing that structured data against a job's requirements to produce a match score, skill comparison, and AI hiring recommendation -- both using free LLMs (Gemini, Groq, or a local Ollama model, selected purely by configuration). Resume Matching is deliberately **not** a separate microservice: it is a second capability merged into this same service, reusing the same AI provider infrastructure, database, and Kafka wiring. It does **not** store the resume file (MinIO, owned by Resume Service), manage the candidate profile (Candidate Service), manage applications (Application Service), or conduct interviews (the future AI-Interview service). This service only answers "what does this resume say, structured" and "how well does this candidate match this job."

## 1. Functional Requirements

- Automatic, asynchronous parsing triggered by Resume Service's `ResumeUploaded` Kafka event -- no manual trigger for the happy path.
- Extracts: name, email, phone, location, LinkedIn/GitHub/portfolio URLs, professional summary, skills (technical/soft/programming language/framework/library/database/cloud/devops), work experience, projects, education, certifications, achievements, languages known, current company/designation, total experience, notice period, expected salary.
- Recruiter-facing parsing queue (filter by status, search by candidate once parsed) with KPI counts (Queued/Processing/Parsed today/Failed).
- Recruiter-facing parsed-resume detail view: processing stepper, every extracted section, parsing logs, retry-on-failure.
- Duplicate parsing prevention (same `resumeId` + `checksum` already `PARSED` is skipped) and idempotent processing (a Redis lock keyed by `resumeId:checksum` prevents a redelivered Kafka message or a concurrent retry from double-processing the same resume).
- Publishes `ResumeParsed` / `ResumeParsingFailed`; consumes `ResumeUploaded` (Resume Service) and `CandidateDeleted` (Candidate Service).
- **Resume Matching (new)**: automatic, asynchronous match analysis triggered by Application Service's `ApplicationCreated` Kafka event. Fetches the job's requirements (Job Service, via Feign) and this service's own parsed resume, then asks the active AI provider to compare them.
- Generates: overall match score, technical/programming-language/framework/database/cloud/devops sub-scores, experience/project/education/certification match, communication/problem-solving/learning-ability predictions, matched skills, missing skills (required vs. preferred), strengths, weaknesses, recommended learning topics, hiring recommendation (PROCEED/HOLD/REJECT), overall AI summary.
- Recruiter-facing per-job candidate ranking table + KPI summary + top-N + cross-job Recommendations feed (job/department/min-score filters) + full analysis drawer (score breakdown, skill comparison, missing skills, strengths/weaknesses, AI recommendation) + manual recalculate.
- Publishes `ResumeAnalyzed` / `ResumeAnalysisFailed`; consumes `ApplicationCreated` (Application Service).

## 2. Non-Functional Requirements

- **Free AI providers only**: Gemini (default), Groq, or a local Ollama instance -- selected by `resume-parser.ai.provider` in `application.yml`. No paid provider is wired in, by design.
- **Strategy pattern**: `ResumeParserProvider` interface + one implementation per provider + `ResumeParserProviderFactory` as the runtime-selecting context. Switching providers is a config change, never a code change. The same three providers implement `analyzeMatch(...)` alongside `parse(...)` -- see Architecture Decisions for why this isn't a second, parallel provider hierarchy.
- **Resilience**: a malformed Kafka event, a Feign timeout, an LLM outage, or an unparseable PDF all fail into `status=FAILED` with a `parser_log` entry and a `ResumeParsingFailed` event -- never a silently stuck row, never a crashed consumer thread. Match analysis mirrors this exactly with `resume_match.status=FAILED` and `ResumeAnalysisFailed`.
- **Idempotent & duplicate-safe**: see above. Match analysis has the same Redis-lock treatment, keyed by `applicationId`.

## 3. System Workflow

```
Resume Service uploads PDF, stores in MinIO
  --> publishes ResumeUploaded (resumeId, candidateId, checksum, occurredAt)
  --> this service's Kafka consumer picks it up
  --> duplicate-parsing check + Redis idempotency lock
  --> upsert parsed_resume row (status=QUEUED)
  --> Feign --> Candidate Service: candidate exists & ACTIVE?
  --> Feign --> Resume Service: GET /internal/resumes/{id} (metadata)
  --> Feign --> Resume Service: GET /internal/resumes/{id}/object (bucket/object coordinates)
  --> status=EXTRACTING_TEXT --> read PDF bytes straight out of the shared MinIO bucket
  --> Apache PDFBox extracts text --> TextCleaner normalizes it
  --> status=PARSING_FIELDS --> active ResumeParserProvider sends the text + fixed-schema prompt, gets structured JSON back
  --> ParsedDataValidator sanity-checks the result (name; email or phone)
  --> persist parsed_resume + 7 child tables, log every step to parser_log
  --> status=PARSED --> publish ResumeParsed
  --> on any failure at any step --> status=FAILED, parser_log ERROR row, publish ResumeParsingFailed
```
`POST /retry` re-runs this exact pipeline for a `FAILED` resume, incrementing `attempt_count`.

### Resume Matching workflow

```
Application Service creates an application
  --> publishes ApplicationCreated (applicationId, companyId, jobId, candidateId, resumeId, occurredAt)
  --> this service's Kafka consumer picks it up
  --> upsert resume_match row (one per applicationId)
  --> resumeId present in the event?
        no  --> status=AWAITING_RESUME (parked; recalculate is the escape hatch -- see below)
        yes --> is that resume's parsed_resume row already status=PARSED?
                  no  --> status=AWAITING_PARSE (parked; auto-resumed by the parsing
                          pipeline's own completion hook once that resume finishes parsing)
                  yes --> Redis idempotency lock (keyed by applicationId) --> status=ANALYZING
                          --> Feign --> Job Service: GET /internal/jobs/{jobId} (requirements + departmentId)
                          --> build ParsedResumeSnapshot (this service's own parsed data) +
                              JobRequirementsSnapshot (Job Service's data)
                          --> active ResumeParserProvider.analyzeMatch(...) --> structured JSON back
                          --> persist resume_match + skill_match + missing_skill + ai_recommendation +
                              resume_match_note, log every step
                          --> status=ANALYZED --> publish ResumeAnalyzed
  --> on any failure at any step --> status=FAILED, publish ResumeAnalysisFailed
```
`POST /recalculate/{applicationId}` re-runs this pipeline. It is also the documented workaround for
a real gap discovered in Application Service: `ApplicationCreatedEvent.resumeId` is null at publish
time today (its `Application` builder chain never sets `resumeId`), so every match starts life as
AWAITING_RESUME. Recalculate re-fetches the application from Application Service (which may have
resolved a resumeId by then) rather than silently patching that other service's business logic --
see Architecture Decisions.

## 4. Database Design

`resume_parser_db`, 9 tables. Only `parsed_resume` (the aggregate root, one row per `resume_id`) carries the full audit trail (`created_at`/`updated_at`/`created_by`/`updated_by`/`version`) -- the 8 children are system-generated and fully replaced on every (re)parse, so per-row audit columns would carry no real information (mirrors Candidate Service's own `candidate_skills`/`candidate_languages` tables, which are equally simple key facts with no audit columns either).

- `parsed_resume` -- status machine (`QUEUED`/`EXTRACTING_TEXT`/`PARSING_FIELDS`/`PARSED`/`FAILED`), attempt count, provider used, and every scalar extracted field.
- `candidate_skill` -- one normalized table with a `skill_category` discriminator (TECHNICAL/SOFT/PROGRAMMING_LANGUAGE/FRAMEWORK/LIBRARY/DATABASE/CLOUD/DEVOPS), not eight separate near-identical tables.
- `candidate_experience`, `candidate_project`, `candidate_education`, `candidate_certification`, `candidate_achievement` -- each with a `display_order` for stable rendering.
- `candidate_language` -- name + proficiency.
- `parser_log` -- append-only INFO/WARN/ERROR trail feeding the drawer's "Parsing logs" box.

Resume Matching adds 5 more tables (same `resume_parser_db`, no new schema):

- `resume_match` -- the aggregate root, one row per `application_id` (unique). Carries the status machine (`AWAITING_RESUME`/`AWAITING_PARSE`/`ANALYZING`/`ANALYZED`/`FAILED`), every AI-generated score/label/prediction, and a denormalized `department_id` (fetched from Job Service at analysis time) so the Recommendations feed can filter by department without an N+1 Feign call per row.
- `skill_match` -- one row per matched skill, same `skill_category` discriminator as `candidate_skill`.
- `missing_skill` -- one row per missing skill, plus `is_required` (required vs. preferred).
- `ai_recommendation` -- 1:1 with `resume_match` (unique `resume_match_id`): hiring recommendation, overall AI summary, recommended learning topics.
- `resume_match_note` -- strengths and weaknesses as ordered rows with a `note_type` discriminator (`STRENGTH`/`WEAKNESS`), not two separate tables.

## 5. Folder Structure

```
src/main/java/com/cadence/resumeparserservice/
  constants/      KafkaTopics, SecurityConstants, PlatformRole, ParsingStatus, SkillCategory, LogLevel, AiProvider
  entity/         BaseAuditEntity, ParsedResume (+8 simple child entities)
  repository/
  security/       JwtAuthenticationFilter, JwtTokenValidator, CurrentUser, CurrentUserProvider
  config/         SecurityConfig, KafkaProducerConfig, KafkaConsumerConfig, MinioConfig, SwaggerConfig
  exception/      ResumeParserServiceException hierarchy, ErrorCode, ApiError, GlobalExceptionHandler
  dto/response/   Queue/summary/aggregate/status/log responses + matching responses + PagedResponse + ApiResponse
  mapper/         ParsedResumeMapper, ResumeMatchMapper (MapStruct)
  feign/          ResumeServiceClient, CandidateServiceClient, JobServiceClient, ApplicationServiceClient (+ feign/dto)
  kafka/          event/, producer/, consumer/
  provider/       ResumeParserProvider + Abstract + Gemini/Groq/Ollama impls + ParsedResumeData/Snapshot/MatchAnalysisData
  strategy/       ResumeParserProviderFactory (the Strategy *context*)
  validation/     ParsedDataValidator
  util/           PdfTextExtractor, TextCleaner, MinioObjectReader
  service/, service/impl/
      ResumeParsingService(+PipelineRunner), ParserQueueService, ParsedResumeQueryService
      ResumeMatchAnalysisService(+PipelineRunner), ResumeAnalysisQueryService
  controller/     ParserQueueController, ParsedResumeController, InternalParsedResumeController,
                   ResumeAnalysisController, InternalResumeAnalysisController
```

## 6. Business Rules -- As Implemented

| Rule | Where enforced |
|---|---|
| Duplicate parsing prevention | `existsByResumeIdAndChecksumAndStatus(..., PARSED)` check before any work starts |
| Idempotent processing | Redis `SETIFABSENT` lock keyed by `resumeId:checksum`, released in a `finally` once the pipeline finishes |
| Retry only allowed on a failed parse | `ResumeParsingServiceImpl.retryParsing` throws `ParsingConflictException` otherwise |
| Candidate must be ACTIVE | Feign call to Candidate Service before any PDF work begins |
| Structural validation of LLM output | `ParsedDataValidator` -- requires a name and at least one of email/phone |
| A malformed Kafka event never crashes the consumer | try/catch around every `@KafkaListener` method, same defensive posture as every sibling service |
| A match can't start analyzing without both a job and a parsed resume | `AWAITING_RESUME`/`AWAITING_PARSE` states; the parsing pipeline's completion hook and `recalculate` are the only ways out of them |
| Recalculate can't be called on an in-flight analysis | `ResumeMatchAnalysisServiceImpl.recalculate` throws `ParsingConflictException` when `status=ANALYZING` |
| Idempotent match processing | Redis `SETIFABSENT` lock keyed by `applicationId`, released in a `finally` once the pipeline finishes |

## 7. Architecture Decisions

- **Strategy pattern for the AI provider, not a `switch`** -- `ResumeParserProvider` is the strategy, `ResumeParserProviderFactory` is the context. Adding a fourth provider later means adding one `@Component`, not touching the factory.
- **`@Async` pipeline lives on a separate bean (`ResumeParsingPipelineRunner`), not on `ResumeParsingServiceImpl` itself** -- Spring can't proxy a method calling another `@Async` method on the *same* bean instance (a well-known self-invocation pitfall), so the synchronous "front door" (lock, validation, state transition) and the actual async pipeline work are deliberately two different Spring beans.
- **MinIO is shared object storage, not something this service owns** -- Resume Service's own README states the `/object` internal endpoint exists precisely so a future parser can "read a file straight out of MinIO with its own credentials... instead of proxying every download through this service's HTTP path." Database-per-service is about the relational store; the blob store is intentionally shared here, per Resume Service's own stated intent. This service takes a read-only `io.minio` client, gets bucket/object coordinates via Feign, and never creates or writes to a bucket.
- **`handleCandidateDeleted` hard-deletes, doesn't archive** -- unlike Resume Service's own conservative "archive the file, don't purge" policy (justified there because the file itself may have audit/legal retention value), parsed data is fully derived and regenerable from the source PDF. Once its source candidate is gone, keeping it around serves no independent purpose, so this is a genuine hard delete (cascading to all 8 child tables via `ON DELETE CASCADE`), explicitly reasoned as a deliberate deviation from the sibling pattern rather than a silent inconsistency.
- **Skills are one table with a category discriminator**, not eight tables -- see Database Design above.
- **The internal aggregate endpoint** exists for the same reason Resume Service's `/object` endpoint does: so the not-yet-built Shortlisting service can read parsed/matched data at pipeline volume without going through a human-facing, JWT-gated path.

### Resume Matching -- decisions specific to the extension

- **No separate Resume Matching microservice** -- per explicit instruction, matching/analysis/AI-recommendation is a second capability on this same service, sharing its database, AI provider infrastructure, and Kafka wiring, not a new deployable.
- **`ApplicationCreated`, not `ResumeUploaded`, is the trigger** -- a resume by itself carries no job context (`ResumeUploaded` has no `jobId`), so it cannot drive job-scoped matching. `ApplicationCreatedEvent`'s own Javadoc in Application Service already documents it as the intended trigger for this exact purpose.
- **Real gap found, not fixed silently**: `ApplicationCreatedEvent.resumeId` is null at publish time today, because `ApplicationServiceImpl`'s `Application` builder chain never calls `.resumeId(...)`. Rather than patching another service's business logic without authorization, this service was designed to degrade gracefully around it: `AWAITING_RESUME` parks the row, and `POST /recalculate/{applicationId}` re-fetches the application (via the existing `GET /internal/application/job/{jobId}` endpoint) to pick up a resumeId once Application Service has one. This one-line fix in Application Service is still flagged as a follow-up for separate authorization, not applied here.
- **One provider hierarchy, not two** -- the spec's suggested naming (`ResumeAIProvider`/`GeminiResumeProvider`/...) was deliberately not built as a second, parallel hierarchy. `ResumeParserProvider` gained a second method, `analyzeMatch(...)`, on the same Gemini/Groq/Ollama implementations, because they already own the HTTP client + config wiring `parse()` needs -- duplicating that for a second interface would be the same three REST clients built twice for no behavioral gain. Flagged here as a deliberate, reasoned deviation from the literal request.
- **`resume_match.department_id` is denormalized**, fetched from Job Service once at analysis time -- lets the cross-job Recommendations feed filter by department without a Feign call per ranking row.
- **The parsing pipeline calls into the matching service, not the other way around** -- `ResumeParsingPipelineRunner`, on successful parse, calls `ResumeMatchAnalysisService.onResumeParsed(resumeId)` to resume any `AWAITING_PARSE` rows. This keeps the "resume finished parsing" event local (a direct method call, not a second Kafka round-trip) since both capabilities live in the same service and the same JVM.
- **Same hiring-recommendation vocabulary as AI Interview Evaluation** (`PROCEED`/`HOLD`/`REJECT`) -- kept consistent across every AI-recommendation surface in the platform rather than inventing a second scale.

## 8. REST API Reference

### Recruiter (JWT, recruiting role)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/parser/queue` | Filterable/searchable parsing queue table |
| GET | `/api/v1/parser/queue/summary` | KPI counts (Queued/Processing/Parsed today/Failed) |
| GET | `/api/v1/parser/resumes/{resumeId}` | Full parsed resume aggregate (drawer main view) |
| GET | `/api/v1/parser/resumes/{resumeId}/skills` | Extracted skills |
| GET | `/api/v1/parser/resumes/{resumeId}/experience` | Extracted work experience |
| GET | `/api/v1/parser/resumes/{resumeId}/education` | Extracted education |
| GET | `/api/v1/parser/resumes/{resumeId}/projects` | Extracted projects |
| GET | `/api/v1/parser/resumes/{resumeId}/certifications` | Extracted certifications |
| GET | `/api/v1/parser/resumes/{resumeId}/status` | Processing stepper / lightweight poll |
| GET | `/api/v1/parser/resumes/{resumeId}/logs` | Parsing log trail |
| POST | `/api/v1/parser/resumes/{resumeId}/retry` | Retry a failed parse (COMPANY_ADMIN/recruiter roles only, not Hiring Manager) |

### Internal (trusted network, no auth)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/parser/resumes/{resumeId}` |

Deliberately **not** exposed: standalone `/languages` or `/achievements` endpoints (both ride inside the aggregate -- the Figma drawer shows Languages only as an inline chip-group with no dedicated view, and Achievements isn't in the Figma at all), and the queue table's "Job" column (that's Application Service's data; composing it is a frontend/gateway concern, not a Feign call this service is authorized to make).

### Resume Analysis / Matching -- Recruiter (JWT, recruiting role)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/resume-analysis/jobs/{jobId}` | Candidate ranking table for a job (search by name/email) |
| GET | `/api/v1/resume-analysis/jobs/{jobId}/summary` | Resume Analysis Dashboard KPI row |
| GET | `/api/v1/resume-analysis/top/{jobId}` | Top-ranked candidates for a job |
| GET | `/api/v1/resume-analysis/recommendations` | Cross-job feed, optional job/department/min-score filters |
| GET | `/api/v1/resume-analysis/applications/{applicationId}` | Full match analysis (drawer main view) |
| GET | `/api/v1/resume-analysis/candidates/{candidateId}` | A candidate's analyses across every job applied to |
| POST | `/api/v1/resume-analysis/recalculate/{applicationId}` | Recalculate (COMPANY_ADMIN/recruiter roles only, not Hiring Manager) |
| GET | `/api/v1/resume-analysis/{analysisId}/skills` | Matched skills |
| GET | `/api/v1/resume-analysis/{analysisId}/missing-skills` | Missing skills (required vs. preferred) |
| GET | `/api/v1/resume-analysis/{analysisId}/strengths` | AI-identified strengths |
| GET | `/api/v1/resume-analysis/{analysisId}/weaknesses` | AI-identified weaknesses |
| GET | `/api/v1/resume-analysis/{analysisId}/recommendation` | AI hiring recommendation + summary |
| GET | `/api/v1/resume-analysis/{analysisId}/summary` | Score/label breakdown only (drawer's compact summary) |

### Resume Analysis -- Internal (trusted network, no auth)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/resume-analysis/{applicationId}` |
| GET | `/api/v1/internal/resume-analysis/job/{jobId}` |

## 9. Kafka Events

**Published**: `resume-parser.resume.parsed`, `resume-parser.resume.parsing-failed`, `resume-parser.resume.analyzed`, `resume-parser.resume.analysis-failed`.
**Consumed**: `resume.resume.uploaded` (Resume Service -- triggers the parsing pipeline), `candidate.profile.deleted` (Candidate Service -- triggers cleanup), `application.application.created` (Application Service -- triggers match analysis).

## 10. Testing

31 unit tests (JUnit 5 + Mockito + AssertJ): the parsing orchestration service's duplicate-prevention/idempotency-lock/retry-conflict/candidate-cleanup paths, the match orchestration service's AWAITING_RESUME/AWAITING_PARSE transitions, recalculate's conflict/re-fetch rules, and the parsing-completion hook; the provider factory's case-insensitive selection and unknown-provider failure; the parsed-data validator's required-field rules; the queue service's progress-percent mapping and KPI aggregation; and the analysis query service's summary aggregation and ranking enrichment. All passing.

A Testcontainers MySQL integration test and a mocked-HTTP test per AI provider are the natural next step, same documented gap as every other service in this platform (no reliably reachable local Docker daemon during this build).

## 11. Docker & Local Run

```
docker compose up --build
```
Spins up resumeparser-db (3312), redis (6385), kafka (9098), eureka (8767), resume-parser-service (8088).

**Running alongside Resume Service** (needed for real MinIO access, not just a demo): each service's `docker-compose.yml` defines its own project-scoped `cadence-net` network by default, so two independently-run `docker compose` stacks do *not* share a network out of the box. Start both stacks under the same Compose project name so they land on the same actual network and this service can resolve Resume Service's `minio` container by name:
```
docker compose -p cadence -f ../resume-service/docker-compose.yml up -d
docker compose -p cadence -f docker-compose.yml up -d
```
Without Docker: local MySQL/Redis/Kafka reachable per `application.yml`, a MinIO instance reachable at `MINIO_ENDPOINT` (point it at Resume Service's own MinIO, e.g. `http://localhost:9000`, since it publishes that host port already), then `mvn spring-boot:run`. Swagger UI at `http://localhost:8088/swagger-ui.html`.

To use Ollama locally instead of Gemini/Groq: set `RESUME_PARSER_PROVIDER=ollama`, run `ollama serve` with a model pulled (e.g. `ollama pull llama3.1`), and leave `OLLAMA_BASE_URL` at its default (`http://localhost:11434` outside Docker, `http://host.docker.internal:11434` inside).

## 12. Senior Architect Review -- Improvement Areas

- **Scalability**: the Kafka consumer group can be scaled horizontally (partition the `resume.resume.uploaded` topic) without any code change, since the idempotency lock is in shared Redis, not in-process state.
- **Security**: every recruiter-facing endpoint is role-gated; the one trusted-network endpoint is scoped to an explicit `permitAll` allowlist by exact path, never a blanket wildcard.
- **Maintainability**: the AI provider is entirely swappable via one config property; `MinioObjectReader` is the only class that imports the MinIO SDK, so a future storage swap touches one file.
- **Next real gap**: `ResumeAnalyzed`/`ResumeAnalysisFailed` have no consumer yet -- same interim state every other service's forward-looking event scaffolding is already in, until a Shortlisting/Notification service exists. The other concrete follow-up is the one-line `ApplicationServiceImpl` fix for the null `resumeId` bug (see Architecture Decisions), left for separate authorization rather than applied here.
