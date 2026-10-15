CREATE TABLE callback_secrets (
    match_key_code varchar(255) NOT NULL,
    user_id bigint NOT NULL,
    encrypted_secret text NOT NULL,
    CONSTRAINT pk_callback_secrets PRIMARY KEY (match_key_code, user_id)
);
