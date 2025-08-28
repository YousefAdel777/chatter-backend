package com.chatter.chatter.request;

import com.chatter.chatter.model.StoryType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StoryPostRequest {

    private String content;

    private String textColor;

    private String backgroundColor;

    @NotNull(message = "storyType is required")
    private StoryType storyType;

    private MultipartFile file;

    private Set<Long> excludedUserIds = new HashSet<>();

}
