
-- Main links table
CREATE TABLE links (
                       id UUID PRIMARY KEY,

                       user_id VARCHAR(255) NOT NULL,

                       original_url TEXT NOT NULL,
                       normalized_url TEXT NOT NULL,

                       status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',

                       failure_code VARCHAR(100),

                       idempotency_key VARCHAR(128),

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       completed_at TIMESTAMPTZ,

                       version BIGINT NOT NULL DEFAULT 0,

                       CONSTRAINT chk_link_status CHECK (
                           status IN (
                                      'SUBMITTED',
                                      'PENDING_EXTRACTION',
                                      'EXTRACTING',
                                      'EXTRACTED',
                                      'SUMMARIZING',
                                      'COMPLETED',
                                      'EXTRACTION_FAILED',
                                      'SUMMARIZATION_FAILED',
                                      'FAILED'
                               )
                           )
);

-- User link history
CREATE INDEX idx_links_user_created
    ON links (user_id, created_at DESC);

-- Find links by processing status
CREATE INDEX idx_links_status
    ON links (status);

-- Prevent duplicate requests when a client retries
CREATE UNIQUE INDEX uq_links_user_idempotency
    ON links (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;


-- Transactional Outbox table
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,

                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,

                               event_type VARCHAR(150) NOT NULL,

                               payload JSONB NOT NULL,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMPTZ,

                               attempts INTEGER NOT NULL DEFAULT 0,
                               last_error TEXT,

                               CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0)
);

-- Efficiently find events waiting to be published
CREATE INDEX idx_outbox_unpublished
    ON outbox_events (available_at, created_at)
    WHERE published_at IS NULL;

-- Find events belonging to a particular link
CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id);
