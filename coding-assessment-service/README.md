# Cadence Coding Assessment Service

Owns the complete coding-assessment lifecycle: assessment definition, the reusable question bank, candidate invitation, the online IDE experience, code execution, AI code review, scoring, and the recruiter's queue/results/analytics views. It does not manage jobs, parse resumes, run AI interviews, manage candidate profiles, or authenticate -- those stay in their owning services.

## 1. Service Responsibility

Three conflicts were found between the text spec and the exported Figma mockup, resolved and flagged rather than silently picked one way or the other:

1. **Question Management has no dedicated screen in the exported HTML** -- the wizard's Step 2 is just a "Number of questions" number field, no "Create Question" modal. The full question-bank CRUD API surface is still built here: the seeded `codingQuestions[]` mockup data already establishes the exact field shape (title, marks, hidden test case count, description, example, constraints, starter code, test cases), the Assessment Details drawer lists "Questions" as a real section, and the system cannot function without real question content. The *screen* isn't pixel-mocked; the *data model* is.
2. **AI Code Review is a single "Clean / Needs attention" badge in the Figma** -- no complexity/SOLID/security/strengths-weaknesses UI exists for coding submissions anywhere in the export. The full structured review is still built (time/space complexity, naming conventions, SOLID principles, design patterns, security issues, optimization suggestions, clean-code notes, strengths/weaknesses/suggestions, overall rating) because Module 6 of the spec is explicit and detailed about it. The badge is now a *derived* summary (`overallRating >= 60 -> "Clean"`), not the ceiling of what's stored.
3. **No code-execution engine exists anywhere in this platform** (grepped all 8 sibling services -- zero matches for judge0/sandbox/compiler/execute). Compiling and running untrusted candidate code safely is a real security boundary, not something to hand-roll in-process. This service integrates **Judge0** (free, open-source, self-hostable sandboxed execution engine) behind a `CodeExecutionProvider` abstraction -- it never executes candidate code inside its own JVM/container.

## 2. Complete Workflow

```
ai-interview-service publishes CandidateRecommended (ai-interview.candidate.recommended)
  --> this service consumes it, persists assessment_eligibility if hiringRecommendation=PROCEED
      (Application Service doesn't yet consume this event itself -- a documented gap on
      ai-interview-service's own side -- so this service tracks eligibility locally)
  --> recruiter creates a Coding Assessment for a job (4-step wizard, matches Figma exactly)
  --> recruiter publishes it --> every eligible, not-yet-invited candidate for that job gets a
      candidate_assessment row (status=NOT_STARTED)
  --> candidate opens it from their dashboard, reads instructions, accepts rules, starts
  --> status=IN_PROGRESS, timer starts --> candidate writes code, Run (execution_log, unscored)
      and Submit (submission, scored against visible+hidden test cases via Judge0) per question
  --> candidate submits the whole assessment (or timer expires --> auto-submit)
  --> status=COMPLETED --> async: AI code review per submission --> final score aggregated
  --> Feign PUT application-service coding-score + publish CodingAssessmentCompleted
      (assessment.coding.completed -- the EXACT topic/shape Application Service already
      actively consumes, zero changes needed on their side)
  --> recruiter dashboard shows queue/results/leaderboard/analytics
```

## 3. Sequence Diagram (text)

```
Recruiter        Coding Assessment Svc      Judge0        AI Provider   Application Svc   Kafka
   |                     |<--CandidateRecommended----------------------------------------------->|
   |                     |--persist eligibility                                                    |
   |--POST /assessments--->|                     |                |             |                 |
   |--POST /publish-------->|--invite eligible candidates (candidate_assessment rows)              |
Candidate                    |                     |                |             |                 |
   |--POST /accept-rules--->|                     |                |             |                 |
   |--POST /start----------->|                     |                |             |                 |
   |--POST /run-------------->|--execute(no verdict stored)->|      |             |                 |
   |<--stdout/stderr----------|<--------------------|                |             |                 |
   |--POST /submit------------>|--execute (per test case)-->|        |             |                 |
   |<--verdict+runtime+memory--|<--------------------|                |             |                 |
   |--POST /finish-------------->|--status=COMPLETED  |                |             |                 |
   |                             |--async: AI review------------------->|             |                 |
   |                             |--aggregate final score              |             |                 |
   |                             |--PUT coding-score------------------------------->|                 |
   |                             |--publish CodingAssessmentCompleted------------------------------->|
Recruiter                        |                     |                |             |                 |
   |--GET /results/summary-------->|                     |                |             |                 |
```

