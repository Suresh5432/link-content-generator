CREATE TABLE IF NOT EXISTS connected_email_accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    account_email VARCHAR(320),
    scopes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DISABLED',
    consented_at TIMESTAMPTZ,
    last_scan_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_connected_email_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE IF NOT EXISTS connected_email_discoveries (
    id UUID PRIMARY KEY,
    connected_account_id UUID NOT NULL REFERENCES connected_email_accounts(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    evidence_type VARCHAR(60) NOT NULL,
    evidence_source TEXT,
    first_seen_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    confidence VARCHAR(20),
    confirmed_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_connected_email_accounts_user ON connected_email_accounts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_connected_email_discoveries_user ON connected_email_discoveries(user_id, domain);
