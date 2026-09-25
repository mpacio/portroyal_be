
    create sequence emails_queue_id_seq start with 1 increment by 1;

    create sequence game_configurations_id_seq start with 1 increment by 1;

    create sequence matches_id_seq start with 1 increment by 1;

    create sequence moves_id_seq start with 1 increment by 1;

    create sequence temporary_tokens_id_seq start with 1 increment by 1;

    create sequence users_id_seq start with 1 increment by 1;

    create table callbacks (
        created_at timestamp(6) not null,
        updated_at timestamp(6),
        user_id bigint not null,
        match_key_code varchar(255) not null,
        secret varchar(255),
        url varchar(255) not null,
        primary key (user_id, match_key_code)
    );

    create table emails_queue (
        sent boolean not null,
        created_at timestamp(6) not null,
        id bigint not null,
        sent_at timestamp(6),
        updated_at timestamp(6),
        html_body TEXT,
        recipient_email varchar(255) not null,
        recipient_name varchar(255) not null,
        subject varchar(255) not null,
        text_body TEXT,
        primary key (id)
    );

    create table game_configurations (
        config_id integer,
        id integer not null,
        config_name varchar(255),
        prop_name varchar(255) unique,
        prop_value varchar(255),
        primary key (id)
    );

    create table match_user (
        match_id bigint not null,
        user_id bigint not null
    );

    create table matches (
        configuration_id integer not null,
        ended boolean not null,
        started boolean not null,
        created_at timestamp(6) not null,
        ended_at timestamp(6),
        host_user_id bigint not null,
        id bigint not null,
        last_move_at timestamp(6),
        started_at timestamp(6),
        updated_at timestamp(6),
        winner_user_id bigint,
        key_code varchar(255) not null unique,
        primary key (id)
    );

    create table matches_moves (
        match_entity_id bigint not null,
        moves_id bigint not null unique
    );

    create table moves (
        active_player_index integer,
        configuration_id integer,
        parameter_index integer,
        pick_player_index integer,
        running_player_index integer,
        time_index integer,
        created_at timestamp(6) not null,
        id bigint not null,
        match_id bigint not null,
        updated_at timestamp(6),
        active_player_username varchar(255),
        lib_version varchar(255),
        move_name varchar(255) check (move_name in ('DISCOVER','REPEL','ACCEPT','FINISH_DISCOVER','TRADE_HIRE','TRADE','HIRE','TRADE_RENOUNCE','END_TURN','COMMIT_EXPEDITION','SIGN_CONTRACT')),
        notes varchar(255),
        running_player_username varchar(255),
        expedition_employees_list varchar(255) array,
        primary key (id)
    );

    create table temporary_tokens (
        failed_attempts integer,
        confirmed_at timestamp(6),
        created_at timestamp(6) not null,
        expires_at timestamp(6),
        id bigint not null,
        updated_at timestamp(6),
        email varchar(255),
        flow_type varchar(255) not null check (flow_type in ('EMAIL_CONFIRMATION','TELEGRAM_UNIFICATION')),
        telegram_id varchar(255),
        token varchar(255) not null,
        primary key (id)
    );

    create table users (
        email_confirmed boolean not null,
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigint not null,
        last_login timestamp(6),
        updated_at timestamp(6),
        email varchar(255) unique,
        first_name varchar(255),
        last_name varchar(255),
        password varchar(255),
        telegram_id varchar(255) unique,
        username varchar(255) not null unique,
        roles varchar(255) array not null,
        primary key (id)
    );

    alter table if exists match_user
       add constraint FK32qnb8qnbu72aj944gt4wiepa
       foreign key (match_id)
       references matches;

    alter table if exists match_user
       add constraint FK2od7qhlqx3qrpklw3qrwrixm9
       foreign key (user_id)
       references users;

    alter table if exists matches
       add constraint FKexmyvtg9m4nk73jdhpxcid13h
       foreign key (host_user_id)
       references users;

    alter table if exists matches
       add constraint FKpbwtjqvm2awhnihq9ujxh87r0
       foreign key (winner_user_id)
       references users;

    alter table if exists matches_moves
       add constraint FKmtec3a80dj9s1j9ajnv38lf3g
       foreign key (moves_id)
       references moves;

    alter table if exists matches_moves
       add constraint FKsoy9s2eq5fcn04ufnf0ecjse9
       foreign key (match_entity_id)
       references matches;

    alter table if exists moves
       add constraint FKak61stg1c88nsisxv5n0wx6wu
       foreign key (match_id)
       references matches;
