package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.CallMessage;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class CallMessageCreator implements MessageCreator {
    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.CALL);
    }

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        CallMessage callMessage = new CallMessage();
        callMessage.setIsMissed(request.getMissed());
        if (!request.getMissed()) {
            callMessage.setDuration(request.getDuration());
        }
        else {
            callMessage.setDuration(0L);
        }
        callMessage.setMessageType(MessageType.CALL);
        return callMessage;
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        Boolean isMissed = request.getMissed();
        Long duration = request.getDuration();
        if (isMissed == null) {
            throw new BadRequestException("isMissed", "isMissed is required");
        }
        if (!isMissed && duration == null) {
            throw new BadRequestException("duration", "duration is required");
        }
    }
}
