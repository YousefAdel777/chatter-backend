package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.StoryViewDto;
import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.model.StoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoryViewMapper {

    private final UserMapper userMapper;

    public StoryViewDto toDto(StoryView storyView) {
        if (storyView == null) return null;
        return StoryViewDto.builder()
                .id(storyView.getId())
                .storyId(storyView.getStory().getId())
                .user(userMapper.toDto(storyView.getUser()))
                .createdAt(storyView.getCreatedAt())
                .build();
    }

    public List<StoryViewDto> toDtoList(List<StoryView> storyViews) {
        if (storyViews == null) return null;
        return storyViews.stream().map(this::toDto).collect(Collectors.toList());
    }

}
