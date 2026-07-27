-- =====================================================================
-- Cadence Offer Management Service - offer_management_db (MySQL 8).
-- application_id/candidate_id/job_id/company_id/approver_id are plain
-- CHAR(36) references into other services' own databases -- never a
-- foreign key across service boundaries, per the platform's database-
-- per-service rule.
--
-- Consolidated from the suggested 13 tables down to 4:
--   - offer_status folded into offer.status (enum column).
--   - offer_salary + offer_component folded into offer (3 numeric
--     columns + a comma-separated benefits column) -- the Figma's
--     compensation step is exactly base/bonus/equity plus a fixed
--     4-item benefits checklist, not an itemized amount-bearing
--     component list.
--   - candidate_offer folded into offer (an offer inherently belongs
--     to one candidate, no separate join table needed).
--   - joining_details folded into offer (department/employment_type/
--     start_date are just offer columns -- no distinct wizard step
--     or Figma section for "joining details" exists).
--   - approval_workflow folded into offer (single approver_id/
--     approval_status/approved_at/approval_notes columns) -- the
--     Figma shows exactly ONE approval stage, not HR/Hiring-Manager/
--     Finance tiers.
--   - offer_history + approval_history merged into offer_activity_log
--     (one append-only table backs both the recruiter drawer's
--     timeline and the /history list endpoint).
--   - offer_version dropped entirely -- no version-history UI exists
--     anywhere in the Figma, and it's absent from the literal API
--     list too.
-- =====================================================================

CREATE TABLE offer (
    id                          CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    company_id                  CHAR(36),
    job_id                      CHAR(36),
    application_id              CHAR(36)      NOT NULL,
    candidate_id                CHAR(36)      NOT NULL,
    candidate_name               VARCHAR(200),
    candidate_email               VARCHAR(255),
    job_title                    VARCHAR(200),
    department                  VARCHAR(100),
    employment_type              VARCHAR(20),
    start_date                  DATE,
    base_salary                  DECIMAL(12,2),
    variable_bonus                DECIMAL(12,2),
    esop_equity                  DECIMAL(12,2),
    total_ctc                    DECIMAL(12,2),
    benefits                    VARCHAR(300),
    approver_id                  CHAR(36),
    approval_status               VARCHAR(20),
    approved_at                  DATETIME,
    approval_notes                VARCHAR(500),
    expiry_date                  DATE,
    status                       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    sent_at                      DATETIME,
    accepted_at                  DATETIME,
    declined_at                  DATETIME,
    decline_reason                VARCHAR(30),
    withdrawn_at                 DATETIME,
    withdraw_reason               VARCHAR(500),
    created_by_recruiter_id        CHAR(36),
    created_at                   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   CHAR(36),
    updated_by                   CHAR(36),
    version                      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_offer_application_id UNIQUE (application_id)
);
CREATE INDEX idx_offer_company_id ON offer(company_id);
CREATE INDEX idx_offer_candidate_id ON offer(candidate_id);
CREATE INDEX idx_offer_status ON offer(status);
CREATE INDEX idx_offer_expiry_date ON offer(expiry_date);

CREATE TABLE offer_document (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    offer_id          CHAR(36)      NOT NULL,
    offer_number      VARCHAR(50)   NOT NULL,
    file_name         VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(100)  NOT NULL DEFAULT 'application/pdf',
    size_bytes        BIGINT        NOT NULL,
    content           LONGBLOB      NOT NULL,
    generated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offer_document_offer FOREIGN KEY (offer_id) REFERENCES offer(id) ON DELETE CASCADE
);
CREATE INDEX idx_offer_document_offer_id ON offer_document(offer_id);

CREATE TABLE offer_negotiation (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    offer_id          CHAR(36)      NOT NULL,
    candidate_id      CHAR(36)      NOT NULL,
    proposed_ctc      DECIMAL(12,2),
    message           VARCHAR(1000),
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    recruiter_notes   VARCHAR(500),
    requested_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at      DATETIME,
    CONSTRAINT fk_offer_negotiation_offer FOREIGN KEY (offer_id) REFERENCES offer(id) ON DELETE CASCADE
);
CREATE INDEX idx_offer_negotiation_offer_id ON offer_negotiation(offer_id);

CREATE TABLE offer_activity_log (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    offer_id          CHAR(36)      NOT NULL,
    event_type        VARCHAR(30)   NOT NULL,
    actor_id          CHAR(36),
    occurred_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details           VARCHAR(1000),
    CONSTRAINT fk_offer_activity_log_offer FOREIGN KEY (offer_id) REFERENCES offer(id) ON DELETE CASCADE
);
CREATE INDEX idx_offer_activity_log_offer_id ON offer_activity_log(offer_id);
