package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.model.PollMessage;
import com.chatter.chatter.service.OptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class PollMessageCreator implements MessageCreator {

    private final OptionService optionService;

    @Autowired
    public PollMessageCreator(
        OptionService optionService
    ) {
        this.optionService = optionService;
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.POLL);
    }

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        PollMessage message = PollMessage.builder()
                .title(request.getTitle())
                .endsAt(request.getEndsAt())
                .multiple(request.getMultiple())
                .messageType(MessageType.POLL)
                .build();
        message.setOptions(optionService.createOptions(request.getOptions(), message));
        return message;
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        String title = request.getTitle();
        List<String> options = request.getOptions();
        if (title == null || title.isEmpty()) {
            throw new BadRequestException("title", "Title is required");
        }
        if (request.getMultiple() == null) {
            throw new BadRequestException("title", "Title is required");
        }
        if (options == null || options.isEmpty()) {
            throw new BadRequestException("options", "At least 1 option is required");
        }
    }

}
