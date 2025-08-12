package com.chatter.chatter.listener;

import com.chatter.chatter.event.MemberDeletionEvent;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberDeletionEventListener {

    private final MemberService memberService;
    private final ChatService chatService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(MemberDeletionEvent event) {
        Member member = event.getMember();
        memberService.replaceOwner(event.getMember());
        if (member.getChat().getMembers().isEmpty()) {
            chatService.deleteChat(member.getChat().getId());
        }
        else {
            chatService.evictChatCacheForUser(member.getUser().getEmail());
            if (member.getChat().getChatType().equals(ChatType.INDIVIDUAL)) {
                chatService.evictChatCache(member.getChat());
            }
        }
    }

}
