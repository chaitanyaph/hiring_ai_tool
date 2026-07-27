# Cadence AI Interview Service

Owns the complete AI Screening process: **AI Shortlisting** (turning a resume match score into a shortlist/reject/manual-review decision), **AI Interview** (running the dynamic chat/voice/video interview), and **AI Evaluation** (scoring the transcript into a hiring recommendation) -- using free LLMs (Gemini, Groq, or a local Ollama model, selected purely by configuration). It does not parse or store resumes (Resume Service, Resume Parser Service), manage jobs (Job Service), manage candidate profiles (Candidate Service), manage applications (Application Service), run coding assessments, or send emails (a future Notification Service) -- those stay in their owning services.

## 1. Service Responsibility

Three modules, one service, one database (`ai_interview_db`):

- **AI Shortlisting** -- consumes `ResumeAnalyzed`, applies company threshold rules, produces a SHORTLISTED/REJECTED/MANUAL_REVIEW decision with a reason, and supports the manual-review queue's single/bulk recruiter actions.
- **AI Interview** -- recruiter-triggered invitation, candidate-driven turn-based chat/voice/video interview with dynamically generated questions (resume + job aware), transcript capture.
- **AI Evaluation** -- async post-completion scoring: communication/confidence/technical/problem-solving/grammar/behavior/leadership/domain-knowledge scores, strengths/weaknesses/improvement areas, hiring recommendation, and recruiter/candidate-facing summaries.

## 2. Complete Workflow

```
Resume Parser Service publishes ResumeAnalyzed (resume-parser.resume.analyzed)
  --> this service consumes it, fetches the full match via Feign
  --> applies threshold rules (>=70 shortlist / <60 reject / 60-69 manual review)
  --> persists candidate_shortlist, publishes CandidateShortlisted
  --> [manual-review rows: recruiter bulk/single Shortlist/Reject/Assign recruiter]
  --> recruiter dashboard shows Top Candidates (shortlisted rows ordered by score)
  --> recruiter clicks "Start AI interview" --> interview_session created (NOT_STARTED),
      candidate receives the invitation
  --> candidate opens the interview, picks mode (chat/voice/video), clicks Start
  --> status=IN_PROGRESS --> publish InterviewStarted --> AI generates question 1..N
      (dynamic, resume+job aware) --> candidate answers each in turn
  --> candidate finishes (or answers the last question) --> status=COMPLETED
      --> publish InterviewCompleted --> async evaluation kicks off
  --> AI evaluates the full transcript --> scores + strengths/weaknesses + recommendation
  --> persist interview_score + interview_recommendation + interview_feedback_note
  --> Feign PUT application-service interview-score
  --> publish InterviewEvaluated, publish CandidateRecommended
  --> recruiter opens the evaluation report --> Move to Coding Assessment / Reject / Manual review
```

## 3. AI Shortlisting Flow

```
ResumeAnalyzed{applicationId, jobId, candidateId, resumeMatchId, overallMatchScore}
  --> Feign --> Resume Parser Service: GET /api/v1/internal/resume-analysis/{applicationId}
  --> decision = score>=70 ? SHORTLISTED : score<60 ? REJECTED : MANUAL_REVIEW
  --> reason: SHORTLISTED -> top matched skills; REJECTED -> top missing required skills;
              MANUAL_REVIEW -> "borderline match score -- needs recruiter judgment"
  --> upsert candidate_shortlist (one row per applicationId, "recalculate replaces" precedent)
```
Manual-review queue supports the mockup's bulk bar (`Shortlist selected` / `Reject selected` / `Assign recruiter`) plus per-row `Shortlist`/`Reject`.

## 4. AI Interview Flow

