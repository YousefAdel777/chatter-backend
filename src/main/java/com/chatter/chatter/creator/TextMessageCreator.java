package com.chatter.chatter.creator;

import com.chatter.chatter.model.TextMessage;
import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class TextMessageCreator implements MessageCreator {

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        return TextMessage.builder()
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .build();
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.TEXT);
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new BadRequestException("content", "Content is required.");
        }
    }

}
