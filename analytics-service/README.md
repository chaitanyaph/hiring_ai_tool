# Analytics Service

Enterprise-wide analytics for the Cadence hiring platform: executive/company/recruiter/hr/hiring-manager dashboards, hiring funnel, per-domain analytics (jobs, candidates, resumes, interviews, assessments, offers), recruiter performance, and daily/monthly/yearly reports with CSV/Excel/PDF export. Consumes Kafka events from all 9 upstream services and never live-queries their databases (database-per-service).

## Architecture: event-sourced materialized view, not live aggregation

Every KPI, funnel-stage count, monthly-series value, and breakdown row is pre-aggregated **at Kafka-ingestion time** into a single generic wide fact table, `metric_snapshot` (see `V1__init_analytics_schema.sql` header). Read endpoints only ever query this pre-aggregated store — no cross-service Feign-call-per-request, no live joins across service boundaries. `application.status.changed` is the funnel backbone: its `toStatus` field alone drives most funnel-stage and KPI counters, avoiding the need to separately consume ~10 granular completion events just for counting.

`*_SUM`/`*_COUNT` metric-key pairs (e.g. `RESUME_SCORE_SUM`/`RESUME_SCORE_COUNT`) make averages an O(1) read (`SUM/COUNT`), never a scan of raw event history.

## Database Design — `analytics_db`

Consolidated from the suggested 12 tables to **4**:

| Table | Purpose |
|---|---|
| `metric_snapshot` | Generic wide fact table. Every KPI/funnel/trend/breakdown value is a differently-keyed row (`scope`, `scope_id`, `metric_key`, `dimension`, `period_type`, `period_date`) → `metric_value`. `scope_id`/`dimension`/`period_date` use sentinel values (`NO_SCOPE_ID = 00000000-0000-0000-0000-000000000000`, `dimension = ""`, `period_date = 1970-01-01`) instead of `NULL`, so a single equality-based upsert lookup always works — MySQL's "`NULL` is never equal to `NULL`" never becomes a bug here. |
| `recruiter_performance_snapshot` | Kept as its own table rather than folded into `metric_snapshot` — the recruiter performance table is queried as multiple recruiters × multiple columns at once. |
| `report_export_log` | Audit trail only, no bytes persisted — reports are regenerable from `metric_snapshot` on demand. |
| `analytics_activity_log` | Ingestion audit trail (source service, event type, related entity). |

## Scope-limitation, flagged

Many granular events (resume/AI-interview/coding-assessment/interview-evaluation scores, all offer-lifecycle events) **do not carry `companyId`** anywhere on their payload — confirmed by reading each event's actual shape upstream. Enriching every single Kafka message with a Feign call to attribute it to a company would be a serious anti-pattern for "scalable analytics aggregation" at high event volume, so these are tracked at `GLOBAL` scope only (see `AnalyticsController`'s `/resumes`, `/interviews`, `/assessments`, `/offers` endpoints — platform-wide, no company parameter). Metrics that *do* carry `companyId` directly (company/job/application volumes, and critically the whole funnel via `ApplicationStatusChangedEvent`) are tracked at both `COMPANY` and `GLOBAL` scope.

One consequence: `CandidateAnalyticsResponse.shortlistedCount` is always platform-wide even on the company-scoped `/candidates` endpoint, since `CandidateShortlistedEvent` carries no `companyId` — flagged in `DomainAnalyticsServiceImpl`, not silently mislabeled as company data.

## Funnel stage ordering

`FunnelServiceImpl` orders stages by ingested volume (largest count first), **not** a hardcoded `ApplicationStatus` enum — this service doesn't own that state machine (it lives in `application-service`) and only ever sees the raw `toStatus` string on the wire. A funnel naturally tapers, so count-descending reconstructs the expected shape without assuming stage names/order that could silently drift out of sync with the owning service.

No job-level funnel is exposed: `FUNNEL_STAGE` is never ingested at `JOB` scope, so a job-filtered funnel (present in the Figma mockup) isn't backed by any ingested data.

## Report/export granularity gap, flagged

`metric_snapshot` only ever writes `ALL_TIME` rows for KPIs/funnel/recruiter-performance, plus `MONTHLY` rows for `HIRES` specifically (company-scoped only, never `GLOBAL`). There is no `DAILY` or `YEARLY` bucketing anywhere in the ingestion pipeline. Consequently:
- `ReportResponse`'s KPI/funnel/recruiter-performance fields are always as-of-generation cumulative snapshots, not sliced to the requested period.
- `hiringTrend` is the one genuinely period-aware field, and only populated for `MONTHLY` company-scoped reports; it's an empty list for `DAILY`, `YEARLY`, and any `GLOBAL`/platform-wide report.

