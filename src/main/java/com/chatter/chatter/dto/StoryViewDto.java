package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class StoryViewDto {

    @NotNull
    private Long id;

    @NotNull
    private Long storyId;

    @NotNull
    private UserDto user;

    @NotNull
    private Instant createdAt;

}
