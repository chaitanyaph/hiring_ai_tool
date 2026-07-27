-- =====================================================================
-- Cadence Application Service - application_db schema (MySQL 8)
-- Owns: the application lifecycle only. company_id/job_id/candidate_id/
-- resume_id/assigned_recruiter_id/assigned_hiring_manager_id are plain
-- CHAR(36) references to other services' data -- never a foreign key
-- across service boundaries, per the platform's database-per-service rule.
-- =====================================================================

CREATE TABLE applications (
    id                          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id                  CHAR(36)     NOT NULL,
    job_id                      CHAR(36)     NOT NULL,
    candidate_id                CHAR(36)     NOT NULL,
    resume_id                   CHAR(36),
    assigned_recruiter_id       CHAR(36),
    assigned_hiring_manager_id  CHAR(36),
    candidate_name_snapshot     VARCHAR(150),
    candidate_email_snapshot    VARCHAR(150),
    job_title_snapshot          VARCHAR(150),
    current_status              VARCHAR(30)  NOT NULL DEFAULT 'APPLIED',
    current_stage               VARCHAR(30)  NOT NULL DEFAULT 'APPLICATION',
    resume_match_score          INT,
    ai_interview_score          INT,
    coding_score                INT,
    overall_score               INT,
    priority                    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    remarks                     VARCHAR(1000),
    applied_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_status_changed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at                  DATETIME,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_applications_candidate_job UNIQUE (candidate_id, job_id)
);
CREATE INDEX idx_applications_company_id ON applications(company_id);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_candidate_id ON applications(candidate_id);
CREATE INDEX idx_applications_status ON applications(current_status);
CREATE INDEX idx_applications_stage ON applications(current_stage);
CREATE INDEX idx_applications_recruiter ON applications(assigned_recruiter_id);
CREATE INDEX idx_applications_hiring_manager ON applications(assigned_hiring_manager_id);
CREATE INDEX idx_applications_priority ON applications(priority);

CREATE TABLE application_status_history (
    id              CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)    NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      CHAR(36),
    changed_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason          VARCHAR(500),
    CONSTRAINT fk_app_status_history_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_status_history_application_id ON application_status_history(application_id);

CREATE TABLE application_stage_history (
    id              CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)    NOT NULL,
    from_stage      VARCHAR(30),
    to_stage        VARCHAR(30) NOT NULL,
    changed_by      CHAR(36),
    changed_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason          VARCHAR(500),
    CONSTRAINT fk_app_stage_history_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_stage_history_application_id ON application_stage_history(application_id);

-- Append-only: every score ever reported, not just the latest (which
-- is denormalized onto applications.* for fast reads/search/sort).
CREATE TABLE application_scores (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)     NOT NULL,
    score_type      VARCHAR(20)  NOT NULL,
    score_value     INT          NOT NULL,
    source          VARCHAR(60),
    recorded_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_scores_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_scores_application_id ON application_scores(application_id);
CREATE INDEX idx_app_scores_type ON application_scores(score_type);

CREATE TABLE application_notes (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)     NOT NULL,
    author_id       CHAR(36)     NOT NULL,
    note            VARCHAR(2000) NOT NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_notes_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_notes_application_id ON application_notes(application_id);

-- Full audit trail of every Kafka event published or consumed for an
-- application -- separate from status/stage history, which only track
-- the lifecycle field changes themselves.
CREATE TABLE application_events (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)     NOT NULL,
    event_type      VARCHAR(60)  NOT NULL,
    direction       VARCHAR(10)  NOT NULL,
    payload         LONGTEXT,
    occurred_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_events_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_events_application_id ON application_events(application_id);
CREATE INDEX idx_app_events_type ON application_events(event_type);
