package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MentionDto;
import com.chatter.chatter.dto.MessageDto;
import com.chatter.chatter.model.Mention;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MentionMapper {

    private final UserMapper userMapper;

    public MentionDto toDto(Mention mention) {
        if (mention == null) return null;
        return MentionDto.builder()
                .id(mention.getId())
                .user(userMapper.toDto(mention.getUser()))
                .build();
    }

    public List<MentionDto> toDtoList(List<Mention> mentions) {
        if (mentions == null) return null;
        return mentions.stream().map(this::toDto).collect(Collectors.toList());
    }

}
