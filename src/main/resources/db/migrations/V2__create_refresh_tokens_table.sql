create TABLE IF NOT EXISTS refresh_tokens
(
    refresh_token_id BIGINT AUTO_INCREMENT NOT NULL,
    token            VARCHAR(255)          NOT NULL,
    user_id          BIGINT                NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (refresh_token_id)
);

alter table refresh_tokens
    add CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

alter table refresh_tokens
    add CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;