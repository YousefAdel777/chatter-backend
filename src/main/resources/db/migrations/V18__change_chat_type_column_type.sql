alter table chat
    drop COLUMN chat_type;

alter table chat
    add chat_type VARCHAR(255) NOT NULL;