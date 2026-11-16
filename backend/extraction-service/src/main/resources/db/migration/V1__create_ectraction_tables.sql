CREATE TABLE extracted_contents (
                                    id UUID PRIMARY KEY,

                                    link_id UUID NOT NULL,
                                    user_id VARCHAR(255) NOT NULL,

                                    source_url TEXT NOT NULL,
                                    canonical_url TEXT,

                                    title VARCHAR(1000),
                                    description TEXT,

                                    content_type VARCHAR(100),
                                    language VARCHAR(20),

                                    extracted_text TEXT NOT NULL,
                                    word_count INTEGER NOT NULL DEFAULT 0,

                                    fetched_at TIMESTAMPTZ NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    version BIGINT NOT NULL DEFAULT 0,

                                    CONSTRAINT chk_extracted_word_count
                                        CHECK (word_count >= 0),

                                    CONSTRAINT uk_extracted_contents_link
                                        UNIQUE (link_id)
);

CREATE INDEX idx_extracted_contents_user
    ON extracted_contents(user_id, created_at DESC);


CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,

                                  event_type VARCHAR(150) NOT NULL,

                                  processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);