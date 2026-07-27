-- =====================================================================
-- Adds real company records so a Company Admin's registration can
-- capture an actual company name/workspace, instead of company_id being
-- a bare UUID pointing at nothing. Company profile data may eventually
-- move to a dedicated Company Service, but for now it lives alongside
-- auth data so registration can create + link it in one transaction.
-- =====================================================================

CREATE TABLE companies (
    id          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(160) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_companies_slug UNIQUE (slug)
);
CREATE INDEX idx_companies_name ON companies(name);

ALTER TABLE users
    ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL;
