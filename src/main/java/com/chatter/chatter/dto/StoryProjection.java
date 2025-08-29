package com.chatter.chatter.dto;

import com.chatter.chatter.model.StoryType;
import com.chatter.chatter.model.User;

import java.time.Instant;
import java.util.Set;

public interface StoryProjection {
    Long getId();

    String getContent();

    String getTextColor();

    String getBackgroundColor();

    String getFilePath();

    Instant getCreatedAt();

    StoryType getStoryType();

    User getUser();

    Set<User> getExcludedUsers();

    Boolean getIsViewed();
}
