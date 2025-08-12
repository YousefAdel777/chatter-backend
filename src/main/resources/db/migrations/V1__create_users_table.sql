create TABLE users
(
    user_id            BIGINT AUTO_INCREMENT NOT NULL,
    username           VARCHAR(255)          NULL,
    email              VARCHAR(255)          NOT NULL,
    password           VARCHAR(255)          NOT NULL,
    created_at         datetime              NOT NULL,
    image              VARCHAR(255)          NULL,
    bio                VARCHAR(255)          NULL,
    last_online        datetime              NULL,
    show_online_status BIT(1)                NULL,
    show_message_reads BIT(1)                NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

alter table users
    add CONSTRAINT uc_users_email UNIQUE (email);