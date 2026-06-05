-- Scenario YAML importer schema migration.
-- Apply once before deploying the scenario importer with prod ddl-auto=validate.
--
-- Example:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260601_add_scenario_import_schema.sql

ALTER TABLE scenarios
    ADD COLUMN code VARCHAR(100) NULL AFTER id,
    ADD COLUMN content_version VARCHAR(50) NULL AFTER code,
    ADD COLUMN content_hash VARCHAR(64) NULL AFTER content_version,
    ADD COLUMN genre VARCHAR(100) NULL AFTER synopsis,
    ADD COLUMN culprit_mode VARCHAR(50) NULL AFTER creator_id,
    ADD COLUMN deduction_mode VARCHAR(50) NULL AFTER culprit_mode,
    ADD COLUMN map_mode VARCHAR(50) NULL AFTER deduction_mode,
    ADD COLUMN evidence_mode VARCHAR(50) NULL AFTER map_mode,
    ADD COLUMN cover_asset_key VARCHAR(500) NULL AFTER evidence_mode,
    ADD COLUMN map_asset_key VARCHAR(500) NULL AFTER cover_asset_key;

ALTER TABLE scenarios
    ADD CONSTRAINT uk_scenarios_code_version UNIQUE (code, content_version);

ALTER TABLE scenario_locations
    ADD COLUMN code VARCHAR(100) NULL AFTER scenario_id,
    ADD COLUMN floor VARCHAR(30) NULL AFTER name,
    ADD COLUMN image_asset_key VARCHAR(500) NULL AFTER description;

ALTER TABLE suspects
    ADD COLUMN code VARCHAR(100) NULL AFTER scenario_id,
    ADD COLUMN character_type VARCHAR(50) NULL AFTER role,
    ADD COLUMN culprit_eligible TINYINT(1) NOT NULL DEFAULT 1 AFTER character_type,
    ADD COLUMN portrait_asset_key VARCHAR(500) NULL AFTER response_policy_json;

ALTER TABLE evidences
    ADD COLUMN code VARCHAR(100) NULL AFTER scenario_id,
    ADD COLUMN one_line VARCHAR(500) NULL AFTER description,
    ADD COLUMN image_asset_key VARCHAR(500) NULL AFTER image_url,
    ADD COLUMN thumbnail_asset_key VARCHAR(500) NULL AFTER image_asset_key,
    ADD COLUMN tags_json TEXT NULL AFTER thumbnail_asset_key,
    ADD COLUMN unlock_phase VARCHAR(50) NULL AFTER tags_json;

ALTER TABLE scenario_variants
    ADD COLUMN code VARCHAR(100) NULL AFTER scenario_id,
    ADD COLUMN culprit_code VARCHAR(100) NULL AFTER description,
    ADD COLUMN weight INT NOT NULL DEFAULT 1 AFTER culprit_code;

ALTER TABLE variant_solutions
    ADD COLUMN culprit_code VARCHAR(100) NULL AFTER culprit_suspect_id,
    ADD COLUMN proof_dimension_json TEXT NULL AFTER key_evidence_ids,
    ADD COLUMN final_feedback_json TEXT NULL AFTER proof_dimension_json;

CREATE TABLE IF NOT EXISTS evidence_unlock_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    evidence_code VARCHAR(100) NOT NULL,
    unlock_type VARCHAR(50) NOT NULL,
    required_phase VARCHAR(50) NULL,
    condition_json TEXT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_unlock_rules_evidence (evidence_id),
    KEY idx_evidence_unlock_rules_scenario (scenario_id),
    CONSTRAINT fk_evidence_unlock_rules_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_evidence_unlock_rules_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evidence_variant_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    variant_code VARCHAR(100) NOT NULL,
    evidence_code VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    detail_override TEXT NULL,
    detail_append TEXT NULL,
    proof_dimensions_json TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_variant_states_variant_evidence (variant_id, evidence_id),
    KEY idx_evidence_variant_states_scenario (scenario_id),
    KEY idx_evidence_variant_states_evidence (evidence_id),
    CONSTRAINT fk_evidence_variant_states_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_evidence_variant_states_variant
        FOREIGN KEY (variant_id) REFERENCES scenario_variants (id),
    CONSTRAINT fk_evidence_variant_states_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS npc_knowledge_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    suspect_id BIGINT NOT NULL,
    character_code VARCHAR(100) NOT NULL,
    public_alibi TEXT NULL,
    self_role TEXT NULL,
    direct_knowledge_json TEXT NULL,
    inferred_knowledge_json TEXT NULL,
    forbidden_knowledge_json TEXT NULL,
    stage_policies_json TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_npc_knowledge_profiles_suspect (suspect_id),
    KEY idx_npc_knowledge_profiles_scenario (scenario_id),
    CONSTRAINT fk_npc_knowledge_profiles_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_npc_knowledge_profiles_suspect
        FOREIGN KEY (suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scenario_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    asset_key VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    target_kind VARCHAR(50) NOT NULL,
    target_code VARCHAR(100) NOT NULL,
    s3_object_key VARCHAR(500) NULL,
    content_type VARCHAR(100) NULL,
    source_status VARCHAR(50) NULL,
    alt_text VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_assets_asset_key (asset_key),
    KEY idx_scenario_assets_scenario (scenario_id),
    KEY idx_scenario_assets_target (target_kind, target_code),
    CONSTRAINT fk_scenario_assets_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
