CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp with time zone DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    CONSTRAINT pk_oauth2_registered_client PRIMARY KEY (id),
    CONSTRAINT uk_oauth2_registered_client_client_id UNIQUE (client_id)
);

CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000),
    attributes text,
    state varchar(500),
    authorization_code_value text,
    authorization_code_issued_at timestamp with time zone,
    authorization_code_expires_at timestamp with time zone,
    authorization_code_metadata text,
    access_token_value text,
    access_token_issued_at timestamp with time zone,
    access_token_expires_at timestamp with time zone,
    access_token_metadata text,
    access_token_type varchar(100),
    access_token_scopes varchar(1000),
    oidc_id_token_value text,
    oidc_id_token_issued_at timestamp with time zone,
    oidc_id_token_expires_at timestamp with time zone,
    oidc_id_token_metadata text,
    refresh_token_value text,
    refresh_token_issued_at timestamp with time zone,
    refresh_token_expires_at timestamp with time zone,
    refresh_token_metadata text,
    user_code_value text,
    user_code_issued_at timestamp with time zone,
    user_code_expires_at timestamp with time zone,
    user_code_metadata text,
    device_code_value text,
    device_code_issued_at timestamp with time zone,
    device_code_expires_at timestamp with time zone,
    device_code_metadata text,
    CONSTRAINT pk_oauth2_authorization PRIMARY KEY (id)
);

CREATE INDEX ix_oauth2_authorization_state
    ON oauth2_authorization (state);
CREATE INDEX ix_oauth2_authorization_authorization_code
    ON oauth2_authorization (authorization_code_value);
CREATE INDEX ix_oauth2_authorization_access_token
    ON oauth2_authorization (access_token_value);
CREATE INDEX ix_oauth2_authorization_oidc_id_token
    ON oauth2_authorization (oidc_id_token_value);
CREATE INDEX ix_oauth2_authorization_refresh_token
    ON oauth2_authorization (refresh_token_value);
CREATE INDEX ix_oauth2_authorization_user_code
    ON oauth2_authorization (user_code_value);
CREATE INDEX ix_oauth2_authorization_device_code
    ON oauth2_authorization (device_code_value);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    CONSTRAINT pk_oauth2_authorization_consent
        PRIMARY KEY (registered_client_id, principal_name)
);
