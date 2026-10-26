CREATE TABLE api_rate_limit_buckets (
    bucket_key varchar(64) NOT NULL,
    operation varchar(64) NOT NULL,
    scope_hash varchar(64) NOT NULL,
    window_started_at timestamptz NOT NULL,
    request_count integer NOT NULL CHECK (request_count > 0),
    expires_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (bucket_key)
);

CREATE INDEX idx_api_rate_limit_buckets_expiry
    ON api_rate_limit_buckets (expires_at);
