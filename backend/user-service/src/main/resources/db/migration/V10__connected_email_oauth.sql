ALTER TABLE connected_email_accounts
    ADD COLUMN IF NOT EXISTS refresh_token_ciphertext BYTEA,
    ADD COLUMN IF NOT EXISTS refresh_token_iv BYTEA;

ALTER TABLE connected_email_discoveries
    ADD COLUMN IF NOT EXISTS dismissed_by_user BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS connected_email_oauth_states (
    state_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    verifier_ciphertext BYTEA NOT NULL,
    verifier_iv BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_connected_email_oauth_states_expiry
    ON connected_email_oauth_states(expires_at);
