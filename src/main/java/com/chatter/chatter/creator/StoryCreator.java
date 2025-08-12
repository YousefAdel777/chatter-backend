package com.chatter.chatter.creator;

import com.chatter.chatter.dto.StoryPostRequest;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryType;

public interface StoryCreator {

    boolean supports(StoryType storyType);

    Story createStory(StoryPostRequest request);

}
