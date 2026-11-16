CREATE TABLE extraction_outbox_events (
                                          id UUID PRIMARY KEY,
                                          aggregate_id UUID NOT NULL,
                                          event_type VARCHAR(150) NOT NULL,
                                          payload JSONB NOT NULL,

                                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          published_at TIMESTAMPTZ,

                                          attempts INTEGER NOT NULL DEFAULT 0,
                                          last_error TEXT,

                                          CONSTRAINT chk_extraction_outbox_attempts
                                              CHECK (attempts >= 0)
);

CREATE INDEX idx_extraction_outbox_unpublished
    ON extraction_outbox_events (available_at, created_at)
    WHERE published_at IS NULL;