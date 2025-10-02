package com.chatter.chatter.request;

import com.chatter.chatter.model.StoryType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StoryPostRequest {

    private String content;

    private String textColor;

    private String backgroundColor;

    @NotNull(message = "storyType is required")
    private StoryType storyType;

    private MultipartFile file;

    @Builder.Default
    private Set<Long> excludedUserIds = new HashSet<>();

}
