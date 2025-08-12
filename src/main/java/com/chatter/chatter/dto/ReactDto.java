package com.chatter.chatter.dto;

import com.chatter.chatter.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReactDto {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "messageId is required")
    private Long messageId;

    @NotNull(message = "userId is required")
    private UserDto user;

    @NotNull(message = "emoji is required")
    @NotBlank(message = "emoji is required")
    private String emoji;
}
