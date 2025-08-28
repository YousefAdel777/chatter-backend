package com.chatter.chatter.creator;

import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryType;
import com.chatter.chatter.model.TextStory;
import org.springframework.stereotype.Component;

@Component
public class TextStoryCreator implements StoryCreator {

    @Override
    public Story createStory(StoryPostRequest request) {
        validateRequest(request);
        return TextStory.builder()
                .content(request.getContent())
                .textColor(request.getTextColor())
                .backgroundColor(request.getBackgroundColor())
                .storyType(StoryType.TEXT)
                .build();
    }

    @Override
    public boolean supports(StoryType storyType) {
        return storyType.equals(StoryType.TEXT);
    }

    private void validateRequest(StoryPostRequest request) {
        String content = request.getContent();
        String backgroundColor = request.getBackgroundColor();
        String textColor = request.getTextColor();
        if (content == null || content.isEmpty()) {
            throw new BadRequestException("content", "content is required");
        }
        if (backgroundColor == null || backgroundColor.isEmpty()) {
            throw new BadRequestException("backgroundColor", "backgroundColor is required");
        }
        if (textColor == null || textColor.isEmpty()) {
            throw new BadRequestException("textColor", "textColor is required");
        }
    }

}
