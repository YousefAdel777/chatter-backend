create TABLE IF NOT EXISTS call_messages
(
    message_id BIGINT NOT NULL,
    is_missed  BIT(1) NOT NULL,
    duration   BIGINT NULL,
    CONSTRAINT pk_call_messages PRIMARY KEY (message_id)
);

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

alter table call_messages
    add CONSTRAINT FK_CALL_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);