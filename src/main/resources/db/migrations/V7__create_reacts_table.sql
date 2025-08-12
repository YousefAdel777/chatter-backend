create TABLE IF NOT EXISTS reacts
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    emoji      VARCHAR(255)          NULL,
    user_id    BIGINT                NULL,
    message_id BIGINT                NULL,
    CONSTRAINT pk_reacts PRIMARY KEY (id)
);

alter table reacts
    add CONSTRAINT uc_592d7e8848264612313fb4f66 UNIQUE (message_id, user_id);

alter table reacts
    add CONSTRAINT FK_REACTS_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id);

alter table reacts
    add CONSTRAINT FK_REACTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);