package com.chatter.chatter.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MessageReadBatchPostRequest {

    @NotNull(message = "messagesIds cannot be null")
    @NotEmpty(message = "messagesIds cannot be empty")
    private Set<Long> messagesIds;

}
