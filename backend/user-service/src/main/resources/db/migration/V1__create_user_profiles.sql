CREATE TABLE user_profiles (
                               user_id VARCHAR(255) PRIMARY KEY,
                               display_name VARCHAR(80),
                               bio VARCHAR(500),
                               avatar_url VARCHAR(2048),
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_profiles_updated_at
    ON user_profiles(updated_at);