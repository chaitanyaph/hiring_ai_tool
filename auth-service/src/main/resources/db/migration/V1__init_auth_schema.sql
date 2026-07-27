-- =====================================================================
-- Cadence Auth Service - auth_db schema (MySQL 8)
-- Design decisions:
--  * UUID primary keys (MySQL UUID(), stored as CHAR(36)) -> safe for
--    distributed services, no collision across microservice databases,
--    no sequence coordination.
--  * Soft delete via is_deleted + deleted_at on user-facing tables, so
--    audit trails and referential history in other services stay intact.
--  * Every table has created_at/updated_at for auditability.
--  * Roles/Permissions are separate normalized tables (many-to-many) to
--    support fine-grained RBAC beyond simple role strings.
--  * Foreign keys are declared with explicit CONSTRAINT ... FOREIGN KEY
--    clauses (MySQL/InnoDB silently ignores Postgres-style inline
--    column-level REFERENCES, so it must be spelled out per constraint).
-- =====================================================================

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                      CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    company_id              CHAR(36),                          -- FK (logical) -> company_db, multi-tenant recruiters
    full_name               VARCHAR(150)  NOT NULL,
    email                   VARCHAR(180)  NOT NULL,
    phone_number            VARCHAR(20),
    password_hash           VARCHAR(255)  NOT NULL,
    user_type               VARCHAR(30)   NOT NULL,             -- CANDIDATE, RECRUITER, COMPANY_ADMIN, ADMIN
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING_VERIFICATION', -- PENDING_VERIFICATION, ACTIVE, LOCKED, DISABLED
    email_verified          BOOLEAN       NOT NULL DEFAULT FALSE,
    mfa_enabled             BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_login_attempts   INT           NOT NULL DEFAULT 0,
    account_locked_until    DATETIME,
    last_login_at           DATETIME,
    last_login_ip           VARCHAR(64),
    auth_provider           VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',  -- LOCAL, GOOGLE, GITHUB
    provider_id             VARCHAR(120),
    is_deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at              DATETIME,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_company_id ON users(company_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_user_type ON users(user_type);

-- ---------------------------------------------------------------------
-- ROLES & PERMISSIONS (RBAC)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    name          VARCHAR(60)  NOT NULL UNIQUE,      -- ROLE_ADMIN, ROLE_COMPANY_ADMIN, ROLE_HR_MANAGER, ROLE_HR_RECRUITER, ROLE_TECHNICAL_RECRUITER, ROLE_TALENT_ACQUISITION_MANAGER, ROLE_HIRING_MANAGER, ROLE_CANDIDATE
    description   VARCHAR(255),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    name          VARCHAR(80)  NOT NULL UNIQUE,      -- JOB_CREATE, CANDIDATE_VIEW, INTERVIEW_SCHEDULE, etc.
    description   VARCHAR(255),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id        CHAR(36) NOT NULL,
    permission_id  CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id   CHAR(36) NOT NULL,
    role_id   CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- REFRESH TOKENS  (rotation + remember-me support)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id             CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id        CHAR(36) NOT NULL,
    token_hash     VARCHAR(255) NOT NULL,          -- SHA-256 hash of raw token, never store raw token
    device_info    VARCHAR(255),
    ip_address     VARCHAR(64),
    remember_me    BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at     DATETIME NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at     DATETIME,
    replaced_by    CHAR(36),                        -- token rotation chain (not FK-enforced, same as original design)
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ---------------------------------------------------------------------
-- PASSWORD RESET TOKENS
-- ---------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id           CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id      CHAR(36) NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   DATETIME NOT NULL,
    used         BOOLEAN NOT NULL DEFAULT FALSE,
    used_at      DATETIME,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);

