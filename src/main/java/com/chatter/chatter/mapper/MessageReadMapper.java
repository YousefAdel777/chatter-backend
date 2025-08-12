package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MessageReadDto;
import com.chatter.chatter.model.MessageRead;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageReadMapper {

    private final UserMapper userMapper;

    public MessageReadDto toDto(MessageRead messageRead) {
        if (messageRead == null) return null;
        return MessageReadDto.builder()
                .id(messageRead.getId())
                .createdAt(messageRead.getCreatedAt())
                .user(userMapper.toDto(messageRead.getUser()))
                .messageId(messageRead.getMessage().getId())
                .showRead(messageRead.getShowRead())
                .build();
    }

    public List<MessageReadDto> toDtoList(List<MessageRead> messageReads) {
        if (messageReads == null) return null;
        return messageReads.stream().map(this::toDto).collect(Collectors.toList());
    }

}
