package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReactPostRequest {
    @NotNull(message = "messageId is required")
    private Long messageId;

    @NotNull(message = "emoji is required")
    @NotBlank(message = "emoji is required")
    private String emoji;
}
