package com.chatter.chatter.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BatchMessageRequest extends BaseMessageRequest {

    @NotNull(message = "chatsIds is required")
    @NotEmpty(message = "chatsIds cannot be empty")
    private Set<Long> chatsIds;

}
