package com.chatter.chatter.creator;


import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoryMessageCreator implements MessageCreator {

    private final StoryService storyService;

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        Story story = storyService.getStoryEntity(email, request.getStoryId());
        return StoryMessage.builder()
                .content(request.getContent())
                .story(story)
                .messageType(MessageType.STORY)
                .build();
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.STORY);
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        if (request.getStoryId() == null) {
            throw new BadRequestException("storyId", "Story id is required");
        }
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new BadRequestException("content", "Content is required");
        }
    }

}
