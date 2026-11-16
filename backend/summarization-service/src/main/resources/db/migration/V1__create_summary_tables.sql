CREATE TABLE summaries (
                           id UUID PRIMARY KEY,

                           link_id UUID NOT NULL UNIQUE,
                           content_id UUID NOT NULL,
                           user_id VARCHAR(255) NOT NULL,

                           title VARCHAR(1000),
                           summary TEXT NOT NULL,

                           key_takeaways JSONB NOT NULL DEFAULT '[]'::jsonb,

                           reading_time_minutes INTEGER NOT NULL DEFAULT 1,
                           word_count INTEGER NOT NULL DEFAULT 0,

                           ai_provider VARCHAR(100) NOT NULL,
                           ai_model VARCHAR(150),

                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT chk_summary_reading_time
                               CHECK (reading_time_minutes >= 1),

                           CONSTRAINT chk_summary_word_count
                               CHECK (word_count >= 0)
);

CREATE INDEX idx_summaries_user_created
    ON summaries(user_id, created_at DESC);


CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  event_type VARCHAR(150) NOT NULL,
                                  processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE summary_outbox_events (
                                       id UUID PRIMARY KEY,

                                       aggregate_id UUID NOT NULL,
                                       event_type VARCHAR(150) NOT NULL,
                                       payload JSONB NOT NULL,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       published_at TIMESTAMPTZ,

                                       attempts INTEGER NOT NULL DEFAULT 0,
                                       last_error TEXT,

                                       CONSTRAINT chk_summary_outbox_attempts
                                           CHECK (attempts >= 0)
);

CREATE INDEX idx_summary_outbox_unpublished
    ON summary_outbox_events(available_at, created_at)
    WHERE published_at IS NULL;