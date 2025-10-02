package com.chatter.chatter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChatStatusProjection {

    private Long id;

    private Long userId;

    private String userEmail;

    private Long unreadMessagesCount;

    private Long membersCount;

    private Long firstUnreadMessageId;

    private Boolean isMentioned;

}
