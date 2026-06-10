-- Evidence guidance UX schema migration.
-- Apply once before deploying the scenario guidance backend with prod ddl-auto=validate.
--
-- Example:
-- mysql -h <host> -u <user> -p <database> < docs/db/migrations/20260610_add_evidence_guidance_json.sql

ALTER TABLE evidences
    ADD COLUMN guidance_json TEXT NULL AFTER tags_json;