-- ---------------------------------------------------------------------
-- EMAIL VERIFICATION TOKENS
-- ---------------------------------------------------------------------
CREATE TABLE email_verification_tokens (
    id           CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id      CHAR(36) NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   DATETIME NOT NULL,
    used         BOOLEAN NOT NULL DEFAULT FALSE,
    used_at      DATETIME,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_verification_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_email_verification_user_id ON email_verification_tokens(user_id);

-- ---------------------------------------------------------------------
-- MFA SECRETS (TOTP)
-- ---------------------------------------------------------------------
CREATE TABLE mfa_secrets (
    id             CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id        CHAR(36) NOT NULL UNIQUE,
    secret_key     VARCHAR(255) NOT NULL,     -- encrypted Base32 TOTP secret
    confirmed      BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_codes TEXT,                       -- comma-separated hashed recovery codes
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mfa_secrets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- USER SESSIONS (device/session management, "log out from all devices")
-- ---------------------------------------------------------------------
CREATE TABLE user_sessions (
    id               CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id          CHAR(36) NOT NULL,
    refresh_token_id CHAR(36),
    device_info      VARCHAR(255),
    ip_address       VARCHAR(64),
    location         VARCHAR(120),
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    last_active_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at         DATETIME,
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_sessions_refresh_token FOREIGN KEY (refresh_token_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL
);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);

-- ---------------------------------------------------------------------
-- AUDIT LOGS
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id       CHAR(36),
    event_type    VARCHAR(60)  NOT NULL,   -- LOGIN_SUCCESS, LOGIN_FAILURE, PASSWORD_RESET, ACCOUNT_LOCKED, ...
    description   VARCHAR(500),
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(255),
    metadata      JSON,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ---------------------------------------------------------------------
-- SEED DATA: default roles & permissions
-- ---------------------------------------------------------------------
INSERT INTO roles (id, name, description) VALUES
 (UUID(), 'ROLE_ADMIN', 'Platform super administrator'),
 (UUID(), 'ROLE_COMPANY_ADMIN', 'Company owner / account administrator'),
 (UUID(), 'ROLE_HR_MANAGER', 'HR manager -- oversees recruiting operations for a company'),
 (UUID(), 'ROLE_HR_RECRUITER', 'HR recruiter -- general recruiting duties'),
 (UUID(), 'ROLE_TECHNICAL_RECRUITER', 'Technical recruiter -- engineering/technical roles'),
 (UUID(), 'ROLE_TALENT_ACQUISITION_MANAGER', 'Talent acquisition manager -- sourcing/pipeline strategy'),
 (UUID(), 'ROLE_HIRING_MANAGER', 'Hiring manager -- owns hiring decisions for their team''s open roles'),
 (UUID(), 'ROLE_CANDIDATE', 'Job applicant');

INSERT INTO permissions (id, name, description) VALUES
 (UUID(), 'USER_MANAGE', 'Create/update/deactivate platform users'),
 (UUID(), 'ROLE_MANAGE', 'Assign or revoke roles and permissions'),
 (UUID(), 'JOB_CREATE', 'Create job postings'),
 (UUID(), 'JOB_MANAGE', 'Edit/close job postings'),
 (UUID(), 'CANDIDATE_VIEW', 'View candidate profiles and resumes'),
 (UUID(), 'INTERVIEW_SCHEDULE', 'Schedule interviews'),
 (UUID(), 'OFFER_MANAGE', 'Generate and manage offer letters'),
 (UUID(), 'ANALYTICS_VIEW', 'View analytics dashboards'),
 (UUID(), 'CANDIDATE_APPLY', 'Apply to jobs and manage own application'),
 (UUID(), 'AUDIT_VIEW', 'View audit logs');

-- ROLE_ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ROLE_ADMIN';

-- ROLE_COMPANY_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_COMPANY_ADMIN'
  AND p.name IN ('USER_MANAGE','ROLE_MANAGE','JOB_CREATE','JOB_MANAGE','CANDIDATE_VIEW','INTERVIEW_SCHEDULE','OFFER_MANAGE','ANALYTICS_VIEW','AUDIT_VIEW');

-- ROLE_HR_MANAGER: broadest recruiting-side role short of company admin (adds user/role
-- management within their own company's recruiting team, on top of the core recruiting set).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_HR_MANAGER'
  AND p.name IN ('USER_MANAGE','JOB_CREATE','JOB_MANAGE','CANDIDATE_VIEW','INTERVIEW_SCHEDULE','OFFER_MANAGE','ANALYTICS_VIEW');

-- ROLE_HR_RECRUITER, ROLE_TECHNICAL_RECRUITER, ROLE_TALENT_ACQUISITION_MANAGER: the core
-- day-to-day recruiting permission set, identical across these three operational roles --
-- they differ in which jobs/candidates they're routed by upstream services, not in platform
-- permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('ROLE_HR_RECRUITER','ROLE_TECHNICAL_RECRUITER','ROLE_TALENT_ACQUISITION_MANAGER')
  AND p.name IN ('JOB_CREATE','JOB_MANAGE','CANDIDATE_VIEW','INTERVIEW_SCHEDULE','OFFER_MANAGE','ANALYTICS_VIEW');

-- ROLE_HIRING_MANAGER: narrower than the recruiter roles -- reviews candidates and
-- participates in interviews/decisions for their own open reqs, but doesn't create/manage
-- job postings or generate offers directly.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_HIRING_MANAGER'
  AND p.name IN ('CANDIDATE_VIEW','INTERVIEW_SCHEDULE','ANALYTICS_VIEW');

-- ROLE_CANDIDATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_CANDIDATE'
  AND p.name IN ('CANDIDATE_APPLY');
