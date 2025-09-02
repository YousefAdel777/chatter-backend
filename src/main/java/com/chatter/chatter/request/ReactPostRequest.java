package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ReactPostRequest {
    @NotNull(message = "messageId is required")
    private Long messageId;

    @NotNull(message = "emoji is required")
    @NotBlank(message = "emoji is required")
    private String emoji;
}
