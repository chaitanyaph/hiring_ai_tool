-- =====================================================================
-- Cadence Coding Assessment Service - coding_assessment_db (MySQL 8).
-- company_id/job_id/application_id/candidate_id are plain CHAR(36)
-- references into other services' own databases -- never a foreign
-- key across service boundaries, per the platform's database-per-
-- service rule.
--
-- Only assessment, question, and candidate_assessment (the 3
-- aggregate roots) carry the full audit trail -- every child table is
-- system-generated/append-only and doesn't need one, same precedent
-- as every sibling service's schema.
-- =====================================================================

CREATE TABLE assessment (
    id                          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id                  CHAR(36)     NOT NULL,
    job_id                      CHAR(36)     NOT NULL,
    created_by_recruiter_id     CHAR(36),
    name                        VARCHAR(200) NOT NULL,
    type                        VARCHAR(20)  NOT NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    difficulty                  VARCHAR(20)  NOT NULL,
    duration_minutes            INT          NOT NULL,
    question_count              INT          NOT NULL,
    passing_score_percent       INT          NOT NULL,
    total_marks                 INT          NOT NULL,
    compiler_version             VARCHAR(50),
    allowed_languages            VARCHAR(200) NOT NULL,
    negative_marking             BOOLEAN      NOT NULL DEFAULT FALSE,
    anti_cheat_monitoring        BOOLEAN      NOT NULL DEFAULT TRUE,
    plagiarism_detection         BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date                  DATE,
    expiry_date                 DATE,
    candidate_instructions       TEXT,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_assessment_company_id ON assessment(company_id);
CREATE INDEX idx_assessment_job_id ON assessment(job_id);
CREATE INDEX idx_assessment_status ON assessment(status);

CREATE TABLE question (
    id                          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id                  CHAR(36)     NOT NULL,
    title                       VARCHAR(200) NOT NULL,
    difficulty                  VARCHAR(20)  NOT NULL,
    marks                       INT          NOT NULL,
    description                 TEXT         NOT NULL,
    example_text                 TEXT,
    constraints_text              TEXT,
    tags                        VARCHAR(300),
    allowed_languages            VARCHAR(200) NOT NULL,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_question_company_id ON question(company_id);
CREATE INDEX idx_question_difficulty ON question(difficulty);

CREATE TABLE assessment_question (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    assessment_id     CHAR(36)     NOT NULL,
    question_id       CHAR(36)     NOT NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_assessment_question UNIQUE (assessment_id, question_id),
    CONSTRAINT fk_assessment_question_assessment FOREIGN KEY (assessment_id) REFERENCES assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_assessment_question_assessment_id ON assessment_question(assessment_id);
CREATE INDEX idx_assessment_question_question_id ON assessment_question(question_id);

CREATE TABLE question_starter_code (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    question_id       CHAR(36)     NOT NULL,
    language          VARCHAR(20)  NOT NULL,
    code              TEXT         NOT NULL,
    CONSTRAINT uq_question_starter_code UNIQUE (question_id, language),
    CONSTRAINT fk_question_starter_code_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);

CREATE TABLE question_test_case (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    question_id       CHAR(36)     NOT NULL,
    visibility        VARCHAR(10)  NOT NULL,
    input_data         TEXT,
    expected_output     TEXT         NOT NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_question_test_case_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);
CREATE INDEX idx_question_test_case_question_id ON question_test_case(question_id);

CREATE TABLE assessment_eligibility (
    id                     CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id         CHAR(36)     NOT NULL,
    job_id                 CHAR(36)     NOT NULL,
    candidate_id           CHAR(36)     NOT NULL,
    hiring_recommendation   VARCHAR(20)  NOT NULL,
    recommended_at          DATETIME     NOT NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_eligibility_application_id UNIQUE (application_id)
);
CREATE INDEX idx_assessment_eligibility_job_id ON assessment_eligibility(job_id);

CREATE TABLE candidate_assessment (
    id                          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    assessment_id                CHAR(36)     NOT NULL,
    application_id               CHAR(36)     NOT NULL,
    job_id                       CHAR(36)     NOT NULL,
    candidate_id                 CHAR(36)     NOT NULL,
    status                       VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    invited_at                   DATETIME,
    reminded_at                  DATETIME,
    expires_at                   DATETIME,
    rules_accepted_at             DATETIME,
    started_at                   DATETIME,
    completed_at                 DATETIME,
    current_question_index         INT          NOT NULL DEFAULT 0,
    language_preference           VARCHAR(20),
    total_score                  INT,
    test_cases_passed             INT,
    test_cases_total              INT,
    time_used_seconds             INT,
    auto_submitted                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_candidate_assessment_assessment_application UNIQUE (assessment_id, application_id),
    CONSTRAINT fk_candidate_assessment_assessment FOREIGN KEY (assessment_id) REFERENCES assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_assessment_application_id ON candidate_assessment(application_id);
CREATE INDEX idx_candidate_assessment_job_id ON candidate_assessment(job_id);
CREATE INDEX idx_candidate_assessment_status ON candidate_assessment(status);

CREATE TABLE candidate_question_progress (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_assessment_id     CHAR(36)     NOT NULL,
    question_id                CHAR(36)     NOT NULL,
    display_order              INT          NOT NULL DEFAULT 0,
    visited                    BOOLEAN      NOT NULL DEFAULT FALSE,
    marked_for_review           BOOLEAN      NOT NULL DEFAULT FALSE,
    last_visited_at             DATETIME,
    CONSTRAINT uq_candidate_question_progress UNIQUE (candidate_assessment_id, question_id),
    CONSTRAINT fk_candidate_question_progress_ca FOREIGN KEY (candidate_assessment_id) REFERENCES candidate_assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_question_progress_ca_id ON candidate_question_progress(candidate_assessment_id);

CREATE TABLE submission (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_assessment_id     CHAR(36)     NOT NULL,
    question_id                CHAR(36)     NOT NULL,
    language                   VARCHAR(20)  NOT NULL,
    code                       LONGTEXT     NOT NULL,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    score                      INT,
    test_cases_passed           INT,
    test_cases_total            INT,
    runtime_ms                 INT,
    memory_kb                  INT,
    attempt_number              INT          NOT NULL DEFAULT 1,
    compile_output              TEXT,
    submitted_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_ca FOREIGN KEY (candidate_assessment_id) REFERENCES candidate_assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_submission_ca_id ON submission(candidate_assessment_id);
CREATE INDEX idx_submission_question_id ON submission(question_id);

CREATE TABLE submission_test_case (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    submission_id     CHAR(36)     NOT NULL,
    test_case_id      CHAR(36)     NOT NULL,
    passed            BOOLEAN      NOT NULL DEFAULT FALSE,
    actual_output      TEXT,
    runtime_ms         INT,
    memory_kb          INT,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_submission_test_case_submission FOREIGN KEY (submission_id) REFERENCES submission(id) ON DELETE CASCADE
);
CREATE INDEX idx_submission_test_case_submission_id ON submission_test_case(submission_id);

CREATE TABLE execution_log (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_assessment_id     CHAR(36)     NOT NULL,
    question_id                CHAR(36)     NOT NULL,
    language                   VARCHAR(20)  NOT NULL,
    code                       LONGTEXT     NOT NULL,
    custom_input                 TEXT,
    output                     TEXT,
    stderr                     TEXT,
    runtime_ms                 INT,
    memory_kb                  INT,
    executed_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_execution_log_ca FOREIGN KEY (candidate_assessment_id) REFERENCES candidate_assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_execution_log_ca_id ON execution_log(candidate_assessment_id);

CREATE TABLE ai_code_review (
    id                            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    submission_id                  CHAR(36)     NOT NULL,
    time_complexity                 VARCHAR(50),
    space_complexity                VARCHAR(50),
    naming_convention_notes           TEXT,
    code_quality_score               INT,
    solid_principles_notes            TEXT,
    design_patterns_notes             TEXT,
    security_issues                  TEXT,
    optimization_suggestions          TEXT,
    clean_code_notes                 TEXT,
    overall_rating                   INT,
    provider_used                    VARCHAR(20),
    CONSTRAINT uq_ai_code_review_submission_id UNIQUE (submission_id),
    CONSTRAINT fk_ai_code_review_submission FOREIGN KEY (submission_id) REFERENCES submission(id) ON DELETE CASCADE
);

CREATE TABLE ai_code_review_note (
    id                    CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    ai_code_review_id       CHAR(36)     NOT NULL,
    note_type             VARCHAR(15)  NOT NULL,
    description           VARCHAR(500) NOT NULL,
    display_order         INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_ai_code_review_note_review FOREIGN KEY (ai_code_review_id) REFERENCES ai_code_review(id) ON DELETE CASCADE
);
CREATE INDEX idx_ai_code_review_note_review_id ON ai_code_review_note(ai_code_review_id);

CREATE TABLE anti_cheat_log (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_assessment_id     CHAR(36)     NOT NULL,
    event_type                 VARCHAR(20)  NOT NULL,
    occurred_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata                   VARCHAR(500),
    CONSTRAINT fk_anti_cheat_log_ca FOREIGN KEY (candidate_assessment_id) REFERENCES candidate_assessment(id) ON DELETE CASCADE
);
CREATE INDEX idx_anti_cheat_log_ca_id ON anti_cheat_log(candidate_assessment_id);
