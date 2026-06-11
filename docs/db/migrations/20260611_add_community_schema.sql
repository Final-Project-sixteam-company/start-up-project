-- Migration for Community Domain tables (bookmarks, reviews, reports)

CREATE TABLE IF NOT EXISTS scenario_bookmarks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_bookmarks_user_scenario (user_id, scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scenario_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT NULL,
    is_spoiler TINYINT(1) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_reviews_user_scenario (user_id, scenario_id),
    KEY idx_scenario_reviews_scenario (scenario_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scenario_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    detail TEXT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_reports_reporter_scenario (reporter_id, scenario_id),
    KEY idx_scenario_reports_scenario (scenario_id, status),
    KEY idx_scenario_reports_reporter (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
