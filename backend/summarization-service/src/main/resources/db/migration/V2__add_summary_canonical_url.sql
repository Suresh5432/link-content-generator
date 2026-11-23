ALTER TABLE summaries ADD COLUMN IF NOT EXISTS canonical_url TEXT;
CREATE INDEX IF NOT EXISTS idx_summaries_canonical_url ON summaries(canonical_url);
