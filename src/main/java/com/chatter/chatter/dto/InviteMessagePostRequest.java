package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InviteMessagePostRequest {

    @NotNull(message = "chatIds is required")
    @Size(min = 1, message = "At least 1 chatId is required")
    private Set<Long> chatIds;

    @NotNull(message = "inviteChatId is required")
    private Long inviteChatId;

    private Instant expiresAt;

}
