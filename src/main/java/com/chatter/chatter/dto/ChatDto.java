package com.chatter.chatter.dto;

import com.chatter.chatter.model.ChatType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ChatDto {

    @NotNull
    private Long id;

    private UserDto otherUser;

    private LastMessageDto lastMessage;

    private Long unreadMessagesCount;

    private Long membersCount;

    private Long firstUnreadMessageId;

    private String name;

    private String description;

    private String image;

    private ChatType chatType;

    @Builder.Default
    private Boolean onlyAdminsCanSend = false;

    @Builder.Default
    private Boolean onlyAdminsCanInvite = true;

    @Builder.Default
    private Boolean onlyAdminsCanEditGroup = true;

    @Builder.Default
    private Boolean onlyAdminsCanPin = true;

    private Instant createdAt;

}
