CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200),
    client_secret_expires_at timestamp,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000),
    post_logout_redirect_uris varchar(1000),
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_oauth2_registered_client_client_id UNIQUE (client_id)
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
    authorization_code_issued_at timestamp,
    authorization_code_expires_at timestamp,
    authorization_code_metadata text,
    access_token_value text,
    access_token_issued_at timestamp,
    access_token_expires_at timestamp,
    access_token_metadata text,
    access_token_type varchar(100),
    access_token_scopes varchar(1000),
    oidc_id_token_value text,
    oidc_id_token_issued_at timestamp,
    oidc_id_token_expires_at timestamp,
    oidc_id_token_metadata text,
    refresh_token_value text,
    refresh_token_issued_at timestamp,
    refresh_token_expires_at timestamp,
    refresh_token_metadata text,
    user_code_value text,
    user_code_issued_at timestamp,
    user_code_expires_at timestamp,
    user_code_metadata text,
    device_code_value text,
    device_code_issued_at timestamp,
    device_code_expires_at timestamp,
    device_code_metadata text,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE INDEX idx_oauth2_authorization_principal_name
    ON oauth2_authorization (principal_name);
