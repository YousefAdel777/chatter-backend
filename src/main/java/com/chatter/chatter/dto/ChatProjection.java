package com.chatter.chatter.dto;

import com.chatter.chatter.model.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChatProjection {

    private Chat chat;

    private Long unreadMessagesCount;

    private Long membersCount;

    private Long firstUnreadMessageId;

    private Boolean isMentioned;

}
