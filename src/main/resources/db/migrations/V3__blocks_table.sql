create TABLE IF NOT EXISTS blocks
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    blocked_by_id   BIGINT                NULL,
    blocked_user_id BIGINT                NULL,
    CONSTRAINT pk_blocks PRIMARY KEY (id)
);

alter table blocks
    add CONSTRAINT uc_bad6ac61eaec8b8f6f6765f04 UNIQUE (blocked_by_id, blocked_user_id);

alter table blocks
    add CONSTRAINT FK_BLOCKS_ON_BLOCKED_BY FOREIGN KEY (blocked_by_id) REFERENCES users (user_id) ON delete CASCADE;

alter table blocks
    add CONSTRAINT FK_BLOCKS_ON_BLOCKED_USER FOREIGN KEY (blocked_user_id) REFERENCES users (user_id) ON delete CASCADE;