-- Schema migration for Custom Scenario features (published_at and solutions)
--
-- Example:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260605_add_custom_scenario_schema.sql

ALTER TABLE scenarios
    ADD COLUMN published_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS solutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    culprit_suspect_id BIGINT NOT NULL,
    motive TEXT NOT NULL,
    method TEXT NOT NULL,
    cover_up TEXT NULL,
    full_explanation TEXT NULL,
    key_evidence_ids TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solutions_scenario (scenario_id),
    KEY idx_solutions_suspect (culprit_suspect_id),
    CONSTRAINT fk_solutions_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_solutions_suspect
        FOREIGN KEY (culprit_suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