```
interview_session (mode: CHAT|VOICE|VIDEO, status: NOT_STARTED|IN_PROGRESS|COMPLETED|EXPIRED)
  created/refreshed by the recruiter's "Start AI interview" action (POST /{applicationId}/start)
  --> candidate GET /details (mode options, question count, estimated duration)
  --> candidate POST /start (mode) --> status=IN_PROGRESS, question 1 generated synchronously
       (the candidate is actively waiting for it in the same request)
  --> loop: candidate POST /answer --> next question generated synchronously, or
      interviewCompleted=true once the last question is answered
  --> candidate POST /finish (early end), OR the last answer naturally completes it,
      OR the invitation's TTL expires before it's started
  --> status=COMPLETED/EXPIRED
```
Question categories (fixed enum): `INTRODUCTION, RESUME, JAVA, SPRING_BOOT, MICROSERVICES, SYSTEM_DESIGN, SQL, BEHAVIORAL, HR, SCENARIO_BASED`. The default 8-question interview uses a fixed 8-slot sequence (`INTRODUCTION, RESUME, JAVA, SPRING_BOOT, MICROSERVICES, SYSTEM_DESIGN, SQL, BEHAVIORAL`) -- HR and SCENARIO_BASED stay available in the enum for a future higher question count. Question *text* is dynamically generated per candidate/job by the active AI provider; the *category sequence* is fixed and deterministic.

## 5. AI Evaluation Flow

```
status=COMPLETED --> assemble transcript (interview_question + interview_answer, ordered)
  --> AIInterviewProvider.evaluateInterview(transcript, resumeSnapshot, jobSnapshot)
  --> structured result: 8 core scores + 4 behaviour metrics + strengths/weaknesses/
      improvementAreas + hiringRecommendation(PROCEED|HOLD|REJECT) + 2 summaries
  --> persist interview_score + interview_recommendation (1:1) + interview_feedback_note (ordered)
  --> Feign PUT application-service interview-score (overallScore, source="ai-interview-service")
  --> publish InterviewEvaluated, CandidateRecommended
```
This runs **asynchronously** -- the Figma completion screen literally says "you'll see your status update within a few hours" -- so `finishInterview`/the last `submitAnswer` return immediately and `InterviewEvaluationService.evaluate()` runs off the calling thread. Unlike Resume Parser Service, this doesn't need a separate pipeline-runner bean: `evaluate()` is always invoked from a *different* Spring bean (`InterviewSessionServiceImpl`), so `@Async` on `InterviewEvaluationServiceImpl` itself is enough -- no self-invocation pitfall to work around.

## 6. Sequence Diagram (text)

```
Candidate        AI Interview Svc         Resume Parser Svc   Job Svc   Application Svc   Kafka
   |                    |<--ResumeAnalyzed------------------------------------------------->|
   |                    |--fetch match------->|                |             |             |
   |                    |--persist shortlist, publish CandidateShortlisted---------------->|
   |--GET details------->|                     |                |             |             |
   |<--mode options------|                     |                |             |             |
   |--POST start--------->|--publish InterviewStarted------------------------------------->|
   |<--question 1---------|                     |                |             |             |
   |--POST answer--------->|--persist answer, generate next question           |             |
   |<--question 2..N-------|                     |                |             |             |
   |--POST answer (last)-->|--status=COMPLETED, publish InterviewCompleted----------------->|
   |                       |--async evaluate()->|--fetch resume/job context---->|             |
   |                       |                     |--PUT interview-score--------->|             |
   |                       |--publish InterviewEvaluated, CandidateRecommended-------------->|
   |--GET result----------->|                     |                |             |             |
```

## 7. Kafka Flow

**Consumed**: `resume-parser.resume.analyzed` (Resume Parser Service) -- triggers AI Shortlisting.
**Published**: `ai-interview.candidate.shortlisted`, `ai-interview.interview.started`, `ai-interview.interview.completed`, `ai-interview.interview.evaluated`, `ai-interview.candidate.recommended`.

## 8. Database Design -- `ai_interview_db`

8 tables (consolidated from a larger suggested list -- see Architecture Decisions):

| Table | Purpose |
|---|---|
| `candidate_shortlist` | root, one row/`application_id`: score snapshot, decision, reason, assigned recruiter |
| `interview_session` | root, one row/`application_id`: mode, status, timing, question/answer counts |
| `interview_question` | ordered, per session: category, question text |
| `interview_answer` | per question (1:1): answer text, response time |
| `interview_score` | 1:1 with session: 8 core scores + 4 behaviour metrics |
| `interview_recommendation` | 1:1 with session: hiring call + 2 summaries |
| `interview_feedback_note` | ordered, per session: STRENGTH/WEAKNESS/IMPROVEMENT |
| `interview_log` | append-only audit trail |

