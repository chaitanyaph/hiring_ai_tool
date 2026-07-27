-- =====================================================================
-- Cadence Resume Parser Service - resume matching / AI recommendation
-- extension to resume_parser_db (MySQL 8). application_id/job_id are
-- plain CHAR(36) references into Application Service's / Job
-- Service's own databases -- never a foreign key across service
-- boundaries, per the platform's database-per-service rule.
--
-- Only resume_match (the aggregate root, one row per application_id)
-- carries the full audit trail, same precedent as parsed_resume in
-- V1 -- the 4 child tables are system-generated and fully replaced on
-- every (re)analysis.
-- =====================================================================

CREATE TABLE resume_match (
    id                            CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    application_id                CHAR(36)      NOT NULL,
    job_id                         CHAR(36)      NOT NULL,
    department_id                  CHAR(36),
    candidate_id                  CHAR(36)      NOT NULL,
    resume_id                     CHAR(36),
    parsed_resume_id               CHAR(36),
    status                         VARCHAR(20)   NOT NULL DEFAULT 'AWAITING_RESUME',
    attempt_count                  INT           NOT NULL DEFAULT 0,
    provider_used                  VARCHAR(20),
    overall_match_score             INT,
    technical_skill_score           INT,
    programming_language_score      INT,
    framework_score                 INT,
    database_score                  INT,
    cloud_score                     INT,
    devops_score                    INT,
    experience_match_label          VARCHAR(20),
    project_match_score             INT,
    education_match_label           VARCHAR(20),
    certification_match_label       VARCHAR(20),
    communication_prediction        INT,
    problem_solving_prediction      INT,
    learning_ability_prediction     INT,
    failure_reason                  TEXT,
    analyzed_at                     DATETIME,
    created_at                      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      CHAR(36),
    updated_by                      CHAR(36),
    version                         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_resume_match_application_id UNIQUE (application_id)
);
CREATE INDEX idx_resume_match_job_id ON resume_match(job_id);
CREATE INDEX idx_resume_match_department_id ON resume_match(department_id);
CREATE INDEX idx_resume_match_candidate_id ON resume_match(candidate_id);
CREATE INDEX idx_resume_match_status ON resume_match(status);
CREATE INDEX idx_resume_match_resume_id ON resume_match(resume_id);

CREATE TABLE skill_match (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    resume_match_id   CHAR(36)     NOT NULL,
    skill_name        VARCHAR(100) NOT NULL,
    skill_category    VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_skill_match_resume_match FOREIGN KEY (resume_match_id) REFERENCES resume_match(id) ON DELETE CASCADE
);
CREATE INDEX idx_skill_match_resume_match_id ON skill_match(resume_match_id);

CREATE TABLE missing_skill (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    resume_match_id   CHAR(36)     NOT NULL,
    skill_name        VARCHAR(100) NOT NULL,
    skill_category    VARCHAR(30)  NOT NULL,
    is_required       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_missing_skill_resume_match FOREIGN KEY (resume_match_id) REFERENCES resume_match(id) ON DELETE CASCADE
);
CREATE INDEX idx_missing_skill_resume_match_id ON missing_skill(resume_match_id);

CREATE TABLE ai_recommendation (
    id                            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    resume_match_id                CHAR(36)     NOT NULL,
    hiring_recommendation          VARCHAR(20)  NOT NULL,
    overall_ai_summary              TEXT,
    recommended_learning_topics     TEXT,
    CONSTRAINT uq_ai_recommendation_resume_match_id UNIQUE (resume_match_id),
    CONSTRAINT fk_ai_recommendation_resume_match FOREIGN KEY (resume_match_id) REFERENCES resume_match(id) ON DELETE CASCADE
);

CREATE TABLE resume_match_note (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    resume_match_id   CHAR(36)     NOT NULL,
    note_type         VARCHAR(10)  NOT NULL,
    description       VARCHAR(500) NOT NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_resume_match_note_resume_match FOREIGN KEY (resume_match_id) REFERENCES resume_match(id) ON DELETE CASCADE
);
CREATE INDEX idx_resume_match_note_resume_match_id ON resume_match_note(resume_match_id);
