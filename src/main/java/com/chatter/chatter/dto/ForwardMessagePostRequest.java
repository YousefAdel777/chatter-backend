package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ForwardMessagePostRequest {

    @NotNull(message = "messageId is required")
    private Long messageId;

    @NotNull(message = "chatIds is required")
    @Size(min = 1, message = "chatIds cannot be empty")
    private Set<Long> chatIds;

}