## 9. ER Diagram (text)

```
candidate_shortlist (application_id UNIQUE) -- 1:1 -- interview_session (application_id UNIQUE)
interview_session --1:N--> interview_question --1:1--> interview_answer
interview_session --1:1--> interview_score
interview_session --1:1--> interview_recommendation
interview_session --1:N--> interview_feedback_note
interview_session --1:N--> interview_log
```
All FKs are plain UUID columns (`application_id`, `job_id`, `candidate_id`, `resume_match_id`) -- never a cross-service or cross-table JPA relationship, same database-per-service style as every sibling service.

## 10. API Design mapped to Figma

Reconciled against `#sec-shortlisting`, `#sec-ai-interviews`, `#interview-view`, and `#csec-interviews` in the mockup -- no extra, none missing.

### Recruiter -- AI Shortlisting (`#sec-shortlisting`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/ai-interviews/shortlisted/{jobId}` | Shortlisted tab |
| GET | `/api/v1/ai-interviews/rejected/{jobId}` | Rejected tab |
| GET | `/api/v1/ai-interviews/manual-review/{jobId}` | Manual review tab |
| GET | `/api/v1/ai-interviews/shortlisting/summary/{jobId}` | KPI row |
| GET | `/api/v1/ai-interviews/ranking/{jobId}` | Top Candidates |
| POST | `/api/v1/ai-interviews/{applicationId}/shortlist` | Manual review row action |
| POST | `/api/v1/ai-interviews/{applicationId}/reject-manual` | Manual review row action |
| POST | `/api/v1/ai-interviews/manual-review/bulk-shortlist` | Bulk bar |
| POST | `/api/v1/ai-interviews/manual-review/bulk-reject` | Bulk bar |
| POST | `/api/v1/ai-interviews/manual-review/assign-recruiter` | Bulk bar |

### Recruiter -- AI Interviews (`#sec-ai-interviews`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/ai-interviews/queue/{jobId}` | Interview queue table |
| GET | `/api/v1/ai-interviews/analysis-summary/{jobId}` | Analysis dashboard KPI row |
| GET | `/api/v1/ai-interviews/completed/{jobId}` | Completed interviews table |
| POST | `/api/v1/ai-interviews/{applicationId}/start` | Sends the interview invitation |
| POST | `/api/v1/ai-interviews/{applicationId}/send-reminder` | Not-started row action |
| POST | `/api/v1/ai-interviews/{applicationId}/resend-invite` | Expired row action |
| GET | `/api/v1/ai-interviews/{applicationId}/report` | Evaluation report drawer |
| POST | `/api/v1/ai-interviews/{applicationId}/move-to-coding` | Report drawer decision |
| POST | `/api/v1/ai-interviews/{applicationId}/reject` | Report drawer decision |
| POST | `/api/v1/ai-interviews/{applicationId}/manual-review` | Report drawer decision |

### Candidate (`#interview-view`, `#csec-interviews`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/candidate/interview/details` | Intro/setup screen |
| POST | `/api/v1/candidate/interview/start` | Start + first question |
| POST | `/api/v1/candidate/interview/answer` | Submit answer + next question |
| POST | `/api/v1/candidate/interview/finish` | Early end |
| GET | `/api/v1/candidate/interview/result` | Completion / transcript screen |

### Internal (trusted network, no auth)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/ai-interviews/{applicationId}` |

Deliberately **not** built: a dedicated "permissions" (mic/camera consent) endpoint -- the mockup folds this into the intro screen's mode-selection cards, with no separate backend-relevant consent step (unlike Coding Assessment's proctoring rules screen, which is out of this service's scope).

## 11. Folder Structure

