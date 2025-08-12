package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class StoryViewPostRequest {

    @NotNull
    private Long storyId;

}
