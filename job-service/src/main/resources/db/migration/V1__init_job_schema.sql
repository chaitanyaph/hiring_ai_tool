-- =====================================================================
-- Cadence Job Service - job_db schema (MySQL 8)
-- Owns: job postings and their full lifecycle. Does NOT own auth,
-- recruiters/hiring-managers-as-people (those are Auth Service users,
-- only referenced here by userId), candidates, resumes, interviews,
-- notifications, or billing.
-- =====================================================================

CREATE TABLE jobs (
    id                     CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id             CHAR(36)     NOT NULL,
    department_id          CHAR(36)     NOT NULL,
    job_code               VARCHAR(40)  NOT NULL,
    title                  VARCHAR(150) NOT NULL,
    location               VARCHAR(200),
    work_type              VARCHAR(20),
    employment_type        VARCHAR(20),
    number_of_openings     INT,
    application_deadline   DATE,
    recruiter_id           CHAR(36),
    hiring_manager_id      CHAR(36),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at           DATETIME,
    closed_at              DATETIME,
    archived_at            DATETIME,
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at             DATETIME,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             CHAR(36),
    updated_by             CHAR(36),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_jobs_company_jobcode UNIQUE (company_id, job_code)
);
CREATE INDEX idx_jobs_company_id ON jobs(company_id);
CREATE INDEX idx_jobs_department_id ON jobs(department_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_title ON jobs(title);
CREATE INDEX idx_jobs_recruiter_id ON jobs(recruiter_id);
CREATE INDEX idx_jobs_hiring_manager_id ON jobs(hiring_manager_id);

CREATE TABLE job_description (
    id                CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    job_id            CHAR(36) NOT NULL,
    description_html  LONGTEXT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_job_description_job UNIQUE (job_id),
    CONSTRAINT fk_job_description_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE TABLE job_requirements (
    id                    CHAR(36)       PRIMARY KEY DEFAULT (UUID()),
    job_id                CHAR(36)       NOT NULL,
    min_experience_years  INT,
    max_experience_years  INT,
    education             VARCHAR(200),
    certifications        LONGTEXT,
    languages             LONGTEXT,
    min_salary            DECIMAL(12,2),
    max_salary            DECIMAL(12,2),
    salary_currency       VARCHAR(10) NOT NULL DEFAULT 'INR',
    notice_period_days    INT,
    responsibilities      LONGTEXT,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_job_requirements_job UNIQUE (job_id),
    CONSTRAINT fk_job_requirements_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE TABLE job_skills (
    id          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    job_id      CHAR(36)     NOT NULL,
    skill_name  VARCHAR(100) NOT NULL,
    skill_type  VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);

CREATE TABLE job_benefits (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    job_id        CHAR(36)     NOT NULL,
    benefit_text  VARCHAR(255) NOT NULL,
    CONSTRAINT fk_job_benefits_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_benefits_job_id ON job_benefits(job_id);

CREATE TABLE job_pipeline_stage (
    id                 CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    job_id             CHAR(36)     NOT NULL,
    stage_name         VARCHAR(100) NOT NULL,
    stage_order        INT          NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_system_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_job_pipeline_stage_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_pipeline_stage_job_id ON job_pipeline_stage(job_id);

CREATE TABLE job_template (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id          CHAR(36)     NOT NULL,
    template_name       VARCHAR(150) NOT NULL,
    template_data_json  LONGTEXT     NOT NULL,
    created_by          CHAR(36),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_template_company_id ON job_template(company_id);

CREATE TABLE job_assignment (
    id                CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    job_id            CHAR(36)    NOT NULL,
    user_id           CHAR(36)    NOT NULL,
    assignment_role   VARCHAR(20) NOT NULL,
    assigned_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_job_assignment_role UNIQUE (job_id, assignment_role),
    CONSTRAINT fk_job_assignment_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_assignment_user_id ON job_assignment(user_id);

CREATE TABLE job_status_history (
    id           CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    job_id       CHAR(36)    NOT NULL,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20) NOT NULL,
    changed_by   CHAR(36),
    changed_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason       VARCHAR(255),
    CONSTRAINT fk_job_status_history_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_status_history_job_id ON job_status_history(job_id);

CREATE TABLE job_audit (
    id            CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    job_id        CHAR(36)    NOT NULL,
    action        VARCHAR(60) NOT NULL,
    performed_by  CHAR(36),
    performed_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details       LONGTEXT,
    CONSTRAINT fk_job_audit_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_audit_job_id ON job_audit(job_id);