```
src/main/java/com/cadence/aiinterviewservice/
  constants/      KafkaTopics, SecurityConstants, PlatformRole, ShortlistDecision, InterviewSessionStatus,
                  InterviewMode, QuestionCategory, HiringRecommendation, NoteType, LogLevel, AiProvider
  entity/         BaseAuditEntity, CandidateShortlist, InterviewSession (+6 simple child entities)
  repository/
  security/       (copied verbatim from Resume Parser Service)
  config/         SecurityConfig, KafkaProducerConfig, KafkaConsumerConfig, SwaggerConfig
  exception/      AiInterviewServiceException hierarchy, ErrorCode, GlobalExceptionHandler
  dto/request/    StartInterviewRequest, AnswerRequest, BulkApplicationIdsRequest, AssignRecruiterRequest
  dto/response/   Shortlist/queue/report/session responses + PagedResponse/ApiResponse
  mapper/         ShortlistMapper, InterviewMapper (MapStruct)
  feign/          CandidateServiceClient, JobServiceClient, ApplicationServiceClient, ResumeParserServiceClient
  kafka/          event/, producer/, consumer/
  provider/       AIInterviewProvider + Abstract + Gemini/Groq/Ollama impls + question/evaluation records
  strategy/       AIInterviewProviderFactory (the Strategy *context*)
  service/, service/impl/
      ShortlistingService, ShortlistQueryService
      InterviewSessionService, InterviewEvaluationService, InterviewQueryService
  controller/     ShortlistingController, InterviewController, CandidateInterviewController,
                  InternalAiInterviewController
```

## 12. Architecture Decisions