## 4. Database Design -- `coding_assessment_db`

13 tables, consolidated from a larger suggested list (see Architecture Decisions):

| Table | Purpose |
|---|---|
| `assessment` | root, audited: definition, config flags, schedule |
| `assessment_question` | join: assessment ↔ question, display order |
| `question` | root, audited: the reusable, company-scoped question bank |
| `question_starter_code` | per-language starter template (child) |
| `question_test_case` | visible/hidden test cases (child) |
| `assessment_eligibility` | one row/`application_id`: CandidateRecommended tracking |
| `candidate_assessment` | root, audited: one row/(`assessment_id`,`application_id`) -- invitation + attempt + progress in one, mirrors ai-interview-service's `interview_session` dual role |
| `candidate_question_progress` | Question Navigator per-pill state (visited / marked-for-review) |
| `submission` | one row per submit attempt per question: code + verdict + score |
| `submission_test_case` | per-test-case pass/fail breakdown (child) |
| `execution_log` | unscored "Run Code" history against custom/sample input |
| `ai_code_review` | 1:1 with submission: complexity/quality/security/rating |
| `ai_code_review_note` | ordered STRENGTH/WEAKNESS/SUGGESTION rows (child) |
| `anti_cheat_log` | append-only tab-switch/fullscreen-exit/copy/paste/... event trail |

## 5. ER Diagram (text)

```
assessment --1:N--> assessment_question --N:1--> question --1:N--> question_starter_code
                                                            --1:N--> question_test_case
assessment --1:N--> candidate_assessment (application_id, assessment_id UNIQUE pair)
candidate_assessment --1:N--> candidate_question_progress
                      --1:N--> submission --1:N--> submission_test_case
                      --1:N--> execution_log
                      --1:N--> anti_cheat_log
submission --1:1--> ai_code_review --1:N--> ai_code_review_note
assessment_eligibility (application_id UNIQUE) -- read at publish time to build the invite list
```
All cross-service references (`job_id`, `company_id`, `application_id`, `candidate_id`) are plain UUID columns -- database-per-service, no cross-service FKs.

## 6. Kafka Flow

