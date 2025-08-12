package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BlockPostRequest {

    @NotNull(message = "blockedUserId is required")
    private Long blockedUserId;

}
