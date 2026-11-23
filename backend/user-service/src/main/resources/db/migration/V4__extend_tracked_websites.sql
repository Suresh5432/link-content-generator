ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS last_inspection_at TIMESTAMPTZ;
ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS data_categories TEXT;
ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS tracking_technologies TEXT;
ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS deletion_request_status VARCHAR(40);
