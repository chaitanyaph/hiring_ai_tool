-- =====================================================================
-- Question Bank: status lifecycle, richer authoring fields, per-test-case
-- explanation/weight, hints child table, and pass/fail on the candidate's
-- attempt.
-- =====================================================================

ALTER TABLE question
    ADD COLUMN status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' AFTER title,
    ADD COLUMN input_format       TEXT         AFTER constraints_text,
    ADD COLUMN output_format      TEXT         AFTER input_format,
    ADD COLUMN explanation        TEXT         AFTER output_format,
    ADD COLUMN topics            VARCHAR(300) AFTER tags,
    ADD COLUMN time_limit_ms      INT          NOT NULL DEFAULT 2000 AFTER allowed_languages,
    ADD COLUMN memory_limit_mb    INT          NOT NULL DEFAULT 256 AFTER time_limit_ms;

-- Pre-existing rows were created before the lifecycle existed and are
-- already in active use by real assessments -- default them to ACTIVE
-- (not DRAFT, which would silently vanish from the assessment builder).
CREATE INDEX idx_question_status ON question(status);

ALTER TABLE question_test_case
    ADD COLUMN explanation TEXT AFTER expected_output,
    ADD COLUMN weight      INT  NOT NULL DEFAULT 1 AFTER explanation;

CREATE TABLE question_hint (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    question_id       CHAR(36)     NOT NULL,
    hint_text          TEXT         NOT NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_question_hint_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);
CREATE INDEX idx_question_hint_question_id ON question_hint(question_id);

ALTER TABLE candidate_assessment
    ADD COLUMN passed BOOLEAN AFTER total_score;
