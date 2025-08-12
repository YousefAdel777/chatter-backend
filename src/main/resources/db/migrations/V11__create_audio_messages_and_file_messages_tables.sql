create TABLE IF NOT EXISTS audio_messages
(
    message_id BIGINT       NOT NULL,
    file_url   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_audio_messages PRIMARY KEY (message_id)
);

create TABLE IF NOT EXISTS file_messages
(
    message_id         BIGINT       NOT NULL,
    file_path          VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size          BIGINT       NOT NULL,
    CONSTRAINT pk_file_messages PRIMARY KEY (message_id)
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

alter table audio_messages
    add CONSTRAINT FK_AUDIO_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);

alter table file_messages
    add CONSTRAINT FK_FILE_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);