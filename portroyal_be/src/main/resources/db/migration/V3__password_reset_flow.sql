ALTER TABLE temporary_tokens
    DROP CONSTRAINT temporary_tokens_flow_type_check;

ALTER TABLE temporary_tokens
    ADD CONSTRAINT temporary_tokens_flow_type_check
        CHECK (flow_type IN ('EMAIL_CONFIRMATION', 'TELEGRAM_UNIFICATION', 'PASSWORD_RESET'));

CREATE INDEX idx_temporary_tokens_flow_token
    ON temporary_tokens (flow_type, token);

CREATE INDEX idx_temporary_tokens_email_flow
    ON temporary_tokens (email, flow_type);