**Consumed**: `ai-interview.candidate.recommended` (`CandidateRecommendedEvent`) → populates `assessment_eligibility`.
**Published**: `assessment.coding.completed` (**exact existing topic string, exact existing consumer already active on Application Service's side** -- `CodingAssessmentCompletedEvent{applicationId, score}`). Also forward-scaffolded (no consumer exists anywhere yet, same posture every sibling service takes for a not-yet-built Analytics/Notification service): `coding-assessment.assessment.created`, `coding-assessment.assessment.started`, `coding-assessment.submission.created`.

## 7. OpenFeign Communication

- **Job Service** -- `GET /api/v1/internal/jobs/{jobId}` for job title on assessment creation and the candidate intro screen.
- **Application Service** -- `GET /internal/application/job/{jobId}` (candidate name/email snapshot, batch-fetched once per job, not per row), `PUT /internal/application/{id}/coding-score` (exact existing endpoint).
- **Candidate Service** -- `GET /api/v1/candidates/{candidateId}/summary`, available where Application Service's snapshot fields aren't sufficient.
- **Company Service** -- `GET /api/v1/companies/{id}` for the candidate intro screen's company name.
- **Notification Service** -- not built, not called. `sendReminder`/`resendInvite` validate state and log for audit purposes only; actual candidate notification is future work once that service exists.

## 8. Folder Structure

```
src/main/java/com/cadence/codingassessmentservice/
  constants/      KafkaTopics, SecurityConstants, PlatformRole, AssessmentStatus, AssessmentType,
                  CandidateAssessmentStatus, SubmissionStatus, Difficulty, ProgrammingLanguage,
                  TestCaseVisibility, AntiCheatEventType, NoteType, HiringRecommendation, AiProvider
  entity/         BaseAuditEntity, Assessment, Question (+11 child/simple entities)
  repository/
  security/       (copied verbatim from ai-interview-service)
  config/         SecurityConfig, KafkaProducerConfig, KafkaConsumerConfig, SwaggerConfig
  exception/      CodingAssessmentServiceException hierarchy, ErrorCode, GlobalExceptionHandler
  dto/request/    Assessment/Question create+update, Run/Submit, MarkForReview, Finish, AntiCheatEvent
  dto/response/   Assessment/queue/results/analytics responses, IDE question response, result responses
  mapper/         AssessmentMapper, QuestionMapper (MapStruct)
  feign/          JobServiceClient, ApplicationServiceClient, CandidateServiceClient, CompanyServiceClient
  kafka/          event/, producer/, consumer/
  execution/      CodeExecutionProvider + Judge0ExecutionProvider + ExecutionRequest/Result records
  review/         AICodeReviewProvider + Abstract + Gemini/Groq/Ollama impls + review records
  strategy/       CodeExecutionProviderFactory, AICodeReviewProviderFactory
  service/, service/impl/
      AssessmentService, QuestionService, AssessmentEligibilityService,
      CandidateAssessmentService, CodeExecutionService, CodingEvaluationService,
      AssessmentQueryService, SubmissionQueryService, LeaderboardService, AnalyticsService
  controller/     AssessmentController, QuestionController, CodingResultsController,
                  CandidateAssessmentController, InternalCodingAssessmentController
```

## 9. API Design mapped to Figma

### Recruiter -- Assessment Management (`#sec-coding-assessments` tab 1, `#modal-assessment`, `#drawer-assessment-details`)
| Method | Path | Figma origin |
|---|---|---|
| POST | `/api/v1/assessments` | Wizard step 4 "Save as draft" / "Publish now" |
| PUT | `/api/v1/assessments/{id}` | Edit an existing draft |
| DELETE | `/api/v1/assessments/{id}` | (draft-only delete) |
| POST | `/api/v1/assessments/{id}/publish` | Publish an existing draft |
| POST | `/api/v1/assessments/{id}/clone` | "Clone assessment" |
| POST | `/api/v1/assessments/{id}/archive` | "Archive assessment" |
| POST | `/api/v1/assessments/{id}/close` | "Close assessment" |
| PUT | `/api/v1/assessments/{id}/questions` | Question bank assignment (data-model-confirmed, see Section 1) |
| GET | `/api/v1/assessments` | Tab 1 table |
| GET | `/api/v1/assessments/{id}` / `/{id}/details` | `openAssessmentDetailsDrawer` |
| GET | `/api/v1/assessments/{id}/queue` | Tab 2 table |
| POST | `/api/v1/assessments/{id}/send-reminders` | Drawer footer "Send reminders" |
| POST | `/api/v1/assessments/{id}/candidates/{applicationId}/remind` | Per-candidate "Remind" / "Resend invite" |

### Recruiter -- Question Bank
| Method | Path |
|---|---|
| POST / PUT / DELETE | `/api/v1/questions[/{id}]` |
| GET | `/api/v1/questions`, `/api/v1/questions/{id}` |

### Recruiter -- Results / Analytics (tabs 3-4)
| Method | Path | Figma origin |
|---|---|---|
| GET | `/api/v1/assessments/{id}/results/summary` | Tab 3 KPI row |
| GET | `/api/v1/leaderboard/{assessmentId}` | Tab 3/4 "Candidate ranking" |
| GET | `/api/v1/submissions/{assessmentId}` | Tab 3 "Completed assessments" list |
| GET | `/api/v1/submissions/{assessmentId}/{applicationId}` | `openSubmissionDrawer` |
| GET | `/api/v1/submissions/detail/{submissionId}/ai-review` | Drawer's "Code review" / "AI analysis" actions |
| POST | `/api/v1/submissions/{assessmentId}/{applicationId}/move-to-next-stage` | Drawer footer |
| GET | `/api/v1/analytics/{assessmentId}` | Tab 4 KPI + difficulty/language breakdowns |

### Candidate (`#csec-coding-history`, `#assessment-view`)
| Method | Path | Figma origin |
|---|---|---|
| GET | `/api/v1/candidate/assessments` | History list |
| GET | `/api/v1/candidate/assessments/{id}` | Intro screen |
| POST | `/api/v1/candidate/assessments/{id}/accept-rules` | Rules screen checkbox + button |
| POST | `/api/v1/candidate/assessments/{id}/start` | `beginCodingAssessment` |
| GET | `/api/v1/candidate/assessments/{id}/questions/{questionIndex}` | Question Navigator click |
| POST | `/api/v1/candidate/run` | `runCode()` -- "Compile & Run" is one Figma button, so there is no separate `/compile` endpoint |
| POST | `/api/v1/candidate/submit` | `submitCode()` (per-question) |
| POST | `/api/v1/candidate/assessments/{id}/mark-for-review` | Footer "Mark for review" |
| POST | `/api/v1/candidate/assessments/{id}/finish` | `confirmSubmitAssessment` → `finishAssessment` |
| GET | `/api/v1/candidate/submissions/{id}` | `openSubmissionHistoryModal` |
| GET | `/api/v1/candidate/result/{id}` | Result screen |
| POST | `/api/v1/candidate/assessments/{id}/anti-cheat-events` | Tab-switch/fullscreen-exit/copy/paste logging |

### Internal (trusted network)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/coding-assessments/{applicationId}` |

## 10. API-to-Figma-action mapping

| Figma element | API |
|---|---|
| `+ Create assessment` → wizard step 4 submit | `POST /api/v1/assessments` |
| Wizard "Publish now" radio | same call, `publishNow=true`, triggers eligible-candidate invitation |
| `View details` (Tab 1 row) | `GET /api/v1/assessments/{id}/details` |
| Coding queue filter tabs | `GET /api/v1/assessments/{id}/queue?status=` |
| `Send reminder` (queue row) / `Resend invite` | `POST .../candidates/{applicationId}/remind` |
| `View submission` (queue/results row) | `GET /api/v1/submissions/{assessmentId}/{applicationId}` |
| Tab 3 "Candidate ranking" | `GET /api/v1/leaderboard/{assessmentId}` |
| Tab 4 KPI + breakdown cards | `GET /api/v1/analytics/{assessmentId}` |
| Drawer "Move to next stage" | `POST .../move-to-next-stage` |
| `csec-coding-history` row "Start assessment" | `GET .../{id}` then `POST .../accept-rules` then `POST .../start` |
| IDE `Compile & Run` | `POST /api/v1/candidate/run` |
| IDE `Submit` (per question) | `POST /api/v1/candidate/submit` |
| Question Navigator pill click | `GET .../questions/{questionIndex}` |
| `Submit assessment` header button → confirm modal | `POST .../finish` |
| Anti-cheat badges (tab-switch counter, fullscreen) | `POST .../anti-cheat-events` (fire-and-forget from the frontend on each detected event) |
| `View submission history` (candidate & result screen) | `GET /api/v1/candidate/submissions/{id}` |
| Result screen | `GET /api/v1/candidate/result/{id}` |
| Submission drawer "Code review" / "AI analysis" | `GET /api/v1/submissions/detail/{submissionId}/ai-review` |

## 11. Architecture Decisions

- **Consolidated the suggested table list**: `submission_result` merged into `submission` (one status-machine row per attempt, same precedent as `parsed_resume`). `assessment_activity` and `anti_cheat_log` described the same monitored-event concept -- kept as one table. `leaderboard` dropped entirely -- it's `candidate_assessment` ordered by `(totalScore DESC, timeUsedSeconds ASC)`, a query, not a materialized table, same precedent as dropping `candidate_ranking` in ai-interview-service. `assessment_invitation` merged into `candidate_assessment` (`invitedAt`/`remindedAt`/`expiresAt` columns on the same row that already tracks status/progress/score).
- **`allowedLanguages`/`tags` are comma-separated columns**, not child tables -- simple multi-select chip lists, same simplification precedent as other flat-string fields across the platform.
- **Judge0, not an in-process sandbox** -- see Section 1. `CodeExecutionProvider` is a real Strategy abstraction (mirroring the AI provider factories) even with a single implementation today, so a second execution backend later is a config change, not a rewrite.
- **Run is synchronous, evaluation is asynchronous** -- the candidate is actively waiting for Run/Submit output in the same request/response cycle (each Judge0 call is itself synchronous, `wait=true`), but the post-completion AI code review pass runs off-thread via `CodingEvaluationService`, same reasoning ai-interview-service's `InterviewEvaluationServiceImpl` already documents (`@Async` is safe without a separate pipeline-runner bean because it's always invoked from a *different* Spring bean).
- **Submission verdict severity ordering**: `COMPILE_ERROR > RUNTIME_ERROR > TIME_LIMIT_EXCEEDED > MEMORY_LIMIT_EXCEEDED > WRONG_ANSWER > ACCEPTED` -- the overall submission status reflects the worst outcome across every test case; a compile error stops further Judge0 calls early (identical outcome for every remaining case, no point burning calls).
- **Per-question score is proportional**: `marks * testCasesPassed / testCasesTotal`, rounded -- no partial-credit weighting per test case (all test cases for a question are equally weighted), a reasonable default not contradicted anywhere in the spec.
- **No plagiarism-detection engine exists** -- the assessment config has a `plagiarismDetection` toggle (matches the wizard) and the submission drawer shows a plagiarism badge (matches the mockup), but there's no real similarity-detection algorithm behind it. The badge is a static `"No plagiarism detected"` placeholder, flagged here rather than faked convincingly.
- **`assessment_eligibility` exists because Application Service doesn't consume `CandidateRecommended` itself** (a gap ai-interview-service's own README already documents) -- this service has to track eligibility locally to know who to invite when an assessment publishes.
- **"Move to next stage" re-publishes `CodingAssessmentCompleted`, doesn't write Application Service's state directly** -- same boundary discipline as ai-interview-service's `recordRecruiterDecision`: this service never reaches into another service's state machine, only re-signals via the same Kafka contract that already drives it.
- **Same hiring-recommendation vocabulary as the rest of the platform** (`PROCEED`/`HOLD`/`REJECT`) is consumed (not produced) here, straight from `CandidateRecommendedEvent`.

