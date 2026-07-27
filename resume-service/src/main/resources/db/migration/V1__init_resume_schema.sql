-- =====================================================================
-- Cadence Resume Service - resume_db schema (MySQL 8)
-- Owns: resume files and their metadata only. candidate_id is a plain
-- CHAR(36) reference into Candidate Service's own database -- never a
-- foreign key across service boundaries, per the platform's
-- database-per-service rule. Resume content itself lives in MinIO;
-- this table only stores bucket_name/object_name pointers to it.
-- =====================================================================

CREATE TABLE resumes (
    id                   CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    candidate_id         CHAR(36)     NOT NULL,
    display_name         VARCHAR(150) NOT NULL,
    original_file_name   VARCHAR(255) NOT NULL,
    file_extension       VARCHAR(10)  NOT NULL,
    mime_type            VARCHAR(100) NOT NULL,
    bucket_name          VARCHAR(100) NOT NULL,
    object_name          VARCHAR(500) NOT NULL,
    checksum             CHAR(64)     NOT NULL,
    file_size            BIGINT       NOT NULL,
    is_default           BOOLEAN      NOT NULL DEFAULT FALSE,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    uploaded_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           CHAR(36),
    updated_by           CHAR(36),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_resumes_object_name UNIQUE (bucket_name, object_name)
);
CREATE INDEX idx_resumes_candidate_id ON resumes(candidate_id);
CREATE INDEX idx_resumes_status ON resumes(status);
CREATE INDEX idx_resumes_candidate_checksum ON resumes(candidate_id, checksum);
