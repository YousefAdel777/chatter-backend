alter table text_stories
    modify background_color VARCHAR(7);

alter table stories
    drop COLUMN story_type;

alter table stories
    add story_type VARCHAR(255) NOT NULL;

alter table text_stories
    modify text_color VARCHAR(7);