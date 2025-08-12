create TABLE IF NOT EXISTS media_stories
(
    story_id  BIGINT       NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    CONSTRAINT pk_media_stories PRIMARY KEY (story_id)
);

create TABLE IF NOT EXISTS stories
(
    story_id   BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT                NOT NULL,
    story_type VARCHAR(255)          NOT NULL,
    content    VARCHAR(255)          NULL,
    created_at datetime              NOT NULL,
    CONSTRAINT pk_stories PRIMARY KEY (story_id)
);

create TABLE IF NOT EXISTS story_excluded_users
(
    story_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,
    CONSTRAINT pk_story_excluded_users PRIMARY KEY (story_id, user_id)
);

create TABLE IF NOT EXISTS text_stories
(
    story_id         BIGINT       NOT NULL,
    background_color VARCHAR(255) NOT NULL,
    text_color       VARCHAR(255) NOT NULL,
    CONSTRAINT pk_text_stories PRIMARY KEY (story_id)
);

alter table media_stories
    add CONSTRAINT FK_MEDIA_STORIES_ON_STORY FOREIGN KEY (story_id) REFERENCES stories (story_id);

alter table stories
    add CONSTRAINT FK_STORIES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id) ON delete CASCADE;

alter table text_stories
    add CONSTRAINT FK_TEXT_STORIES_ON_STORY FOREIGN KEY (story_id) REFERENCES stories (story_id);

alter table story_excluded_users
    add CONSTRAINT fk_stoexcuse_on_story FOREIGN KEY (story_id) REFERENCES stories (story_id);

alter table story_excluded_users
    add CONSTRAINT fk_stoexcuse_on_user FOREIGN KEY (user_id) REFERENCES users (user_id);