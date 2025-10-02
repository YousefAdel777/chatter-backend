create TABLE mentions
(
    mention_id BIGINT AUTO_INCREMENT NOT NULL,
    message_id BIGINT                NOT NULL,
    user_id    BIGINT                NOT NULL,
    CONSTRAINT pk_mentions PRIMARY KEY (mention_id)
);

alter table messages
    add content_json JSON NULL;

alter table messages
    add is_everyone_mentioned BIT(1) NULL;

alter table mentions
    add CONSTRAINT uc_25daca18d71496f3a6c298849 UNIQUE (user_id, message_id);

alter table mentions
    add CONSTRAINT FK_MENTIONS_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id) ON delete CASCADE;

create index idx_message_id on mentions (message_id);

alter table mentions
    add CONSTRAINT FK_MENTIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;

create index idx_user_id on mentions (user_id);