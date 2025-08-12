CREATE TABLE story_views
(
    story_view_id BIGINT AUTO_INCREMENT NOT NULL,
    user_id       BIGINT                NOT NULL,
    story_id      BIGINT                NOT NULL,
    created_at    datetime              NOT NULL,
    CONSTRAINT pk_story_views PRIMARY KEY (story_view_id)
);

ALTER TABLE story_views
    ADD CONSTRAINT uc_5dc711e365935abf61f95c363 UNIQUE (user_id, story_id);

ALTER TABLE story_views
    ADD CONSTRAINT FK_STORY_VIEWS_ON_STORY FOREIGN KEY (story_id) REFERENCES stories (story_id) ON DELETE CASCADE;

ALTER TABLE story_views
    ADD CONSTRAINT FK_STORY_VIEWS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);