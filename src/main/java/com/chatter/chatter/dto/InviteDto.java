package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InviteDto {

    @NotNull
    private Long id;

    private GroupChatPreviewDto inviteChat;

    private Instant createdAt;

    private Instant expiresAt;

    @Builder.Default
    private Boolean canUseLink = false;

}
