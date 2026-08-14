-- V3__email_otp_failed_attempts.sql
-- Track failed verification attempts (FR-009) for max-attempts enforcement.
-- OTP codes are stored as plaintext (no at-rest encryption in this phase).

ALTER TABLE email_otps
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;