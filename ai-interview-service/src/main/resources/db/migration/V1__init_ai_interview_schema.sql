-- =====================================================================
-- Cadence AI Interview Service - ai_interview_db (MySQL 8). application_id/
-- job_id/candidate_id/resume_match_id are plain CHAR(36) references into
-- other services' own databases -- never a foreign key across service
-- boundaries, per the platform's database-per-service rule.
--
-- Only candidate_shortlist and interview_session (the two aggregate
-- roots) carry the full audit trail -- every child table is system-
-- generated and fully replaced/appended as its parent's pipeline runs,
-- same precedent as resume_parser_db.
-- =====================================================================

CREATE TABLE candidate_shortlist (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id            CHAR(36)     NOT NULL,
    job_id                    CHAR(36)     NOT NULL,
    department_id             CHAR(36),
    candidate_id              CHAR(36)     NOT NULL,
    resume_match_id           CHAR(36),
    overall_match_score       INT,
    decision                  VARCHAR(20)  NOT NULL,
    reason                    TEXT,
    assigned_recruiter_id     CHAR(36),
    decided_at                DATETIME,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                CHAR(36),
    updated_by                CHAR(36),
    version                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_candidate_shortlist_application_id UNIQUE (application_id)
);
CREATE INDEX idx_candidate_shortlist_job_id ON candidate_shortlist(job_id);
CREATE INDEX idx_candidate_shortlist_department_id ON candidate_shortlist(department_id);
CREATE INDEX idx_candidate_shortlist_decision ON candidate_shortlist(decision);

CREATE TABLE interview_session (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    application_id            CHAR(36)     NOT NULL,
    job_id                    CHAR(36)     NOT NULL,
    candidate_id              CHAR(36)     NOT NULL,
    mode                      VARCHAR(10),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    total_questions            INT          NOT NULL DEFAULT 8,
    current_question_index     INT          NOT NULL DEFAULT 0,
    invited_at                 DATETIME,
    started_at                 DATETIME,
    completed_at               DATETIME,
    expires_at                 DATETIME,
    failure_reason             TEXT,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                CHAR(36),
    updated_by                CHAR(36),
    version                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_interview_session_application_id UNIQUE (application_id)
);
CREATE INDEX idx_interview_session_job_id ON interview_session(job_id);
CREATE INDEX idx_interview_session_candidate_id ON interview_session(candidate_id);
CREATE INDEX idx_interview_session_status ON interview_session(status);

CREATE TABLE interview_question (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    session_id        CHAR(36)     NOT NULL,
    question_order    INT          NOT NULL,
    category          VARCHAR(30)  NOT NULL,
    question_text     TEXT         NOT NULL,
    asked_at          DATETIME,
    CONSTRAINT fk_interview_question_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_question_session_id ON interview_question(session_id);

CREATE TABLE interview_answer (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    question_id               CHAR(36)     NOT NULL,
    session_id                CHAR(36)     NOT NULL,
    answer_text                TEXT,
    response_time_seconds       INT,
    answered_at                DATETIME,
    CONSTRAINT uq_interview_answer_question_id UNIQUE (question_id),
    CONSTRAINT fk_interview_answer_question FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_answer_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_answer_session_id ON interview_answer(session_id);

CREATE TABLE interview_score (
    id                            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    session_id                    CHAR(36)     NOT NULL,
    communication_score            INT,
    confidence_score               INT,
    technical_accuracy_score        INT,
    problem_solving_score           INT,
    grammar_score                  INT,
    behavior_score                 INT,
    leadership_score               INT,
    domain_knowledge_score          INT,
    overall_score                  INT,
    eye_contact_score               INT,
    speaking_pace_score             INT,
    filler_word_count               INT,
    avg_response_latency_seconds     INT,
    CONSTRAINT uq_interview_score_session_id UNIQUE (session_id),
    CONSTRAINT fk_interview_score_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);

CREATE TABLE interview_recommendation (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    session_id                CHAR(36)     NOT NULL,
    hiring_recommendation      VARCHAR(20)  NOT NULL,
    interview_summary          TEXT,
    recruiter_summary           TEXT,
    CONSTRAINT uq_interview_recommendation_session_id UNIQUE (session_id),
    CONSTRAINT fk_interview_recommendation_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);

CREATE TABLE interview_feedback_note (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    session_id        CHAR(36)     NOT NULL,
    note_type         VARCHAR(15)  NOT NULL,
    description       VARCHAR(500) NOT NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_interview_feedback_note_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_feedback_note_session_id ON interview_feedback_note(session_id);

CREATE TABLE interview_log (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    session_id        CHAR(36)     NOT NULL,
    log_level         VARCHAR(10)  NOT NULL,
    message           TEXT         NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_log_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_log_session_id ON interview_log(session_id);
