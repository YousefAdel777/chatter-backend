package com.chatter.chatter.listener;

import com.chatter.chatter.event.MemberJoinEvent;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.MessageReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberJoinEventListener {

    private final MessageReadService messageReadService;
    private final ChatService chatService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(MemberJoinEvent event) {
        Member member = event.getMember();
        if (member.getChat().getChatType().equals(ChatType.GROUP)) {
            messageReadService.readChatMessages(member.getUser().getEmail(), member.getChat().getId());
        }
        chatService.evictChatCacheForUser(member.getUser().getEmail());
    }

}
