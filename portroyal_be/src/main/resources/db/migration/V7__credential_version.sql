ALTER TABLE users
    ADD COLUMN credential_version bigint NOT NULL DEFAULT 0;
