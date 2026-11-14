ALTER TABLE links
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_links_active_user_created
    ON links (user_id, created_at DESC)
    WHERE deleted_at IS NULL;
