package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.dto.StoryProjection;
import com.chatter.chatter.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoryProjectionMapper {

    private final UserMapper userMapper;

    public StoryDto toDto(StoryProjection storyProjection, String email) {
        if (storyProjection == null) return null;
        StoryDto.StoryDtoBuilder builder = StoryDto.builder()
                .id(storyProjection.getId())
                .content(storyProjection.getContent())
                .createdAt(storyProjection.getCreatedAt())
                .storyType(storyProjection.getStoryType())
                .user(userMapper.toDto(storyProjection.getUser()))
                .textColor(storyProjection.getTextColor())
                .backgroundColor(storyProjection.getBackgroundColor())
                .filePath(storyProjection.getFilePath());
        if (email.equals(storyProjection.getUser().getEmail())) {
            builder.excludedUsersIds(storyProjection.getExcludedUsers().stream().map(User::getId).collect(Collectors.toSet()));
        }
        else {
            builder.isViewed(storyProjection.getIsViewed());
        }
        return builder.build();
    }

    public List<StoryDto> toDtoList(List<StoryProjection> storyProjections, String email) {
        if (storyProjections == null) return null;
        return storyProjections.stream().map(projection -> toDto(projection, email)).collect(Collectors.toList());
    }

}
