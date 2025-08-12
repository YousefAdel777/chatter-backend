package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.StarredMessageDto;
import com.chatter.chatter.model.StarredMessage;

import java.util.List;
import java.util.stream.Collectors;

public class StarredMessageMapper {

    public static StarredMessageDto toDto(StarredMessage starredMessage) {
        if (starredMessage == null) return null;
        return StarredMessageDto.builder()
                .id(starredMessage.getId())
                .messageId(starredMessage.getMessage().getId())
                .userId(starredMessage.getUser().getId())
                .createdAt(starredMessage.getCreatedAt())
                .build();
    }

    public static List<StarredMessageDto> toDtoList(List<StarredMessage> starredMessages) {
        if (starredMessages == null) return null;
        return starredMessages.stream().map(StarredMessageMapper::toDto).collect(Collectors.toList());
    }

}
