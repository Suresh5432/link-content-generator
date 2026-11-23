CREATE TABLE IF NOT EXISTS privacy_event_outbox (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255),
    exchange VARCHAR(120) NOT NULL,
    routing_key VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    last_error VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_privacy_event_outbox_pending
    ON privacy_event_outbox (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_privacy_event_outbox_user_created
    ON privacy_event_outbox (user_id, created_at DESC);
