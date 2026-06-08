-- Schema migration for timeline events
--
-- Example:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260603_add_timeline_events_schema.sql

CREATE TABLE IF NOT EXISTS timeline_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    related_suspect_id BIGINT NULL,
    related_evidence_id BIGINT NULL,
    event_time VARCHAR(50) NOT NULL,
    event_order INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    event_type VARCHAR(50) NOT NULL DEFAULT 'FACT',
    is_true_event TINYINT(1) NOT NULL DEFAULT 1,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_timeline_events_order (scenario_id, event_order),
    KEY idx_timeline_events_suspect (related_suspect_id),
    KEY idx_timeline_events_evidence (related_evidence_id),
    CONSTRAINT fk_timeline_events_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_timeline_events_suspect
        FOREIGN KEY (related_suspect_id) REFERENCES suspects (id),
    CONSTRAINT fk_timeline_events_evidence
        FOREIGN KEY (related_evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
