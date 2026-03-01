create TABLE gif_messages
(
    message_id BIGINT       NOT NULL,
    gif_id     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_gif_messages PRIMARY KEY (message_id)
);

update messages set is_everyone_mentioned = 0 where is_everyone_mentioned is null;

alter table messages
    modify is_everyone_mentioned BIT(1) NOT NULL;

alter table gif_messages
    add CONSTRAINT FK_GIF_MESSAGES_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);

alter table messages
    modify pinned BIT(1) NOT NULL;