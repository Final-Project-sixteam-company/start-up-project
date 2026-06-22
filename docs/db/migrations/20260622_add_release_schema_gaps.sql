-- Schema migration for release-time JPA validation gaps.
--
-- Apply before deploying entities that map examples and solution_evidences:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260622_add_release_schema_gaps.sql
--
-- Rollback, only after backup/approval:
-- DROP TABLE solution_evidences;
-- DROP TABLE examples;

CREATE TABLE IF NOT EXISTS examples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_examples_status_deleted (status, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS solution_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solution_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    reason TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_evidences_pair (solution_id, evidence_id),
    KEY idx_solution_evidences_evidence (evidence_id),
    CONSTRAINT fk_solution_evidences_solution
        FOREIGN KEY (solution_id) REFERENCES solutions (id),
    CONSTRAINT fk_solution_evidences_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
