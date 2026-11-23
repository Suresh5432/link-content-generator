CREATE TABLE website_analyses (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    original_url TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    domain VARCHAR(255),
    title VARCHAR(1000),
    status VARCHAR(40) NOT NULL,
    risk_level VARCHAR(20),
    confidence INTEGER,
    category VARCHAR(100),
    report TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_website_analyses_user_created ON website_analyses(user_id, created_at DESC);

CREATE TABLE website_pages (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES website_analyses(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    canonical_url TEXT,
    title VARCHAR(1000),
    status_code INTEGER,
    content_type VARCHAR(100),
    depth INTEGER,
    safety_notes TEXT,
    content_excerpt TEXT,
    fetched_at TIMESTAMPTZ,
    UNIQUE (analysis_id, url)
);

CREATE TABLE website_findings (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES website_analyses(id) ON DELETE CASCADE,
    page_id UUID REFERENCES website_pages(id) ON DELETE CASCADE,
    type VARCHAR(60) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    evidence TEXT,
    confidence VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_website_findings_analysis ON website_findings(analysis_id);
