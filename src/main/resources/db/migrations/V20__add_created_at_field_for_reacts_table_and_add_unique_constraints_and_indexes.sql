alter table reacts
    add created_at datetime NULL;

alter table reacts
    modify created_at datetime not NULL;

create index idx_blocked_by_id on blocks (blocked_by_id);

create index idx_username on users (username);

alter table chat
    drop COLUMN chat_type;

alter table chat
    add chat_type VARCHAR(255) NOT NULL;

alter table reacts
    modify emoji VARCHAR(255) NOT NULL;

alter table members
    drop COLUMN member_role;

alter table members
    add member_role VARCHAR(255) NULL;

alter table messages
    drop COLUMN message_type;

alter table messages
    add message_type VARCHAR(255) NOT NULL;

alter table stories
    drop COLUMN story_type;

alter table stories
    add story_type VARCHAR(255) NOT NULL;

alter table refresh_tokens
    drop COLUMN token;

alter table refresh_tokens
    add token VARCHAR(500) NOT NULL;

alter table refresh_tokens
    add CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

alter table users
    modify username VARCHAR(255) NULL;