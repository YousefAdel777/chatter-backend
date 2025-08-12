package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Invite;
import com.chatter.chatter.model.InviteMessage;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InviteMessageCreator implements MessageCreator {

    private final InviteService inviteService;

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        Invite invite = inviteService.getInviteEntity(request.getInviteId());
        if (!invite.isValid()) {
            throw new BadRequestException("message", "Invite expired");
        }
//        if (request.getChatId().equals(invite.getGroupChat().getId())) {
//            throw new BadRequestException("message", "Cannot send invite message to same chat");
//        }
        return InviteMessage.builder()
                .invite(invite)
                .messageType(MessageType.INVITE)
                .build();
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.INVITE);
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        if (request.getInviteId() == null) {
            throw new BadRequestException("message", "inviteId is required.");
        }
    }

}
