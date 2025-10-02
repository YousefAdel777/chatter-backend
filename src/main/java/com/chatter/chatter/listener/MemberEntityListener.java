package com.chatter.chatter.listener;

import com.chatter.chatter.event.MemberDeletionEvent;
import com.chatter.chatter.event.MemberJoinEvent;
import com.chatter.chatter.model.Member;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberEntityListener {

    private final ApplicationEventPublisher eventPublisher;

    @PreRemove
    public void handleMemberDelete(Member member) {
        eventPublisher.publishEvent(new MemberDeletionEvent(member));
    }

    @PostPersist
    public void handleMemberJoin(Member member) {
        eventPublisher.publishEvent(new MemberJoinEvent(member));
    }

}
