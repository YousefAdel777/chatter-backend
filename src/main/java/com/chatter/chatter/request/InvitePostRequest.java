package com.chatter.chatter.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvitePostRequest {

    @NotNull(message = "inviteChatId is required")
    private Long inviteChatId;

    @Future(message = "expiresAt must be in the future")
    private Instant expiresAt;

    @NotNull(message = "canUseLink cannot be null")
    private Boolean canUseLink = false;

}
