CREATE TABLE auth_login_attempts (
    attempt_key char(64) NOT NULL,
    attempt_count integer NOT NULL,
    window_started_at timestamp with time zone NOT NULL,
    blocked_until timestamp with time zone,
    CONSTRAINT pk_auth_login_attempts PRIMARY KEY (attempt_key)
);

CREATE INDEX ix_auth_login_attempts_window
    ON auth_login_attempts (window_started_at);
