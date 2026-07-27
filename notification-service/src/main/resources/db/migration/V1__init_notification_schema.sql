-- =====================================================================
-- Cadence Notification Service - notification_db (MySQL 8).
-- recipient_id/company_id/entity_id/user_id are plain CHAR(36)
-- references into other services' own databases -- never a foreign
-- key across service boundaries, per the platform's database-per-
-- service rule.
--
-- Consolidated from the suggested 9 tables down to 6:
--   - notification_status folded into notification.status (enum column).
--   - notification_history + notification_delivery + the rest of
--     notification_status folded into email_queue -- one status-machine
--     row per email serves as both the send queue AND the history/
--     delivery record, queried differently for the Queue/History/
--     Scheduled/Failed views (same "don't split a status machine
--     across two tables" precedent used throughout this platform).
-- =====================================================================

-- trigger_event = which Kafka event (if any -- 'NONE' where no real
-- publisher exists yet) drives evaluation of this template;
-- category = the unique business code used to actually pick a
-- template at render time (several templates can share one
-- trigger_event, e.g. USER_REGISTERED branches into
-- CANDIDATE_REGISTRATION vs WELCOME_EMAIL by category).
CREATE TABLE notification_template (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    name              VARCHAR(150)  NOT NULL,
    trigger_event     VARCHAR(50)   NOT NULL,
    category          VARCHAR(50)   NOT NULL,
    subject           VARCHAR(500)  NOT NULL,
    body_html         LONGTEXT      NOT NULL,
    variables_hint    VARCHAR(500),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_template_category UNIQUE (category)
);
CREATE INDEX idx_notification_template_trigger_event ON notification_template(trigger_event);

CREATE TABLE notification (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    recipient_id      CHAR(36)      NOT NULL,
    recipient_role    VARCHAR(40)   NOT NULL,
    company_id        CHAR(36),
    category          VARCHAR(30)   NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    message           VARCHAR(1000) NOT NULL,
    color_tone        VARCHAR(20)   NOT NULL DEFAULT 'INFO',
    entity_type       VARCHAR(30),
    entity_id         CHAR(36),
    status            VARCHAR(20)   NOT NULL DEFAULT 'UNREAD',
    read_at           DATETIME,
    archived_at       DATETIME,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_notification_recipient_id ON notification(recipient_id);
CREATE INDEX idx_notification_recipient_status ON notification(recipient_id, status);

CREATE TABLE email_queue (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    template_id           CHAR(36),
    recipient_email       VARCHAR(255)  NOT NULL,
    recipient_name        VARCHAR(200),
    subject               VARCHAR(500)  NOT NULL,
    body_html             LONGTEXT      NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts              INT           NOT NULL DEFAULT 0,
    max_attempts          INT           NOT NULL DEFAULT 5,
    scheduled_at          DATETIME,
    next_retry_at         DATETIME,
    sent_at               DATETIME,
    delivered_at          DATETIME,
    opened_at             DATETIME,
    failure_reason        VARCHAR(1000),
    related_entity_type   VARCHAR(30),
    related_entity_id     CHAR(36),
    trigger_event         VARCHAR(50),
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_email_queue_template FOREIGN KEY (template_id) REFERENCES notification_template(id) ON DELETE SET NULL
);
CREATE INDEX idx_email_queue_status ON email_queue(status);
CREATE INDEX idx_email_queue_recipient_email ON email_queue(recipient_email);
CREATE INDEX idx_email_queue_scheduled_at ON email_queue(scheduled_at);

CREATE TABLE email_attachment (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    email_queue_id    CHAR(36)      NOT NULL,
    file_name         VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(100)  NOT NULL,
    size_bytes        BIGINT        NOT NULL,
    content           LONGBLOB      NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_attachment_queue FOREIGN KEY (email_queue_id) REFERENCES email_queue(id) ON DELETE CASCADE
);
CREATE INDEX idx_email_attachment_queue_id ON email_attachment(email_queue_id);

CREATE TABLE notification_preference (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id           CHAR(36)      NOT NULL,
    category          VARCHAR(40)   NOT NULL,
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_preference_user_category UNIQUE (user_id, category)
);
CREATE INDEX idx_notification_preference_user_id ON notification_preference(user_id);

CREATE TABLE notification_log (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    level                 VARCHAR(10)   NOT NULL DEFAULT 'INFO',
    source                VARCHAR(60)   NOT NULL,
    event_type            VARCHAR(60)   NOT NULL,
    message                VARCHAR(1000) NOT NULL,
    related_entity_id      CHAR(36),
    occurred_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notification_log_occurred_at ON notification_log(occurred_at);
CREATE INDEX idx_notification_log_source ON notification_log(source);