- **Consolidated the suggested table list**: `candidate_ranking` was dropped -- it's just `candidate_shortlist` ordered by score, not new information. `technical_score`/`communication_score`/`behavior_score` as three separate tables became columns on one `interview_score` row, mirroring how `resume_match` already holds many score columns on one row rather than sibling tables. `ai_feedback` became two things: `interview_recommendation` (1:1, summary + hiring call) and `interview_feedback_note` (ordered STRENGTH/WEAKNESS/IMPROVEMENT rows), mirroring Resume Parser Service's `ai_recommendation` + `resume_match_note` split exactly.
- **No `interview_transcript` table** -- the transcript is assembled from `interview_question` ⋈ `interview_answer` in order, not stored a third time.
- **Question generation is synchronous, evaluation is asynchronous** -- the candidate is actively waiting for the next question in the same request/response cycle, but the Figma's own completion screen says evaluation takes "a few hours," so only evaluation gets the `@Async` treatment.
- **One `AIInterviewProvider` interface, not split by capability** -- `generateNextQuestion` and `evaluateInterview` both belong on the same interface (unlike Resume Parser Service's matching extension, which had to extend an *existing* single-purpose interface after the fact); this service was built fresh with both capabilities from day one, exactly as named in the request (`AIInterviewProvider`/`GeminiInterviewProvider`/`GroqInterviewProvider`/`OllamaInterviewProvider`).
- **`{applicationId}` scoping throughout recruiter/internal endpoints, not `{candidateId}`** -- a candidate can apply to multiple jobs, each with its own shortlist decision and interview; `applicationId` is the correct scoping key, same convention Resume Parser Service's matching extension already established.
- **Real gap found, not fixed silently**: Application Service's `InternalApplicationController` has no "advance stage" endpoint -- its 3 score-update PUTs deliberately never drive a status transition (per that controller's own Javadoc). So `AI_INTERVIEW_PENDING -> AI_INTERVIEW_COMPLETED` and the recruiter's "Move to Coding Assessment"/"Reject" actions cannot directly write Application Service's state machine from here. This service publishes `InterviewEvaluated`/`CandidateRecommended` (and locally records the recruiter's override on `interview_recommendation`); wiring an actual consumer on Application Service's side is flagged as a follow-up requiring separate authorization, not applied here.
- **No Feign client to a Notification Service** -- the spec asked for one, but no such service exists among the 7 built so far. `sendReminder`/`resendInvite` validate state and log for audit purposes only; actual candidate notification is future work once that service exists.
- **`recordRecruiterDecision` overloads the hiring recommendation, doesn't gate on Application Service** -- "Move to Coding" (`PROCEED`), "Reject" (`REJECT`), and "Manual review" (`HOLD`) all just update `interview_recommendation.hiringRecommendation` and re-publish `CandidateRecommended`; the actual pipeline-stage transition is Application Service's responsibility once it wires a consumer (see the gap above).
- **Same hiring-recommendation vocabulary as Resume Parser Service** (`PROCEED`/`HOLD`/`REJECT`) -- kept consistent across every AI-recommendation surface in the platform.
- **Fixed 8-slot question category sequence**, not AI-chosen categories -- keeps the *coverage* (intro, resume, 4 technical areas, behavioral) deterministic and predictable, while letting the AI provider handle the *creative* part (actual question wording tailored to the specific resume and job).
- **`CandidateResumeSnapshot.experienceSummaries` is always empty** -- `ResumeMatchResponse` (the only resume data this service pulls via Feign) doesn't expose per-role experience descriptions, only matched/missing skills and a professional summary; pulling full parsed-resume data would require a second Feign hop to Resume Parser Service's separate parsed-resume endpoint for marginal benefit, so this was accepted as a reasonable simplification rather than added complexity.

## 13. Testing

23 unit tests (JUnit 5 + Mockito + AssertJ): the shortlisting service's threshold-decision/re-analysis/bulk-action paths, the interview session service's state machine (invite/start/answer/finish/expiry/conflict rules) and its hand-off into the evaluation pipeline, the provider factory's case-insensitive selection and unknown-provider failure, and the shortlist query service's KPI-rate aggregation and ranking enrichment. All passing.

A Testcontainers MySQL integration test and a mocked-HTTP test per AI provider are the natural next step, same documented gap as every other service in this platform (no reliably reachable local Docker daemon during this build).

## 14. Docker & Local Run

```
docker compose up --build
```
Spins up aiinterview-db (3313), redis (6386), kafka (9099), eureka (8768), ai-interview-service (8089).

**Running alongside the other services** (needed for real cross-service Feign calls, not just a demo): each service's `docker-compose.yml` defines its own project-scoped `cadence-net` network by default, so independently-run stacks do not share a network out of the box. Start every stack under the same Compose project name so they land on the same actual network:
```
docker compose -p cadence -f ../job-service/docker-compose.yml up -d
docker compose -p cadence -f ../application-service/docker-compose.yml up -d
docker compose -p cadence -f ../resume-parser-service/docker-compose.yml up -d
docker compose -p cadence -f docker-compose.yml up -d
```
Without Docker: local MySQL/Redis/Kafka reachable per `application.yml`, then `mvn spring-boot:run`. Swagger UI at `http://localhost:8089/swagger-ui.html`.

To use Ollama locally instead of Gemini/Groq: set `AI_INTERVIEW_PROVIDER=ollama`, run `ollama serve` with a model pulled (e.g. `ollama pull llama3.1`), and leave `OLLAMA_BASE_URL` at its default (`http://localhost:11434` outside Docker, `http://host.docker.internal:11434` inside).

## 15. Senior Architect Review -- Improvement Areas

- **Scalability**: the Kafka consumer group can be scaled horizontally without any code change, since shortlisting decisions are idempotent upserts keyed by `application_id`.
- **Security**: every recruiter-facing endpoint is role-gated (decision actions exclude `HIRING_MANAGER`, the platform's view-only role); candidate endpoints are gated to `CANDIDATE`; the one trusted-network endpoint is scoped to an explicit `permitAll` allowlist by exact path.
- **Maintainability**: the AI provider is entirely swappable via one config property; question generation and evaluation share one prompt-building/JSON-extraction base class, so adding a fourth provider means one new `@Component`, not touching the factory or the orchestration services.
- **Next real gaps**: (1) Application Service needs a consumer for `InterviewEvaluated`/`CandidateRecommended` to actually drive its own stage transitions -- flagged, not applied here. (2) A Notification Service needs to exist before `sendReminder`/candidate invitations can do anything beyond logging.