This is documented in `ReportResponse`'s javadoc and is a real gap, not a shortcut taken silently.

## Export formats

- **CSV** — hand-rolled (`CsvExportGenerator`), RFC 4180-aware quoting. No CSV library (`commons-csv`, `opencsv`) is cached in this offline `.m2` repository.
- **Excel** — `ExcelExportGenerator` produces a real **`.xls`** file via `HSSFWorkbook` (Apache POI legacy binary format), **not** `.xlsx`. `poi-ooxml` (true `.xlsx` support) is not cached offline; only base `poi:5.2.2` is. The file extension is `.xls` throughout — never mislabeled as `.xlsx`.
- **PDF** — `PdfReportGenerator` uses OpenPDF 1.3.8 (`com.github.librepdf:openpdf`), the same library and table/paragraph pattern as `offer-management-service`'s `OfferLetterPdfGenerator` — the only PDF library confirmed cached offline (iText unavailable).

## OpenFeign Communication

| Client | Purpose |
|---|---|
| `CompanyServiceClient`, `JobServiceClient`, `CandidateServiceClient`, `ApplicationServiceClient` | Reserved for enrichment (name/title lookups); the pre-aggregated read model does not require live calls per request. |
| `ResumeParserServiceClient`, `CodingAssessmentServiceClient` | Internal endpoints confirmed to exist on those services. |

**Not built:** Interview Management Service and Offer Management Service Feign clients — both services were confirmed (by reading their actual controllers) to have **no internal/unauthenticated endpoint**, so a backend-to-backend call would have no working target. All integration with those two services is via Kafka consumption instead (see Kafka Event Flow).

## Kafka Event Flow

**Consumed**, grouped by source service (9 consumers, `kafka/consumer/`): Company (`CompanyCreated`), Job (`JobPublished`, `JobClosed`), Auth (`UserRegistered`), Application (`ApplicationCreated`, `ApplicationStatusChanged`, `RecruiterAssigned`), Resume Parser (`ResumeParsed`, `ResumeAnalyzed`), AI Interview (`CandidateShortlisted`, `InterviewEvaluated`), Coding Assessment (`CodingAssessmentCompleted`), Interview Management (`InterviewCompleted`, `InterviewCancelled`), Offer Management (`OfferGenerated`, `OfferSent`, `OfferAccepted`, `OfferRejected`, `OfferNegotiationRequested`). No DLQ — matches the simpler try/catch-and-log pattern used by `interview-management-service`/`offer-management-service`, not `notification-service`'s DLQ pattern (not requested for this service).

**Published:** none — this service is a pure sink; it only ever reads from Kafka and serves pre-aggregated data over REST.

## Known gaps, flagged (not fabricated)

- **No diversity/gender field** anywhere in the platform — `diversityRatioPercent` is always `null`, not a fabricated value matching the Figma's static mockup number.
- **No average time-to-hire** — no first-applied→hired timestamp pair is ingested anywhere; `avgTimeToHireDays` is always `null` at both the dashboard and recruiter-performance level.
- **No source/channel field** anywhere in the platform — `sourceBreakdown` is always an empty list.
- **`RecruiterPerformanceSnapshot.hiresCount`/`avgTimeToHireDays`** are always `0`/`null` — `RecruiterAssignedEvent` has no completion linkage back to it (would require either a `recruiterId` field on `ApplicationStatusChangedEvent`, which doesn't exist, or a local `applicationId → recruiterId` mapping table, out of scope).
- **`spring-boot-starter-data-redis`** is present in `pom.xml` as a scaffold leftover — no caching layer was implemented (the pre-aggregated `metric_snapshot` store removes the need for one). `docker-compose.yml` still provisions a `redis` container for parity with sibling services, but nothing in the code uses it.

## Build environment note

This is an offline-only build environment (no network access to Maven Central). All dependencies used here (`com.github.librepdf:openpdf:1.3.8`, `org.apache.poi:poi:5.2.2`) were verified present in the local `.m2` repository before use.

## Running locally

```
docker compose up -d
```

Or standalone: `mvn spring-boot:run` (requires MySQL on `3306`, Redis on `6379`, Kafka on `9092`).
