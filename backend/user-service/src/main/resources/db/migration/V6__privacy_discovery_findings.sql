CREATE TABLE IF NOT EXISTS privacy_discovery_findings (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    finding_type VARCHAR(60) NOT NULL,
    name VARCHAR(255),
    source_url TEXT,
    description TEXT,
    confidence VARCHAR(20),
    analysis_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_privacy_discovery_finding UNIQUE (user_id, domain, finding_type, source_url)
);

CREATE INDEX IF NOT EXISTS idx_privacy_discovery_user_domain
    ON privacy_discovery_findings (user_id, domain);
