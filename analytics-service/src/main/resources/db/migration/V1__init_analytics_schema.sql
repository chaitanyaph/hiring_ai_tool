-- =====================================================================
-- Cadence Analytics Service - analytics_db (MySQL 8).
-- company_id/job_id/recruiter_id/candidate_id are plain CHAR(36)
-- references into other services' own databases -- never a foreign
-- key across service boundaries, per the platform's database-per-
-- service rule.
--
-- Consolidated from the suggested 12 tables down to 4 -- more
-- aggressively than any prior service, because a generic wide fact
-- table is the architecturally correct pattern for an analytics
-- platform (this is how real systems like this are built), not
-- consolidation for its own sake:
--   - dashboard_metrics, job_metrics, candidate_metrics, resume_metrics,
--     assessment_metrics, interview_metrics, offer_metrics, kpi_metrics,
--     analytics_snapshot are all folded into metric_snapshot -- every
--     KPI, funnel-stage count, monthly bar-chart value, and source-
--     breakdown row is just a differently-keyed row in one table.
--   - recruiter_metrics kept as its own dedicated table
--     (recruiter_performance_snapshot) since the Figma's "Recruiter
--     performance" table is queried as multiple recruiters x multiple
--     columns at once -- more practical than reconstructing a
--     multi-column row from the generic fact table.
--   - monthly_report/daily_report are NOT separate tables -- a report
--     is just a query over metric_snapshot filtered by period_type/
--     period_date, generated on demand.
--
-- Sentinel values (never NULL) are used so a single equality-based
-- upsert lookup always works, avoiding MySQL's "NULL is never equal
-- to NULL" pitfall in unique constraints:
--   scope_id    = '00000000-0000-0000-0000-000000000000' when scope
--                 has no natural id (e.g. GLOBAL)
--   dimension   = '' when the metric has no secondary grouping key
--   period_date = '1970-01-01' when period_type = 'ALL_TIME'
-- =====================================================================

CREATE TABLE metric_snapshot (
    id                CHAR(36)       PRIMARY KEY DEFAULT (UUID()),
    scope             VARCHAR(20)    NOT NULL,
    scope_id          CHAR(36)       NOT NULL,
    metric_key        VARCHAR(50)    NOT NULL,
    dimension         VARCHAR(100)   NOT NULL DEFAULT '',
    period_type       VARCHAR(20)    NOT NULL DEFAULT 'ALL_TIME',
    period_date       DATE           NOT NULL,
    metric_value      DECIMAL(20,4)  NOT NULL DEFAULT 0,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_metric_snapshot_key UNIQUE (scope, scope_id, metric_key, dimension, period_type, period_date)
);
CREATE INDEX idx_metric_snapshot_scope_id ON metric_snapshot(scope, scope_id);
CREATE INDEX idx_metric_snapshot_metric_key ON metric_snapshot(metric_key);
CREATE INDEX idx_metric_snapshot_period ON metric_snapshot(period_type, period_date);

CREATE TABLE recruiter_performance_snapshot (
    id                       CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    recruiter_id             CHAR(36)      NOT NULL,
    company_id               CHAR(36)      NOT NULL,
    period_date              DATE          NOT NULL,
    open_reqs                INT           NOT NULL DEFAULT 0,
    applications_reviewed     INT           NOT NULL DEFAULT 0,
    hires_count               INT           NOT NULL DEFAULT 0,
    avg_time_to_hire_days      DECIMAL(6,2),
    avg_interview_rating       DECIMAL(4,2),
    avg_offer_acceptance_pct    DECIMAL(5,2),
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recruiter_perf_snapshot UNIQUE (recruiter_id, period_date)
);
CREATE INDEX idx_recruiter_perf_company_id ON recruiter_performance_snapshot(company_id);

CREATE TABLE report_export_log (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    report_type       VARCHAR(30)   NOT NULL,
    format            VARCHAR(10),
    requested_by      CHAR(36),
    company_id        CHAR(36),
    filters           VARCHAR(500),
    file_size_bytes   BIGINT,
    requested_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_report_export_log_company_id ON report_export_log(company_id);
CREATE INDEX idx_report_export_log_requested_at ON report_export_log(requested_at);

CREATE TABLE analytics_activity_log (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    source            VARCHAR(60)   NOT NULL,
    event_type        VARCHAR(60)   NOT NULL,
    message           VARCHAR(1000) NOT NULL,
    related_entity_id CHAR(36),
    occurred_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_analytics_activity_log_occurred_at ON analytics_activity_log(occurred_at);
