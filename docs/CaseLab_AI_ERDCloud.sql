CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    creator_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    synopsis TEXT NULL,
    thumbnail_url VARCHAR(500) NULL,
    scenario_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    difficulty VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    player_count_min INT NOT NULL,
    player_count_max INT NOT NULL,
    estimated_play_time_minutes INT NULL,
    play_count INT NOT NULL DEFAULT 0,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count INT NOT NULL DEFAULT 0,
    price_credit INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    published_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_scenarios_creator (creator_id),
    KEY idx_scenarios_list (status, visibility, scenario_type),
    KEY idx_scenarios_difficulty (difficulty),
    KEY idx_scenarios_created_at (created_at),
    KEY idx_scenarios_play_count (play_count),
    KEY idx_scenarios_average_rating (average_rating),
    CONSTRAINT fk_scenarios_creator
        FOREIGN KEY (creator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    case_graph_json TEXT NULL,
    change_note TEXT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_versions_number (scenario_id, version_number),
    KEY idx_scenario_versions_active (scenario_id, is_active),
    CONSTRAINT fk_scenario_versions_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    map_x INT NULL,
    map_y INT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_locations_sort (scenario_id, sort_order),
    KEY idx_scenario_locations_scenario (scenario_id),
    CONSTRAINT fk_scenario_locations_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE victims (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    found_location_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    age INT NULL,
    role VARCHAR(100) NULL,
    description TEXT NULL,
    cause_of_death VARCHAR(255) NULL,
    found_condition TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_victims_scenario (scenario_id),
    KEY idx_victims_location (found_location_id),
    CONSTRAINT fk_victims_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_victims_location
        FOREIGN KEY (found_location_id) REFERENCES scenario_locations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE suspects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(100) NULL,
    relation_to_victim VARCHAR(255) NULL,
    public_profile TEXT NULL,
    public_statement TEXT NULL,
    alibi TEXT NULL,
    personality_prompt TEXT NULL,
    response_policy_json TEXT NULL,
    suspicion_level INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_suspects_sort (scenario_id, sort_order),
    KEY idx_suspects_scenario (scenario_id),
    CONSTRAINT fk_suspects_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE suspect_secrets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    suspect_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    secret_level VARCHAR(30) NOT NULL DEFAULT 'HIDDEN',
    unlock_condition_json TEXT NULL,
    is_core_secret TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_suspect_secrets_suspect (suspect_id),
    KEY idx_suspect_secrets_core (suspect_id, is_core_secret),
    CONSTRAINT fk_suspect_secrets_suspect
        FOREIGN KEY (suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE suspect_response_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    suspect_id BIGINT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    user_intent VARCHAR(100) NULL,
    required_evidence_ids JSON NULL,
    excluded_evidence_ids JSON NULL,
    presented_evidence_id BIGINT NULL,
    policy_text TEXT NOT NULL,
    allowed_facts JSON NULL,
    forbidden_facts JSON NULL,
    tone VARCHAR(100) NULL,
    priority INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_suspect_response_policies_lookup (suspect_id, condition_key, priority),
    CONSTRAINT fk_suspect_response_policies_suspect
        FOREIGN KEY (suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    location_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    evidence_type VARCHAR(50) NOT NULL DEFAULT 'SCENE',
    importance VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    image_url VARCHAR(500) NULL,
    is_initial_public TINYINT(1) NOT NULL DEFAULT 0,
    unlock_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    unlock_condition_json TEXT NULL,
    unlock_after_minutes INT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidences_sort (scenario_id, sort_order),
    KEY idx_evidences_scenario (scenario_id),
    KEY idx_evidences_location (location_id),
    KEY idx_evidences_unlock (scenario_id, is_initial_public, unlock_type),
    CONSTRAINT fk_evidences_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_evidences_location
        FOREIGN KEY (location_id) REFERENCES scenario_locations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evidence_suspects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evidence_id BIGINT NOT NULL,
    suspect_id BIGINT NOT NULL,
    relation_type VARCHAR(50) NOT NULL DEFAULT 'RELATED',
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_suspects_pair (evidence_id, suspect_id),
    KEY idx_evidence_suspects_suspect (suspect_id),
    CONSTRAINT fk_evidence_suspects_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id),
    CONSTRAINT fk_evidence_suspects_suspect
        FOREIGN KEY (suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    hint_level INT NOT NULL,
    content TEXT NOT NULL,
    unlock_after_minutes INT NULL,
    penalty_score INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hints_level (scenario_id, hint_level),
    KEY idx_hints_scenario (scenario_id),
    CONSTRAINT fk_hints_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timeline_events (
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
    created_at DATETIME NOT NULL,
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

CREATE TABLE solutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    culprit_suspect_id BIGINT NOT NULL,
    motive TEXT NOT NULL,
    method TEXT NOT NULL,
    cover_up TEXT NULL,
    full_explanation TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solutions_scenario (scenario_id),
    KEY idx_solutions_culprit (culprit_suspect_id),
    CONSTRAINT fk_solutions_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_solutions_culprit
        FOREIGN KEY (culprit_suspect_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE solution_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solution_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    reason TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_evidences_pair (solution_id, evidence_id),
    KEY idx_solution_evidences_evidence (evidence_id),
    CONSTRAINT fk_solution_evidences_solution
        FOREIGN KEY (solution_id) REFERENCES solutions (id),
    CONSTRAINT fk_solution_evidences_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    variant_type VARCHAR(30) NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_variant_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE variant_solutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    culprit_suspect_id BIGINT NOT NULL,
    culprit_name VARCHAR(100) NOT NULL,
    culprit_role VARCHAR(100) NULL,
    motive TEXT NULL,
    method TEXT NULL,
    cover_up TEXT NULL,
    full_explanation TEXT NULL,
    key_evidence_ids TEXT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_solutions_variant (variant_id),
    CONSTRAINT fk_variant_solution_variant
        FOREIGN KEY (variant_id) REFERENCES scenario_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE play_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    scenario_variant_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLAYING',
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    current_elapsed_seconds INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    grade VARCHAR(20) NULL,
    hint_count INT NOT NULL DEFAULT 0,
    interrogation_count INT NOT NULL DEFAULT 0,
    active_key VARCHAR(255) NULL UNIQUE,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_play_sessions_user_status (user_id, status, started_at),
    KEY idx_play_sessions_user_scenario (user_id, scenario_id),
    KEY idx_play_sessions_scenario (scenario_id),
    CONSTRAINT fk_play_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_play_sessions_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_play_sessions_variant
        FOREIGN KEY (scenario_variant_id) REFERENCES scenario_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE unlocked_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    play_session_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    unlocked_reason VARCHAR(255) NULL,
    unlocked_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unlocked_evidences_pair (play_session_id, evidence_id),
    KEY idx_unlocked_evidences_evidence (evidence_id),
    CONSTRAINT fk_unlocked_evidences_session
        FOREIGN KEY (play_session_id) REFERENCES play_sessions (id),
    CONSTRAINT fk_unlocked_evidences_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE used_hints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    play_session_id BIGINT NOT NULL,
    hint_id BIGINT NOT NULL,
    used_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_used_hints_pair (play_session_id, hint_id),
    KEY idx_used_hints_hint (hint_id),
    CONSTRAINT fk_used_hints_session
        FOREIGN KEY (play_session_id) REFERENCES play_sessions (id),
    CONSTRAINT fk_used_hints_hint
        FOREIGN KEY (hint_id) REFERENCES hints (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE interrogation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    play_session_id BIGINT NOT NULL,
    suspect_id BIGINT NOT NULL,
    presented_evidence_id BIGINT NULL,
    question_type VARCHAR(50) NOT NULL DEFAULT 'FREE',
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    ai_model VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_interrogation_logs_session (play_session_id, created_at),
    KEY idx_interrogation_logs_session_suspect (play_session_id, suspect_id, created_at),
    KEY idx_interrogation_logs_suspect (suspect_id),
    KEY idx_interrogation_logs_evidence (presented_evidence_id),
    CONSTRAINT fk_interrogation_logs_session
        FOREIGN KEY (play_session_id) REFERENCES play_sessions (id),
    CONSTRAINT fk_interrogation_logs_suspect
        FOREIGN KEY (suspect_id) REFERENCES suspects (id),
    CONSTRAINT fk_interrogation_logs_evidence
        FOREIGN KEY (presented_evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE final_deductions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    play_session_id BIGINT NOT NULL,
    selected_culprit_id BIGINT NULL,
    motive_text TEXT NULL,
    method_text TEXT NULL,
    cover_up_text TEXT NULL,
    score INT NOT NULL DEFAULT 0,
    grade VARCHAR(20) NULL,
    feedback TEXT NULL,
    matched_parts JSON NULL,
    missed_parts JSON NULL,
    submitted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_final_deductions_session (play_session_id),
    KEY idx_final_deductions_culprit (selected_culprit_id),
    CONSTRAINT fk_final_deductions_session
        FOREIGN KEY (play_session_id) REFERENCES play_sessions (id),
    CONSTRAINT fk_final_deductions_culprit
        FOREIGN KEY (selected_culprit_id) REFERENCES suspects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE final_deduction_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    final_deduction_id BIGINT NOT NULL,
    evidence_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_final_deduction_evidences_pair (final_deduction_id, evidence_id),
    KEY idx_final_deduction_evidences_evidence (evidence_id),
    CONSTRAINT fk_final_deduction_evidences_deduction
        FOREIGN KEY (final_deduction_id) REFERENCES final_deductions (id),
    CONSTRAINT fk_final_deduction_evidences_evidence
        FOREIGN KEY (evidence_id) REFERENCES evidences (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT NULL,
    is_spoiler TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_reviews_user_scenario (user_id, scenario_id),
    KEY idx_scenario_reviews_scenario (scenario_id, created_at),
    CONSTRAINT fk_scenario_reviews_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_bookmarks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_bookmarks_user_scenario (user_id, scenario_id),
    KEY idx_scenario_bookmarks_scenario (scenario_id),
    CONSTRAINT fk_scenario_bookmarks_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_bookmarks_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_scenario_reports_scenario (scenario_id, status),
    KEY idx_scenario_reports_reporter (reporter_id),
    CONSTRAINT fk_scenario_reports_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_tags_pair (scenario_id, tag_id),
    KEY idx_scenario_tags_tag (tag_id),
    CONSTRAINT fk_scenario_tags_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_validation_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    validation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    validation_score INT NULL,
    problem_summary TEXT NULL,
    suggestion TEXT NULL,
    check_items_json JSON NULL,
    checked_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_scenario_validation_results_scenario (scenario_id, checked_at),
    CONSTRAINT fk_scenario_validation_results_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_generation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    scenario_id BIGINT NULL,
    request_type VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NULL,
    prompt_summary TEXT NULL,
    result_status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    cost_credit INT NOT NULL DEFAULT 0,
    latency_ms BIGINT NULL,
    error_code VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_generation_logs_user (user_id, created_at),
    KEY idx_ai_generation_logs_scenario (scenario_id, created_at),
    KEY idx_ai_generation_logs_type (request_type, result_status, created_at),
    CONSTRAINT fk_ai_generation_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_generation_logs_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE credit_wallets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credit_wallets_user (user_id),
    CONSTRAINT fk_credit_wallets_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE credit_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    amount INT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    balance_after INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_credit_transactions_user (user_id, created_at),
    KEY idx_credit_transactions_wallet (wallet_id, created_at),
    CONSTRAINT fk_credit_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES credit_wallets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_purchases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    price_credit INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    purchased_at DATETIME NOT NULL,
    refunded_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_purchases_user_scenario (user_id, scenario_id),
    KEY idx_scenario_purchases_scenario (scenario_id),
    CONSTRAINT fk_scenario_purchases_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_scenario_purchases_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scenario_accesses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    access_type VARCHAR(50) NOT NULL,
    purchase_id BIGINT NULL,
    granted_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_accesses_user_scenario (user_id, scenario_id),
    KEY idx_scenario_accesses_scenario (scenario_id),
    KEY idx_scenario_accesses_purchase (purchase_id),
    CONSTRAINT fk_scenario_accesses_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_scenario_accesses_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_accesses_purchase
        FOREIGN KEY (purchase_id) REFERENCES scenario_purchases (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

