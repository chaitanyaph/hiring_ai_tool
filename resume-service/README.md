# Cadence Resume Service

Owns the complete lifecycle of a candidate's resume files: upload, storage, retrieval, metadata, default-resume management, and access control. It does **not** parse resumes, score/match them, own the candidate profile, or manage job applications -- those are Candidate Service, the future Resume Parser/Matching services, and Application Service respectively. This service only stores the file and answers "does this resume exist, is it valid, who may see it."

## 1. Functional Requirements

- Upload (max 3 ACTIVE resumes per candidate, PDF only, max 5MB, content-verified not just extension-trusted, unique checksum per candidate).
- List/detail/rename/delete a candidate's own resumes; set which one is the default.
- Download/preview (candidate: their own; recruiter: only for a candidate who applied to their company; admin: unrestricted).
- Internal metadata + raw MinIO object-location lookups for the future Resume Parser Service.
- Publish 3 Kafka events on upload/delete/default-change; consume `CandidateDeleted` to archive a departed candidate's resumes.

## 2. Non-Functional Requirements

- **Content-verified validation**: file type is checked by magic bytes (`%PDF-`), not just the client-supplied filename/Content-Type, which either can be spoofed.
- **Integrity**: every resume is SHA-256 checksummed; duplicate detection is a real per-candidate content comparison, not a filename heuristic.
- **Isolation**: `resume_db` shares nothing with any other service. `candidate_id` is a plain id, validated via Feign at upload time only.
- **Access control**: a recruiter can only preview/download a resume if their company actually has an application from that candidate (verified via Feign to Application Service) -- never "any recruiter, any resume."
- **Resilience**: the `CandidateDeleted` consumer never crashes on a malformed event; MinIO/DB failures surface as a clear 500 (`STORAGE_ERROR`), never a silent partial write.

## 3. System Workflow

```
Candidate uploads --> JWT validated --> Candidate Service: exists + ACTIVE?
  --> PDF magic-byte + size validation --> resume count < 3? --> checksum unique?
  --> generate object name --> upload to MinIO --> save metadata row (+ auto-default if first)
  --> publish ResumeUploaded (+ ResumeDefaultChanged if first) --> return resume details
```

Delete: owned + ACTIVE --> Feign to Application Service ("is this resume on a non-terminal application?") --> if yes, reject; if no, mark `DELETED`, auto-promote the next most recent resume to default if the deleted one was it, publish `ResumeDeleted` (+ `ResumeDefaultChanged` if promotion happened).

## 4. Database Design

One table, `resumes` (see the ER diagram above). `status` (ACTIVE/DELETED/ARCHIVED) is the *only* lifecycle field -- there's no separate `is_deleted` flag, since that would just be a second, confusing way to say the same thing. `bucket_name`/`object_name` point to the real file in MinIO; nothing else in this service or database touches file bytes directly.

## 5. Folder Structure

```
src/main/java/com/cadence/resumeservice/
  config/       SecurityConfig, MinioConfig, KafkaProducerConfig, KafkaConsumerConfig, RedisConfig, SwaggerConfig
  controller/   ResumeController, RecruiterResumeController, InternalResumeController
  service/      ResumeService (+ impl/), ResumeContent (streaming transport holder)
  repository/   ResumeRepository
  entity/       BaseAuditEntity, Resume
  dto/          request/ + response/
  mapper/       ResumeMapper (MapStruct)
  security/     JWT validation, CurrentUser, JwtAuthenticationFilter
  validation/   FileValidator (PDF magic bytes, size, MIME)
  exception/    ErrorCode, ResumeServiceException hierarchy, GlobalExceptionHandler
  feign/        CandidateServiceClient, ApplicationServiceClient (+ dto/)
  kafka/        producer/, consumer/
  minio/        MinioStorageService
  util/         ChecksumUtil (SHA-256)
  constants/    ResumeStatus, PlatformRole, KafkaTopics, SecurityConstants
  event/        3 published + 1 consumed event POJOs
```

## 6. Business Rules -- As Implemented

| Rule | Where enforced |
|---|---|
| Max 3 resumes per candidate | `ResumeServiceImpl.upload` via `countByCandidateIdAndStatus` |
| PDF only, real content check | `FileValidator` -- extension + declared MIME + actual `%PDF-` magic bytes |
| Max 5MB | `FileValidator` + `MaxUploadSizeExceededException` handler |
| One default resume | `setDefault` unsets the previous default in the same transaction before setting the new one |
| First upload auto-default | `upload()` checks `activeCount == 0` |
| Unique checksum per candidate | `existsByCandidateIdAndChecksumAndStatus` before any MinIO write |
| Cannot delete a resume on an active application | Feign to Application Service's `isResumeInUse` before soft-deleting |
| Recruiters preview/download only | Separate `RecruiterResumeController`, no upload/rename/delete mapping exists there at all |
| Recruiter scoped to their own company's candidates | Feign to Application Service's `hasApplicationFromCandidateToCompany`; ADMIN bypasses this |

