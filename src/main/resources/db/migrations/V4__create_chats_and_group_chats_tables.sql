create TABLE IF NOT EXISTS chat
(
    chat_id    BIGINT AUTO_INCREMENT NOT NULL,
    chat_type  SMALLINT              NOT NULL,
    created_at datetime              NOT NULL,
    CONSTRAINT pk_chat PRIMARY KEY (chat_id)
);

create TABLE IF NOT EXISTS group_chats
(
    chat_id                    BIGINT       NOT NULL,
    name                       VARCHAR(255) NOT NULL,
    `description`              VARCHAR(255) NOT NULL,
    image                      VARCHAR(255) NOT NULL,
    only_admins_can_send       BIT(1)       NULL,
    only_admins_can_invite     BIT(1)       NULL,
    only_admins_can_edit_group BIT(1)       NULL,
    only_admins_can_pin        BIT(1)       NULL,
    CONSTRAINT pk_group_chats PRIMARY KEY (chat_id)
);

alter table group_chats
    add CONSTRAINT FK_GROUP_CHATS_ON_CHAT FOREIGN KEY (chat_id) REFERENCES chat (chat_id);