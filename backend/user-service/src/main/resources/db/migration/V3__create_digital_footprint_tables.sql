CREATE TABLE tracked_websites (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    category VARCHAR(100),
    account_status VARCHAR(40),
    privacy_policy_url TEXT,
    deletion_policy_url TEXT,
    risk_level VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tracked_websites_user ON tracked_websites(user_id, created_at DESC);

CREATE TABLE personal_data_records (
    id UUID PRIMARY KEY,
    tracked_website_id UUID NOT NULL REFERENCES tracked_websites(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    data_category VARCHAR(100) NOT NULL,
    data_type VARCHAR(100) NOT NULL,
    description TEXT,
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE privacy_requests (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tracked_website_id UUID REFERENCES tracked_websites(id) ON DELETE SET NULL,
    request_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    request_message TEXT,
    external_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
