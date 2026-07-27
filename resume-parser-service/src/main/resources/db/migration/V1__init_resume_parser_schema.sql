-- =====================================================================
-- Cadence Resume Parser Service - resume_parser_db schema (MySQL 8)
-- Owns: structured data extracted from resumes by an LLM, plus the
-- processing status/log trail for each resume. resume_id/candidate_id
-- are plain CHAR(36) references into Resume Service's / Candidate
-- Service's own databases -- never a foreign key across service
-- boundaries, per the platform's database-per-service rule. This
-- service does not store the resume file itself (MinIO, owned by
-- Resume Service) or the candidate profile (Candidate Service).
--
-- Only parsed_resume (the aggregate root) carries the full audit
-- trail (created_at/updated_at/created_by/updated_by/version) --
-- the 7 child tables are system-generated, fully replaced on every
-- (re)parse rather than incrementally edited, so per-row audit
-- columns would carry no real information. That mirrors Candidate
-- Service's own candidate_skills/candidate_languages tables, which
-- are equally simple key facts with no BaseAuditEntity either.
-- =====================================================================

CREATE TABLE parsed_resume (
    id                       CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    resume_id                CHAR(36)      NOT NULL,
    candidate_id             CHAR(36)      NOT NULL,
    checksum                 CHAR(64)      NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'QUEUED',
    attempt_count            INT           NOT NULL DEFAULT 0,
    provider_used            VARCHAR(20),
    full_name                VARCHAR(150),
    email                    VARCHAR(150),
    phone                    VARCHAR(30),
    location                 VARCHAR(150),
    linkedin_url             VARCHAR(300),
    github_url               VARCHAR(300),
    portfolio_url            VARCHAR(300),
    professional_summary     TEXT,
    current_company          VARCHAR(150),
    current_designation      VARCHAR(150),
    total_experience_years   DECIMAL(4,1),
    notice_period            VARCHAR(50),
    expected_salary          VARCHAR(50),
    failure_reason           TEXT,
    parsed_at                DATETIME,
    created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               CHAR(36),
    updated_by               CHAR(36),
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_parsed_resume_resume_id UNIQUE (resume_id)
);
CREATE INDEX idx_parsed_resume_candidate_id ON parsed_resume(candidate_id);
CREATE INDEX idx_parsed_resume_status ON parsed_resume(status);

CREATE TABLE candidate_skill (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    skill_name        VARCHAR(100) NOT NULL,
    skill_category    VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_skill_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_skill_parsed_resume_id ON candidate_skill(parsed_resume_id);
CREATE INDEX idx_candidate_skill_category ON candidate_skill(skill_category);

CREATE TABLE candidate_experience (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    company_name      VARCHAR(150) NOT NULL,
    designation       VARCHAR(150),
    start_date        VARCHAR(20),
    end_date          VARCHAR(20),
    is_current        BOOLEAN      NOT NULL DEFAULT FALSE,
    description       TEXT,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_experience_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_experience_parsed_resume_id ON candidate_experience(parsed_resume_id);

CREATE TABLE candidate_project (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    project_name      VARCHAR(200) NOT NULL,
    description       TEXT,
    technologies      VARCHAR(500),
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_project_parsed_resume_id ON candidate_project(parsed_resume_id);

CREATE TABLE candidate_education (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    institution_name  VARCHAR(200) NOT NULL,
    degree            VARCHAR(150),
    field_of_study    VARCHAR(150),
    start_date        VARCHAR(20),
    end_date          VARCHAR(20),
    grade             VARCHAR(50),
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_education_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_education_parsed_resume_id ON candidate_education(parsed_resume_id);

CREATE TABLE candidate_certification (
    id                       CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id         CHAR(36)     NOT NULL,
    certification_name      VARCHAR(200) NOT NULL,
    issuing_organization     VARCHAR(200),
    issued_date              VARCHAR(20),
    expiry_date              VARCHAR(20),
    credential_id            VARCHAR(100),
    display_order            INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_certification_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_certification_parsed_resume_id ON candidate_certification(parsed_resume_id);

CREATE TABLE candidate_achievement (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    display_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_achievement_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_achievement_parsed_resume_id ON candidate_achievement(parsed_resume_id);

CREATE TABLE candidate_language (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)     NOT NULL,
    language_name     VARCHAR(100) NOT NULL,
    proficiency       VARCHAR(50),
    CONSTRAINT fk_language_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_candidate_language_parsed_resume_id ON candidate_language(parsed_resume_id);

CREATE TABLE parser_log (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    parsed_resume_id  CHAR(36)      NOT NULL,
    resume_id         CHAR(36)      NOT NULL,
    log_level         VARCHAR(10)   NOT NULL,
    message           VARCHAR(1000) NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_parsed_resume FOREIGN KEY (parsed_resume_id) REFERENCES parsed_resume(id) ON DELETE CASCADE
);
CREATE INDEX idx_parser_log_parsed_resume_id ON parser_log(parsed_resume_id);
CREATE INDEX idx_parser_log_resume_id ON parser_log(resume_id);