## 12. Testing

21 unit tests (JUnit 5 + Mockito + AssertJ): the assessment service's draft/publish/clone lifecycle rules and company-scoping, the candidate assessment session's state machine (accept-rules/start/finish conflict rules, time-used computation, anti-cheat logging), the code execution service's severity-ordering and proportional-scoring logic (including the compile-error early-stop), and both provider factories' case-insensitive selection and unknown-provider failure. All passing.

A Testcontainers MySQL integration test, a mocked-HTTP test per AI/execution provider, and a real Judge0 integration test are the natural next step, same documented gap as every other service in this platform (no reliably reachable local Docker daemon during this build).

## 13. Docker & Local Run

```
docker compose up --build
```
Spins up codingassessment-db (3314), redis (6387), kafka (9100), eureka (8769), coding-assessment-service (8090). **Does not** spin up Judge0 -- see below.

**Code Execution Backend (Judge0)**: this compose file deliberately does not bundle Judge0. Judge0 CE's own official stack needs privileged cgroup/isolate access for real sandboxed execution, and faking a minimal version inside this file would produce something that looks like it works but doesn't actually sandbox anything -- worse than not including it. Run Judge0's own official `docker-compose.yml` (https://github.com/judge0/judge0) separately, then point this service at it:
```
JUDGE0_BASE_URL=http://localhost:2358
```
Alternatively, use the free-tier hosted Judge0 via RapidAPI and set both `JUDGE0_BASE_URL` and `JUDGE0_API_KEY`.

