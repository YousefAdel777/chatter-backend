package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoryMapper {

    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;

    public StoryDto toDto(Story story, String email) {
        if (story == null) return null;
        StoryDto storyDto = StoryDto.builder()
                .id(story.getId())
                .content(story.getContent())
                .createdAt(story.getCreatedAt())
                .storyType(story.getStoryType())
                .user(userMapper.toDto(story.getUser()))
                .build();
        if (story.getStoryType().equals(StoryType.TEXT)) {
            storyDto.setTextColor(((TextStory) story).getTextColor());
            storyDto.setBackgroundColor(((TextStory) story).getBackgroundColor());
        }
        else if (story.getStoryType().equals(StoryType.VIDEO) || story.getStoryType().equals(StoryType.IMAGE)) {
            storyDto.setFilePath(fileUploadService.getFileUrl(((MediaStory) story).getFilePath()));
        }
        if (email.equals(story.getUser().getEmail())) {
            storyDto.setExcludedUsersIds(story.getExcludedUsers().stream().map(User::getId).collect(Collectors.toSet()));
        }
        else {
            storyDto.setIsViewed(story.isViewed(email));
        }
        return storyDto;
    }

    public List<StoryDto> toDtoList(List<Story> stories, String email) {
        if (stories == null) return null;
        return stories.stream().map(story -> toDto(story, email)).collect(Collectors.toList());
    }

}
