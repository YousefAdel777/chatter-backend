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

create TABLE IF NOT EXISTS text_messages
(
    message_id BIGINT NOT NULL,
    CONSTRAINT pk_text_messages PRIMARY KEY (message_id)
);

alter table messages
    add CONSTRAINT FK_MESSAGES_ON_CHAT FOREIGN KEY (chat_id) REFERENCES chat (chat_id);

alter table messages
    add CONSTRAINT FK_MESSAGES_ON_REPLY_MESSAGE FOREIGN KEY (reply_message_id) REFERENCES messages (message_id);

alter table messages
    add CONSTRAINT FK_MESSAGES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete SET NULL;

alter table text_messages
    add CONSTRAINT FK_TEXT_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);