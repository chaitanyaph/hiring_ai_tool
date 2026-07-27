-- =====================================================================
-- Cadence Company Service - company_db schema (MySQL 8)
-- Owns: company profile, departments, offices, team invitations.
-- Does NOT own: authentication, passwords, JWT, sessions, job postings,
-- candidates, notifications, billing.
--
-- Soft delete: is_deleted + deleted_at, never a hard DELETE, so other
-- services keep valid historical references.
-- Uniqueness note: MySQL has no partial/filtered unique index, so
-- "unique within non-deleted rows" (department name per company) is
-- enforced at the service layer, not here -- a DB-level UNIQUE on
-- (company_id, department_name) would incorrectly block reusing a name
-- after its previous holder was soft-deleted.
-- =====================================================================

CREATE TABLE companies (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_name        VARCHAR(150) NOT NULL,
    company_slug        VARCHAR(160) NOT NULL,
    industry            VARCHAR(120),
    website             VARCHAR(255),
    company_email       VARCHAR(180),
    company_phone       VARCHAR(30),
    headquarters        VARCHAR(200),
    description         TEXT,
    company_logo        VARCHAR(500),
    subscription_plan   VARCHAR(60)  NOT NULL DEFAULT 'FREE',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          DATETIME,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_companies_name UNIQUE (company_name),
    CONSTRAINT uq_companies_slug UNIQUE (company_slug)
);
CREATE INDEX idx_companies_status ON companies(status);

CREATE TABLE departments (
    id               CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id       CHAR(36)     NOT NULL,
    department_name  VARCHAR(100) NOT NULL,
    description      VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(150),
    updated_by       VARCHAR(150),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_departments_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX idx_departments_company_id ON departments(company_id);

CREATE TABLE offices (
    id                 CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id         CHAR(36)     NOT NULL,
    office_name        VARCHAR(120) NOT NULL,
    country            VARCHAR(100),
    state              VARCHAR(100),
    city               VARCHAR(100),
    address            VARCHAR(255),
    postal_code        VARCHAR(20),
    timezone           VARCHAR(60),
    latitude           DECIMAL(9,6),
    longitude          DECIMAL(9,6),
    is_primary_office  BOOLEAN      NOT NULL DEFAULT FALSE,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at         DATETIME,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(150),
    updated_by         VARCHAR(150),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_offices_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX idx_offices_company_id ON offices(company_id);

CREATE TABLE team_invitations (
    id             CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    company_id     CHAR(36)     NOT NULL,
    department_id  CHAR(36),
    email          VARCHAR(180) NOT NULL,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    role           VARCHAR(40)  NOT NULL,
    invite_token   CHAR(36)     NOT NULL,
    expiry_date    DATETIME     NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by     VARCHAR(150),
    accepted_at    DATETIME,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_team_invitations_token UNIQUE (invite_token),
    CONSTRAINT fk_team_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_invitations_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);
CREATE INDEX idx_team_invitations_company_id ON team_invitations(company_id);
CREATE INDEX idx_team_invitations_email ON team_invitations(email);
CREATE INDEX idx_team_invitations_status ON team_invitations(status);
