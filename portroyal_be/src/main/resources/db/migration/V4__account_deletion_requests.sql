ALTER TABLE users
    ADD COLUMN deletion_requested_at timestamp(6);

CREATE INDEX idx_users_deletion_requested_at
    ON users (deletion_requested_at)
    WHERE deletion_requested_at IS NOT NULL;

CREATE INDEX idx_matches_active_host
    ON matches (host_user_id)
    WHERE ended = false;

CREATE INDEX idx_match_user_user_match
    ON match_user (user_id, match_id);
