package com.chatter.chatter.dto;

import com.chatter.chatter.model.StoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StoryDto {

    @NotNull
    private Long id;

    @NotBlank
    private String content;

    @NotBlank
    private String textColor;

    @NotBlank
    private String backgroundColor;

    @NotBlank
    private String filePath;

    @NotNull
    private Instant createdAt;

    @NotNull
    private StoryType storyType;

    private UserDto user;

    @Builder.Default
    private Set<Long> excludedUsersIds = new HashSet<>();

    private Boolean isViewed;

}
