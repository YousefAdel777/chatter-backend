package com.chatter.chatter.factory;

import com.chatter.chatter.creator.MessageCreator;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@Component
public class MessageFactory {

    private final List<MessageCreator> creators;

    @Autowired
    public MessageFactory(List<MessageCreator> creators) {
        this.creators = creators;
    }

    public MessageCreator getMessageCreator(MessageType messageType) {
        return creators.stream()
                .filter(messageCreator -> messageCreator.supports(messageType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported Message Type"));
    }

}
