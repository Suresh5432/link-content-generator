ALTER TABLE user_profiles
    ADD COLUMN email VARCHAR(320);

CREATE INDEX idx_user_profiles_email
    ON user_profiles(email);