-- V1__initial_schema.sql
-- Consolidated initial schema: transactional outbox, identity tables, and OTP codes.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- outbox_messages  (transactional outbox for reliable event publishing)
-- =====================================================================

CREATE TABLE outbox_messages (
    message_id      UUID NOT NULL PRIMARY KEY,
    aggregate_id    UUID NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    message_key     VARCHAR(255),
    message_type    VARCHAR(100) NOT NULL,
    correlation_id  UUID,
    causation_id    VARCHAR(255),
    trace_id        VARCHAR(64),
    payload         JSONB NOT NULL,
    headers         JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER NOT NULL DEFAULT 0,
    max_retries     INTEGER NOT NULL DEFAULT 5,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    last_error      TEXT
);

CREATE INDEX idx_outbox_status_created_at
    ON outbox_messages (status, created_at);

CREATE INDEX idx_outbox_aggregate_id
    ON outbox_messages (aggregate_id);

CREATE INDEX idx_outbox_topic
    ON outbox_messages (topic);

CREATE INDEX idx_outbox_published_at
    ON outbox_messages (published_at)
    WHERE published_at IS NOT NULL;

-- =====================================================================
-- users
-- =====================================================================

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    phone_number      VARCHAR(30),
    profile_picture   VARCHAR(1000),
    role              VARCHAR(10) NOT NULL DEFAULT 'BUYER'
                          CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),
    enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- =====================================================================
-- tokens  (refresh tokens only, stored hashed)
-- =====================================================================

CREATE TABLE tokens (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    token            VARCHAR(255) NOT NULL,
    expiration_date  TIMESTAMPTZ NOT NULL,
    revocation_date  TIMESTAMPTZ NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_tokens_token ON tokens (token);
CREATE INDEX idx_tokens_user_id ON tokens (user_id);

-- =====================================================================
-- email_otps  (plaintext OTP for email verification + password reset)
-- =====================================================================

CREATE TABLE email_otps (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL,
    purpose          VARCHAR(20) NOT NULL
                         CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    otp              VARCHAR(6) NOT NULL,
    failed_attempts  INTEGER NOT NULL DEFAULT 0,
    expires_at       TIMESTAMPTZ NOT NULL,
    used_at          TIMESTAMPTZ NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_otps_email ON email_otps (email);
CREATE INDEX idx_email_otps_expires ON email_otps (expires_at);