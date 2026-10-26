CREATE SEQUENCE ai_players_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE emails_queue_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE game_configurations_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE matches_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE moves_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE temporary_tokens_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE users_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ai_players (
    created_at timestamp(6) NOT NULL,
    id bigint NOT NULL,
    match_id bigint NOT NULL,
    difficulty varchar(255) NOT NULL CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
    display_name varchar(255),
    username varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE callbacks (
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6),
    user_id bigint NOT NULL,
    match_key_code varchar(255) NOT NULL,
    secret varchar(255),
    url varchar(255) NOT NULL,
    PRIMARY KEY (user_id, match_key_code)
);

CREATE TABLE emails_queue (
    sent boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    id bigint NOT NULL,
    sent_at timestamp(6),
    updated_at timestamp(6),
    html_body TEXT,
    recipient_email varchar(255) NOT NULL,
    recipient_name varchar(255) NOT NULL,
    subject varchar(255) NOT NULL,
    text_body TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE game_configurations (
    config_id integer,
    id integer NOT NULL,
    config_name varchar(255),
    prop_name varchar(255) UNIQUE,
    prop_value varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE match_user (
    match_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE matches (
    configuration_id integer NOT NULL,
    ended boolean NOT NULL,
    started boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    ended_at timestamp(6),
    host_user_id bigint NOT NULL,
    id bigint NOT NULL,
    last_move_at timestamp(6),
    started_at timestamp(6),
    updated_at timestamp(6),
    winner_user_id bigint,
    key_code varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE matches_moves (
    match_entity_id bigint NOT NULL,
    moves_id bigint NOT NULL UNIQUE
);

CREATE TABLE moves (
    active_player_index integer,
    configuration_id integer,
    parameter_index integer,
    pick_player_index integer,
    running_player_index integer,
    time_index integer,
    created_at timestamp(6) NOT NULL,
    id bigint NOT NULL,
    match_id bigint NOT NULL,
    updated_at timestamp(6),
    active_player_username varchar(255),
    lib_version varchar(255),
    move_name varchar(255) CHECK (move_name IN (
        'DISCOVER','REPEL','ACCEPT','FINISH_DISCOVER','TRADE_HIRE','TRADE','HIRE',
        'TRADE_RENOUNCE','END_TURN','COMMIT_EXPEDITION','SIGN_CONTRACT'
    )),
    notes varchar(255),
    running_player_username varchar(255),
    expedition_employees_list varchar(255)[],
    PRIMARY KEY (id)
);

CREATE TABLE temporary_tokens (
    failed_attempts integer,
    confirmed_at timestamp(6),
    created_at timestamp(6) NOT NULL,
    expires_at timestamp(6),
    id bigint NOT NULL,
    updated_at timestamp(6),
    email varchar(255),
    flow_type varchar(255) NOT NULL CHECK (flow_type IN ('EMAIL_CONFIRMATION','TELEGRAM_UNIFICATION')),
    telegram_id varchar(255),
    token varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE users (
    email_confirmed boolean NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    id bigint NOT NULL,
    last_login timestamp(6),
    updated_at timestamp(6),
    email varchar(255) UNIQUE,
    first_name varchar(255),
    last_name varchar(255),
    password varchar(255),
    telegram_id varchar(255) UNIQUE,
    username varchar(255) NOT NULL UNIQUE,
    roles varchar(255)[] NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE ai_players
    ADD CONSTRAINT fk_ai_players_match FOREIGN KEY (match_id) REFERENCES matches;
ALTER TABLE match_user
    ADD CONSTRAINT fk_match_user_match FOREIGN KEY (match_id) REFERENCES matches;
ALTER TABLE match_user
    ADD CONSTRAINT fk_match_user_user FOREIGN KEY (user_id) REFERENCES users;
ALTER TABLE matches
    ADD CONSTRAINT fk_matches_host_user FOREIGN KEY (host_user_id) REFERENCES users;
ALTER TABLE matches
    ADD CONSTRAINT fk_matches_winner_user FOREIGN KEY (winner_user_id) REFERENCES users;
ALTER TABLE matches_moves
    ADD CONSTRAINT fk_matches_moves_move FOREIGN KEY (moves_id) REFERENCES moves;
ALTER TABLE matches_moves
    ADD CONSTRAINT fk_matches_moves_match FOREIGN KEY (match_entity_id) REFERENCES matches;
ALTER TABLE moves
    ADD CONSTRAINT fk_moves_match FOREIGN KEY (match_id) REFERENCES matches;
