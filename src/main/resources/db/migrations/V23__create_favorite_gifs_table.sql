create TABLE favorite_gifs
(
    favorite_gif_id BIGINT AUTO_INCREMENT NOT NULL,
    gif_id          VARCHAR(255)          NOT NULL,
    user_id         BIGINT                NOT NULL,
    created_at      datetime              NOT NULL,
    CONSTRAINT pk_favorite_gifs PRIMARY KEY (favorite_gif_id)
);

alter table favorite_gifs
    add CONSTRAINT uc_user_gif UNIQUE (user_id, gif_id);

alter table favorite_gifs
    add CONSTRAINT FK_FAVORITE_GIFS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;