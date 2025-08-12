drop table message_reads;

create TABLE message_reads
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT                NULL,
    message_id BIGINT                NOT NULL,
    show_read  BIT(1)                NULL,
    created_at datetime              NOT NULL,
    CONSTRAINT pk_message_reads PRIMARY KEY (id)
);

alter table message_reads
    add CONSTRAINT uc_c716fea4e79ca6814cf7604d7 UNIQUE (message_id, user_id);

alter table message_reads
    add CONSTRAINT FK_MESSAGE_READS_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES messages (message_id) ON delete CASCADE;

alter table message_reads
    add CONSTRAINT FK_MESSAGE_READS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete SET NULL;