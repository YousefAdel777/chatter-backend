package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;

import java.security.Principal;

public interface MessageCreator {

    boolean supports(MessageType messageType);

    Message createMessage(BaseMessageRequest request, String email);

    void validateRequest(BaseMessageRequest request);
}
