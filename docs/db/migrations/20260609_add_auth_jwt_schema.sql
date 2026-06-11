-- Schema migration for auth/JWT foundation.
--
-- Apply before deploying the auth API:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260609_add_auth_jwt_schema.sql
--
-- Rollback, only if no auth traffic has been accepted or after backup/approval:
-- DROP TABLE auth_refresh_tokens;
-- DROP TABLE user_oauth_accounts;
-- DROP TABLE users;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NULL,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_oauth_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL,
    email VARCHAR(255) NULL,
    nickname VARCHAR(100) NULL,
    profile_image_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_oauth_provider_user (provider, provider_user_id),
    UNIQUE KEY uk_user_oauth_user_provider (user_id, provider),
    KEY idx_user_oauth_user (user_id),
    CONSTRAINT fk_user_oauth_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    device_id VARCHAR(100) NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    rotated_from_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_refresh_tokens_hash (token_hash),
    KEY idx_auth_refresh_tokens_user (user_id),
    KEY idx_auth_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_auth_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
