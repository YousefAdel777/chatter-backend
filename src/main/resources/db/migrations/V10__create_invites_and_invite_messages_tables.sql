create TABLE IF NOT EXISTS invite_messages
(
    message_id BIGINT NOT NULL,
    invite_id  BIGINT NULL,
    CONSTRAINT pk_invite_messages PRIMARY KEY (message_id)
);

create TABLE IF NOT EXISTS invites
(
    invite_id     BIGINT AUTO_INCREMENT NOT NULL,
    group_chat_id BIGINT                NULL,
    can_use_link  BIT(1)                NULL,
    created_at    datetime              NOT NULL,
    expires_at    datetime              NULL,
    CONSTRAINT pk_invites PRIMARY KEY (invite_id)
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

alter table invites
    add CONSTRAINT FK_INVITES_ON_GROUP_CHAT FOREIGN KEY (group_chat_id) REFERENCES group_chats (chat_id) ON delete CASCADE;

alter table invite_messages
    add CONSTRAINT FK_INVITE_MESSAGES_ON_INVITE FOREIGN KEY (invite_id) REFERENCES invites (invite_id) ON delete SET NULL;

alter table invite_messages
    add CONSTRAINT FK_INVITE_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);