ALTER TABLE privacy_event_outbox ADD COLUMN IF NOT EXISTS user_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_privacy_event_outbox_user_created ON privacy_event_outbox (user_id, created_at DESC);
