-- Expand tracked websites
ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS from_analysis_id UUID;
ALTER TABLE tracked_websites ADD COLUMN IF NOT EXISTS first_recorded_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_tracked_websites_user_domain ON tracked_websites(user_id, domain);

-- Expand personal data inventory
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS confirmed_by_user BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS observed_on_website BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS declared_in_policy BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS inferred_by_ai BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS date_shared DATE;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS evidence_source VARCHAR(120);
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMPTZ;
ALTER TABLE personal_data_records ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_personal_data_user_website ON personal_data_records(user_id, tracked_website_id);

-- Expand privacy requests
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS privacy_contact_url TEXT;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS privacy_contact_email VARCHAR(320);
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS data_categories TEXT;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS preferred_contact VARCHAR(320);
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS legal_basis VARCHAR(255);
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS submission_method VARCHAR(40);
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS submission_evidence TEXT;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS expected_response_date DATE;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS website_response TEXT;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS user_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS website_claimed_deletion BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS reminder_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS last_reminder_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE privacy_requests ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_privacy_requests_user_status ON privacy_requests(user_id, status);
CREATE INDEX IF NOT EXISTS idx_privacy_requests_reminder ON privacy_requests(status, reminder_at);

-- Website account records
CREATE TABLE IF NOT EXISTS user_website_accounts (
    id UUID PRIMARY KEY,
    tracked_website_id UUID NOT NULL REFERENCES tracked_websites(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    account_identifier VARCHAR(320),
    account_creation_date DATE,
    account_category VARCHAR(100),
    shared_information TEXT,
    account_status VARCHAR(40),
    discovery_source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_website_accounts_user ON user_website_accounts(user_id, tracked_website_id);

-- Cookie inventory (metadata only; never values)
CREATE TABLE IF NOT EXISTS cookie_inventory (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tracked_website_id UUID REFERENCES tracked_websites(id) ON DELETE SET NULL,
    browser_integration_id UUID,
    cookie_name VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    first_party BOOLEAN NOT NULL DEFAULT FALSE,
    expiration TIMESTAMPTZ,
    purpose VARCHAR(255),
    category VARCHAR(40) NOT NULL,
    secure BOOLEAN NOT NULL DEFAULT FALSE,
    http_only BOOLEAN NOT NULL DEFAULT FALSE,
    same_site VARCHAR(20),
    tracking_related BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(40) NOT NULL DEFAULT 'BROWSER_EXTENSION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cookie_inventory_user_domain ON cookie_inventory(user_id, domain);
CREATE INDEX IF NOT EXISTS idx_cookie_inventory_user_category ON cookie_inventory(user_id, category);

-- Privacy request evidence
CREATE TABLE IF NOT EXISTS privacy_request_evidence (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES privacy_requests(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL,
    description TEXT,
    reference_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_privacy_request_evidence_request ON privacy_request_evidence(request_id, created_at);

-- Privacy request history
CREATE TABLE IF NOT EXISTS privacy_request_history (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES privacy_requests(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_privacy_request_history_request ON privacy_request_history(request_id, created_at);

-- Website privacy contacts discovered from analysis
CREATE TABLE IF NOT EXISTS website_privacy_contacts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tracked_website_id UUID REFERENCES tracked_websites(id) ON DELETE CASCADE,
    privacy_policy_url TEXT,
    deletion_policy_url TEXT,
    access_request_url TEXT,
    account_deletion_url TEXT,
    cookie_management_url TEXT,
    consent_management_url TEXT,
    privacy_contact_email VARCHAR(320),
    dpo_contact TEXT,
    source_urls TEXT,
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_website_privacy_contacts UNIQUE (user_id, tracked_website_id)
);

-- User privacy preferences
CREATE TABLE IF NOT EXISTS user_privacy_preferences (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    analytics_cookies BOOLEAN NOT NULL DEFAULT FALSE,
    advertising_cookies BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_communications BOOLEAN NOT NULL DEFAULT FALSE,
    third_party_tracking BOOLEAN NOT NULL DEFAULT FALSE,
    personalized_advertising BOOLEAN NOT NULL DEFAULT FALSE,
    data_sharing BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Browser integrations (metadata only)
CREATE TABLE IF NOT EXISTS browser_integrations (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    installation_id VARCHAR(255) NOT NULL,
    browser_name VARCHAR(80),
    extension_version VARCHAR(40),
    device_label VARCHAR(120),
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_browser_integrations UNIQUE (user_id, installation_id)
);

-- Browser sync history
CREATE TABLE IF NOT EXISTS browser_sync_history (
    id UUID PRIMARY KEY,
    integration_id UUID NOT NULL REFERENCES browser_integrations(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    cookie_count INTEGER NOT NULL DEFAULT 0,
    domains_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_browser_sync_history_integration ON browser_sync_history(integration_id, created_at DESC);
