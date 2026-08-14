-- V1__outbox_table.sql
-- Transactional outbox table for reliable event publishing (publish-only service).

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

-- Index for outbox relay polling (ordered by created_at for ordering guarantees)
CREATE INDEX idx_outbox_status_created_at
    ON outbox_messages (status, created_at);

-- Index for finding messages by aggregate
CREATE INDEX idx_outbox_aggregate_id
    ON outbox_messages (aggregate_id);

-- Index for finding messages by topic
CREATE INDEX idx_outbox_topic
    ON outbox_messages (topic);

-- Index for cleanup queries
CREATE INDEX idx_outbox_published_at
    ON outbox_messages (published_at)
    WHERE published_at IS NOT NULL;