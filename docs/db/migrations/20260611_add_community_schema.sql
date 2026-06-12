-- ==============================================================================
-- Migration for Community Domain tables (bookmarks, reviews, reports)
-- ==============================================================================
-- [적용 방법]
-- mysql> source 20260611_add_community_schema.sql;
--
-- [롤백 방법]
-- DROP TABLE IF EXISTS scenario_bookmarks;
-- DROP TABLE IF EXISTS scenario_reviews;
-- DROP TABLE IF EXISTS scenario_reports;
--
-- [주의 사항: 이미 구버전 테이블이 생성된 환경을 위한 ALTER 안내]
-- CREATE TABLE IF NOT EXISTS 이므로, 기존 환경에서는 제약조건(FK)이 추가되지 않고 넘어갈 수 있습니다.
-- ddl-auto=validate 환경에서 FK 누락 시 아래 ALTER 문을 수동 실행해 주세요.
-- 
-- ALTER TABLE scenario_bookmarks ADD CONSTRAINT fk_scenario_bookmarks_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios(id);
-- ALTER TABLE scenario_bookmarks ADD CONSTRAINT fk_scenario_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id);
-- ALTER TABLE scenario_reviews ADD CONSTRAINT fk_scenario_reviews_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios(id);
-- ALTER TABLE scenario_reviews ADD CONSTRAINT fk_scenario_reviews_user FOREIGN KEY (user_id) REFERENCES users(id);
-- ALTER TABLE scenario_reports ADD CONSTRAINT fk_scenario_reports_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios(id);
-- ALTER TABLE scenario_reports ADD CONSTRAINT fk_scenario_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id);
-- ==============================================================================
CREATE TABLE IF NOT EXISTS scenario_bookmarks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario_bookmarks_user_scenario (user_id, scenario_id),
    CONSTRAINT fk_scenario_bookmarks_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id)
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
    KEY idx_scenario_reviews_scenario (scenario_id, created_at),
    CONSTRAINT fk_scenario_reviews_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_reviews_user FOREIGN KEY (user_id) REFERENCES users (id)
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
    KEY idx_scenario_reports_reporter (reporter_id),
    CONSTRAINT fk_scenario_reports_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios (id),
    CONSTRAINT fk_scenario_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
