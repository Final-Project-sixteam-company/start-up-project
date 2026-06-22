-- Schema migration for Android FCM device-token registration.
--
-- Apply before deploying the DeviceToken entity:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260622_add_device_tokens_schema.sql
--
-- Rollback, only after backup/approval:
-- DROP TABLE device_tokens;

CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    token VARCHAR(512) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_tokens_token (token),
    KEY idx_device_tokens_user_active (user_id, is_active),
    KEY idx_device_tokens_last_used_at (last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
