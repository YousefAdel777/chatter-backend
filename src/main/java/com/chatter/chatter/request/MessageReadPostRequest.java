package com.chatter.chatter.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageReadPostRequest {

    @NotNull(message = "messageId is required")
    private Long messageId;

}
