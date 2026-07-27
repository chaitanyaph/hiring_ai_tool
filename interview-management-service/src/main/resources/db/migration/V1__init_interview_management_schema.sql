-- =====================================================================
-- Cadence Interview Management Service - interview_management_db (MySQL 8).
-- company_id/job_id/application_id/candidate_id/interviewer_id are
-- plain CHAR(36) references into other services' own databases (or
-- Auth Service's user table for interviewer_id) -- never a foreign key
-- across service boundaries, per the platform's database-per-service
-- rule.
--
-- Consolidated from the suggested 11 tables down to 6:
--   - interview_schedule + meeting_details folded into interview
--     (reschedule updates the row in place; interview_activity_log
--     carries the historical trail, same pattern as candidate_assessment
--     in coding-assessment-service).
--   - interview_status folded into interview.status (enum column, no
--     separate lookup table needed).
--   - interview_panel + interviewer_assignment merged into the single
--     interview_panelist join table (no reusable named-panel concept
--     exists in the Figma -- panel picker is a flat name list).
--   - feedback_score folded into interview_feedback (one row holding
--     all score columns, same precedent as every prior service).
-- =====================================================================

CREATE TABLE interview_round (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id        CHAR(36)     NOT NULL,
    name              VARCHAR(150) NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    round_order       INT          NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    version           BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_interview_round_company_id ON interview_round(company_id);

CREATE TABLE interview (
    id                          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id                  CHAR(36)     NOT NULL,
    job_id                      CHAR(36)     NOT NULL,
    application_id              CHAR(36)     NOT NULL,
    candidate_id                CHAR(36)     NOT NULL,
    interview_round_id          CHAR(36),
    round_type                  VARCHAR(20)  NOT NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date               DATE         NOT NULL,
    scheduled_time               TIME         NOT NULL,
    duration_minutes             INT          NOT NULL DEFAULT 60,
    mode                        VARCHAR(20)  NOT NULL DEFAULT 'ONLINE',
    meeting_link                 VARCHAR(500),
    auto_generate_meet_link       BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_candidate_by_email      BOOLEAN      NOT NULL DEFAULT TRUE,
    notes_for_panel               TEXT,
    created_by_recruiter_id        CHAR(36),
    cancel_reason                 VARCHAR(500),
    reschedule_reason              VARCHAR(500),
    completed_at                 DATETIME,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_interview_company_id ON interview(company_id);
CREATE INDEX idx_interview_application_id ON interview(application_id);
CREATE INDEX idx_interview_candidate_id ON interview(candidate_id);
CREATE INDEX idx_interview_status ON interview(status);
CREATE INDEX idx_interview_scheduled_date ON interview(scheduled_date);

CREATE TABLE interview_panelist (
    id                    CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    interview_id          CHAR(36)     NOT NULL,
    interviewer_id        CHAR(36)     NOT NULL,
    interviewer_role      VARCHAR(30),
    invited_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    feedback_submitted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_interview_panelist UNIQUE (interview_id, interviewer_id),
    CONSTRAINT fk_interview_panelist_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_panelist_interview_id ON interview_panelist(interview_id);
CREATE INDEX idx_interview_panelist_interviewer_id ON interview_panelist(interviewer_id);

CREATE TABLE interview_feedback (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    interview_id              CHAR(36)     NOT NULL,
    interviewer_id             CHAR(36)     NOT NULL,
    communication_score         INT,
    technical_score             INT,
    culture_fit_score            INT,
    coding_skills_score          INT,
    problem_solving_score        INT,
    system_design_score          INT,
    leadership_score             INT,
    overall_rating              INT,
    strengths                  TEXT,
    weaknesses                 TEXT,
    comments                   TEXT,
    recommendation             VARCHAR(20)  NOT NULL,
    submitted_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_interview_feedback UNIQUE (interview_id, interviewer_id),
    CONSTRAINT fk_interview_feedback_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_feedback_interview_id ON interview_feedback(interview_id);

CREATE TABLE candidate_timeline (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id    CHAR(36)     NOT NULL,
    candidate_id      CHAR(36)     NOT NULL,
    stage             VARCHAR(30)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    occurred_at       DATETIME,
    score             INT,
    note              VARCHAR(500),
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candidate_timeline UNIQUE (application_id, stage)
);
CREATE INDEX idx_candidate_timeline_application_id ON candidate_timeline(application_id);
CREATE INDEX idx_candidate_timeline_candidate_id ON candidate_timeline(candidate_id);

CREATE TABLE interview_activity_log (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    interview_id      CHAR(36)     NOT NULL,
    event_type        VARCHAR(30)  NOT NULL,
    actor_id          CHAR(36),
    occurred_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details           VARCHAR(1000),
    CONSTRAINT fk_interview_activity_log_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_activity_log_interview_id ON interview_activity_log(interview_id);
