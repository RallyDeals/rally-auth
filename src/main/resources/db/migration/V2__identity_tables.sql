-- V2__identity_tables.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
    stripe_customer_id VARCHAR(255),
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
-- email_otps  (6-digit OTP for email verification + password reset)
-- =====================================================================

CREATE TABLE email_otps (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    purpose     VARCHAR(20) NOT NULL
                    CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    otp         VARCHAR(6) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_otps_email ON email_otps (email);
CREATE INDEX idx_email_otps_expires ON email_otps (expires_at);