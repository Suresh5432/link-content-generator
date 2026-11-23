ALTER TABLE notifications ALTER COLUMN link_id DROP NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS request_id UUID;
CREATE INDEX IF NOT EXISTS idx_notifications_user_request ON notifications(user_id, request_id);