**Running alongside the other services**: each service's `docker-compose.yml` defines its own project-scoped `cadence-net` network by default. Start every stack under the same Compose project name so they land on the same actual network:
```
docker compose -p cadence -f ../job-service/docker-compose.yml up -d
docker compose -p cadence -f ../application-service/docker-compose.yml up -d
docker compose -p cadence -f ../ai-interview-service/docker-compose.yml up -d
docker compose -p cadence -f docker-compose.yml up -d
```
Without Docker: local MySQL/Redis/Kafka reachable per `application.yml`, a Judge0 instance reachable at `JUDGE0_BASE_URL`, then `mvn spring-boot:run`. Swagger UI at `http://localhost:8090/swagger-ui.html`.

To use Ollama locally instead of Gemini/Groq for AI code review: set `CODING_ASSESSMENT_AI_PROVIDER=ollama`, run `ollama serve` with a model pulled, and leave `OLLAMA_BASE_URL` at its default.

## 14. Senior Architect Review -- Improvement Areas

- **Scalability**: Judge0 calls happen synchronously per test case during Submit -- for questions with many hidden test cases this is the natural next bottleneck; Judge0 itself supports async submission (`wait=false` + polling or webhooks), which this service could switch to without changing its own API contract.
- **Security**: candidate code never runs inside this service -- it's always delegated to Judge0's isolated sandboxing. Every recruiter-facing endpoint is role-gated (destructive/decision actions exclude `HIRING_MANAGER`); candidate endpoints are gated to `CANDIDATE`; the one trusted-network endpoint is scoped to an explicit `permitAll` allowlist by exact path.
- **Maintainability**: both the execution and AI-review providers are entirely swappable via one config property each; adding a second sandbox backend or a fourth AI provider means one new `@Component`, not touching either factory or any orchestration service.
- **Next real gaps**: (1) a real plagiarism-detection engine, currently a static placeholder. (2) Application Service needs a consumer wired for `CandidateRecommended` on its own side so this service doesn't have to track eligibility independently. (3) a Notification Service needs to exist before `sendReminder`/candidate invitations can do anything beyond logging.
