create TABLE IF NOT EXISTS messages
(
    message_id       BIGINT AUTO_INCREMENT NOT NULL,
    chat_id          BIGINT                NOT NULL,
    user_id          BIGINT                NULL,
    reply_message_id BIGINT                NULL,
    message_type     VARCHAR(255)          NOT NULL,
    content          VARCHAR(255)          NULL,
    created_at       datetime              NOT NULL,
    is_forwarded     BIT(1)                NOT NULL,
    is_edited        BIT(1)                NOT NULL,
    pinned           BIT(1)                NULL,
    CONSTRAINT pk_messages PRIMARY KEY (message_id)
);

create TABLE IF NOT EXISTS options
(
    option_id  BIGINT AUTO_INCREMENT NOT NULL,
    message_id BIGINT                NOT NULL,
    title      VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_options PRIMARY KEY (option_id)
);

create TABLE IF NOT EXISTS poll_messages
(
    message_id BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    ends_at    datetime     NULL,
    multiple   BIT(1)       NULL,
    CONSTRAINT pk_poll_messages PRIMARY KEY (message_id)
);

create TABLE IF NOT EXISTS votes
(
    id        BIGINT AUTO_INCREMENT NOT NULL,
    user_id   BIGINT                NOT NULL,
    option_id BIGINT                NOT NULL,
    CONSTRAINT pk_votes PRIMARY KEY (id)
);

alter table votes
    add CONSTRAINT uc_9ce6f41e41df31a00c685fccb UNIQUE (user_id, option_id);

alter table options
    add CONSTRAINT FK_OPTIONS_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES poll_messages (message_id);

alter table poll_messages
    add CONSTRAINT FK_POLL_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);

alter table votes
    add CONSTRAINT FK_VOTES_ON_OPTION FOREIGN KEY (option_id) REFERENCES options (option_id);

alter table votes
    add CONSTRAINT FK_VOTES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;