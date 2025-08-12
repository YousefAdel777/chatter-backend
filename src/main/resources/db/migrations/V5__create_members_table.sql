create TABLE IF NOT EXISTS members
(
    member_id   BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT                NOT NULL,
    chat_id     BIGINT                NOT NULL,
    joined_at   datetime              NOT NULL,
    member_role VARCHAR(255)          NULL,
    CONSTRAINT pk_members PRIMARY KEY (member_id)
);

ALTER TABLE members
    add CONSTRAINT uc_dc6d0043424327253220a1dcd UNIQUE (user_id, chat_id);

ALTER TABLE members
    add CONSTRAINT FK_MEMBERS_ON_CHAT FOREIGN KEY (chat_id) REFERENCES chat (chat_id) ON DELETE CASCADE;

ALTER TABLE members
    add CONSTRAINT FK_MEMBERS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;