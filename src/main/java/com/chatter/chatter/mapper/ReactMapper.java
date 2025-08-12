package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.ReactDto;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.React;
import com.chatter.chatter.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReactMapper {

    private final UserMapper userMapper;

    public ReactDto toDto(React react) {
        if (react == null) return null;
        return ReactDto.builder()
                .id(react.getId())
                .messageId(react.getMessage().getId())
                .user(userMapper.toDto(react.getUser()))
                .emoji(react.getEmoji())
                .build();
    }

    public List<ReactDto> toDtoList(List<React> reacts) {
        if (reacts == null) return null;
        return reacts.stream().map(this::toDto).collect(Collectors.toList());
    }
}
