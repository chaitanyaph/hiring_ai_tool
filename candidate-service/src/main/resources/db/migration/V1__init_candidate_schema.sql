-- =====================================================================
-- Cadence Candidate Service - candidate_db schema (MySQL 8)
-- Owns: candidate profiles, resumes (metadata only), applications and
-- saved jobs. Does NOT own auth/credentials (Auth Service), job
-- postings (Job Service), or interview/assessment scoring (future AI
-- services) -- those are referenced here only by id, or as a snapshot
-- taken at the moment of interaction (e.g. application.job_title).
-- =====================================================================

-- candidates.id is NOT auto-generated: it is the same userId Auth
-- Service issued at registration, since a candidate profile is
-- inherently 1:1 with a candidate user account.
CREATE TABLE candidates (
    id                          CHAR(36)     PRIMARY KEY,
    full_name                   VARCHAR(150) NOT NULL,
    headline                    VARCHAR(150),
    email                       VARCHAR(150) NOT NULL,
    phone                       VARCHAR(20),
    location                    VARCHAR(200),
    current_company             VARCHAR(150),
    notice_period_days          INT,
    profile_photo_url           VARCHAR(500),
    resume_url                  VARCHAR(500),
    resume_filename             VARCHAR(255),
    resume_parsed_at            DATETIME,
    ai_resume_score             INT,
    profile_completion_percent  INT          NOT NULL DEFAULT 0,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at                  DATETIME,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    version                     BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_candidates_email ON candidates(email);

CREATE TABLE candidate_education (
    id           CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id CHAR(36)     NOT NULL,
    degree       VARCHAR(150) NOT NULL,
    institution  VARCHAR(200) NOT NULL,
    start_year   INT,
    end_year     INT,
    grade        VARCHAR(30),
    display_order INT         NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_cand_education_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_education_candidate_id ON candidate_education(candidate_id);

CREATE TABLE candidate_experience (
    id                 CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id       CHAR(36)     NOT NULL,
    job_title          VARCHAR(150) NOT NULL,
    company_name       VARCHAR(150) NOT NULL,
    start_date         DATE,
    end_date           DATE,
    currently_working  BOOLEAN      NOT NULL DEFAULT FALSE,
    achievements       LONGTEXT,
    display_order      INT          NOT NULL DEFAULT 0,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at         DATETIME,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         CHAR(36),
    updated_by         CHAR(36),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_cand_experience_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_experience_candidate_id ON candidate_experience(candidate_id);

CREATE TABLE candidate_skills (
    id           CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id CHAR(36)     NOT NULL,
    skill_name   VARCHAR(100) NOT NULL,
    CONSTRAINT uq_cand_skills_candidate_skill UNIQUE (candidate_id, skill_name),
    CONSTRAINT fk_cand_skills_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_skills_candidate_id ON candidate_skills(candidate_id);

CREATE TABLE candidate_projects (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id  CHAR(36)     NOT NULL,
    title         VARCHAR(150) NOT NULL,
    description   LONGTEXT,
    project_url   VARCHAR(500),
    display_order INT          NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_cand_projects_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_projects_candidate_id ON candidate_projects(candidate_id);

CREATE TABLE candidate_certifications (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id    CHAR(36)     NOT NULL,
    name            VARCHAR(200) NOT NULL,
    issued_by       VARCHAR(150),
    issue_date      DATE,
    credential_url  VARCHAR(500),
    display_order   INT          NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      CHAR(36),
    updated_by      CHAR(36),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_cand_certifications_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_certifications_candidate_id ON candidate_certifications(candidate_id);

CREATE TABLE candidate_languages (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id  CHAR(36)     NOT NULL,
    language_name VARCHAR(60)  NOT NULL,
    CONSTRAINT uq_cand_languages_candidate_lang UNIQUE (candidate_id, language_name),
    CONSTRAINT fk_cand_languages_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_languages_candidate_id ON candidate_languages(candidate_id);

-- 1:1 with candidate -- own row so the wizard's Step 9 can PATCH
-- independently without touching the rest of the profile.
CREATE TABLE candidate_job_preferences (
    id                        CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id              CHAR(36)     NOT NULL,
    preferred_work_type       VARCHAR(20),
    preferred_employment_type VARCHAR(20),
    expected_salary           DECIMAL(12,2),
    salary_currency           VARCHAR(10)  NOT NULL DEFAULT 'INR',
    notice_period             VARCHAR(30),
    preferred_locations       VARCHAR(500),
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cand_job_prefs_candidate UNIQUE (candidate_id),
    CONSTRAINT fk_cand_job_prefs_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- 1:1 with candidate -- Step 10 of the wizard.
CREATE TABLE candidate_portfolio_links (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id  CHAR(36)     NOT NULL,
    website_url   VARCHAR(500),
    linkedin_url  VARCHAR(500),
    github_url    VARCHAR(500),
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cand_portfolio_candidate UNIQUE (candidate_id),
    CONSTRAINT fk_cand_portfolio_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

CREATE TABLE saved_jobs (
    id            CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    candidate_id  CHAR(36) NOT NULL,
    job_id        CHAR(36) NOT NULL,
    saved_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_saved_jobs_candidate_job UNIQUE (candidate_id, job_id),
    CONSTRAINT fk_saved_jobs_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_saved_jobs_candidate_id ON saved_jobs(candidate_id);

-- Job/company fields are snapshotted at apply-time (title, company_name,
-- location, employment_type) so an application remains readable even if
-- Job Service or Company Service is unreachable, or the job is later
-- archived/deleted upstream.
CREATE TABLE applications (
    id                CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id      CHAR(36)     NOT NULL,
    job_id            CHAR(36)     NOT NULL,
    company_id        CHAR(36)     NOT NULL,
    job_title_snapshot      VARCHAR(150),
    company_name_snapshot   VARCHAR(150),
    location_snapshot       VARCHAR(200),
    employment_type_snapshot VARCHAR(20),
    status            VARCHAR(30)  NOT NULL DEFAULT 'APPLIED',
    match_score        INT,
    applied_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at        DATETIME,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          DATETIME,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          CHAR(36),
    updated_by           CHAR(36),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_applications_candidate_job UNIQUE (candidate_id, job_id),
    CONSTRAINT fk_applications_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);
CREATE INDEX idx_applications_candidate_id ON applications(candidate_id);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_company_id ON applications(company_id);
CREATE INDEX idx_applications_status ON applications(status);

CREATE TABLE application_status_history (
    id              CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    application_id  CHAR(36)    NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      CHAR(36),
    changed_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            VARCHAR(500),
    CONSTRAINT fk_app_status_history_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
CREATE INDEX idx_app_status_history_application_id ON application_status_history(application_id);
