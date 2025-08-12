create TABLE IF NOT EXISTS message_reads
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT                NOT NULL,
    message_id BIGINT                NOT NULL,
    show_read  BIT(1)                NULL,
    created_at datetime              NOT NULL,
    CONSTRAINT pk_message_read PRIMARY KEY (id)
);

create TABLE IF NOT EXISTS starred_messages
(
    starred_message_id BIGINT AUTO_INCREMENT NOT NULL,
    user_id            BIGINT                NOT NULL,
    message_id         BIGINT                NOT NULL,
    created_at         datetime              NOT NULL,
    CONSTRAINT pk_starred_messages PRIMARY KEY (starred_message_id)
);

alter table message_reads
    add CONSTRAINT uc_1c188302cce80b5059b163f8c UNIQUE (message_id, user_id);

alter table starred_messages
    add CONSTRAINT uc_597d67530610a14f453f5095a UNIQUE (user_id, message_id);

alter table message_reads
    add CONSTRAINT FK_MESSAGE_READ_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id) ON delete CASCADE;

alter table message_reads
    add CONSTRAINT FK_MESSAGE_READ_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

alter table starred_messages
    add CONSTRAINT FK_STARRED_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id) ON delete CASCADE;

alter table starred_messages
    add CONSTRAINT FK_STARRED_MESSAGES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;