## 7. Architecture Decisions

- **JWT validation only, no issuance** -- same shared HS256 secret as every other service.
- **Object path deviates from the literal spec**: the spec's `/companyId/candidateId/{uuid}.pdf` folder structure doesn't fit this service's actual domain -- a resume belongs to a candidate, not to one company (a candidate applies to many companies with the same resume set). Objects are stored at `/{candidateId}/{uuid}.pdf` instead; documented here rather than silently diverging.
- **Two different meanings of "/internal"**: the product spec puts recruiter preview/download under `/api/v1/internal/resumes/...`. Those are real JWT-authenticated, role-gated human endpoints (`RecruiterResumeController`) -- not the trusted-network machine endpoints (`InternalResumeController`, permitAll) that live at the same path prefix for metadata/object lookups. `SecurityConfig`/`SecurityConstants` distinguish them by exact path, not a blanket wildcard.
- **Soft delete keeps the MinIO object** -- `DELETE` only flips `status` to `DELETED`; the file stays in MinIO for audit/recovery. `CandidateDeleted` similarly only archives rows (`ARCHIVED`) rather than purging storage -- this service doesn't own a data-retention/GDPR-erasure policy, so physical erasure is a deliberate, documented follow-up for whenever that policy exists, not fabricated here.
- **The `/object` internal endpoint** exists so the future Resume Parser Service can read a file straight out of MinIO with its own credentials at parsing-pipeline volume, instead of proxying every file through this service's HTTP path.
- **Two small, justified additions to sibling services** were needed to make this service's own Feign calls real rather than aspirational: Candidate Service's `CandidateSummaryResponse` gained a `status` field, and Application Service gained two internal GET queries (`resume/{id}/in-use`, `exists?candidateId=&companyId=`).

## 8. REST API Reference

### Candidate (JWT, role CANDIDATE)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/resumes/upload` | Upload |
| GET | `/api/v1/resumes` | My resumes |
| GET | `/api/v1/resumes/{resumeId}` | Detail |
| GET | `/api/v1/resumes/{resumeId}/download` | Download |
| GET | `/api/v1/resumes/{resumeId}/preview` | Preview (inline) |
| PUT | `/api/v1/resumes/{resumeId}/default` | Set default |
| PUT | `/api/v1/resumes/{resumeId}/rename` | Rename |
| DELETE | `/api/v1/resumes/{resumeId}` | Delete |

### Recruiter (JWT, recruiting role or ADMIN -- company-scoped unless ADMIN)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/resumes/{resumeId}/download` |
| GET | `/api/v1/internal/resumes/{resumeId}/preview` |

### Internal (trusted network, no auth)
| Method | Path |
|---|---|
| GET | `/api/v1/internal/resumes/{resumeId}` |
| GET | `/api/v1/internal/resumes/{resumeId}/object` |

## 9. Kafka Events

**Published**: `resume.resume.uploaded`, `resume.resume.deleted`, `resume.default.changed`.
**Consumed**: `candidate.profile.deleted` (forward-looking -- Candidate Service doesn't publish this yet, same documented pattern as the platform's other cross-service event scaffolding).

## 10. Testing

12 unit tests (JUnit 5 + Mockito + AssertJ + `MockMultipartFile`): candidate-not-active/limit-exceeded/duplicate-checksum upload guards, first-upload auto-default vs. subsequent uploads not auto-defaulting, default-swap unsetting the previous default, delete's in-use guard and default-promotion on delete, the recruiter company-scoping guard and its ADMIN bypass, cross-candidate 404, and rename. All passing.

A Testcontainers MySQL + MinIO integration test is the natural next step, same documented gap as every other service in this platform (no reliably reachable local Docker daemon during this build).

## 11. Docker & Local Run

```
docker compose up --build
```

Spins up resume-db (3311), MinIO (9000 API / 9001 console), redis (6384), kafka (9097), eureka (8766), resume-service (8087).

Without Docker: local MySQL/Redis/Kafka/MinIO reachable per `application.yml`, then `mvn spring-boot:run`. Swagger UI at `http://localhost:8087/swagger-ui.html`.

## 12. Senior Architect Review -- Improvement Areas

- **Scalability**: resume lists are Redis-cached per candidate (5 min TTL) and evicted on every write; MinIO scales independently of the metadata database, so file storage growth never pressures MySQL.
- **Security**: every candidate-facing query is scoped server-side to the JWT's own `userId`; every recruiter-facing preview/download additionally requires a real Feign-verified application relationship, not just "any authenticated recruiter."
- **Maintainability**: `MinioStorageService` is the only class that imports the MinIO SDK -- swapping to S3/GCS later touches one file. `status` being the sole lifecycle field (no parallel soft-delete flag) means there is exactly one thing to check when asking "is this resume usable."
- **Next real gap**: this service assumes a Resume Parser Service will eventually call the internal metadata/object endpoints -- until it exists, `ResumeUploaded` events have no consumer, same interim state the other services' forward-looking event scaffolding is already in.
