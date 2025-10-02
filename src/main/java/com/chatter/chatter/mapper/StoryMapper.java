package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.dto.StoryStatusProjection;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoryMapper {

    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;

    public StoryDto toDto(Story story, StoryStatusProjection statusProjection, String email) {
        if (story == null) return null;
        StoryDto storyDto = StoryDto.builder()
                .id(story.getId())
                .content(story.getContent())
                .createdAt(story.getCreatedAt())
                .storyType(story.getStoryType())
                .user(userMapper.toDto(story.getUser()))
                .build();
        if (story instanceof TextStory textStory) {
            storyDto.setTextColor(textStory.getTextColor());
            storyDto.setBackgroundColor(textStory.getBackgroundColor());
        }
        else if (story instanceof MediaStory mediaStory) {
            storyDto.setFilePath(fileUploadService.getFileUrl(mediaStory.getFilePath()));
        }
        if (email.equals(story.getUser().getEmail())) {
            storyDto.setExcludedUsersIds(story.getExcludedUsers().stream().map(User::getId).collect(Collectors.toSet()));
        }
        else if (statusProjection != null) {
            storyDto.setIsViewed(statusProjection.getIsViewed());
        }
        return storyDto;
    }

    public List<StoryDto> toDtoList(List<Story> stories, List<StoryStatusProjection> projections, String email) {
        if (stories == null) return null;
        Map<Long, StoryStatusProjection> statusProjectionMap = projections.stream().collect(Collectors.toMap(StoryStatusProjection::getId, p -> p));
        return stories.stream().map(story -> toDto(story, statusProjectionMap.get(story.getId()), email)).collect(Collectors.toList());
    }

}
