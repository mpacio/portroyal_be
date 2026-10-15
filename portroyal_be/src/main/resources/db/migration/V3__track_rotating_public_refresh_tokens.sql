CREATE TABLE oauth2_refresh_token_history (
    token_hash char(64) NOT NULL,
    authorization_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    client_id varchar(100) NOT NULL,
    status varchar(16) NOT NULL,
    issued_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_oauth2_refresh_token_history PRIMARY KEY (token_hash)
);

CREATE INDEX ix_oauth2_refresh_token_family
    ON oauth2_refresh_token_history (authorization_id);
