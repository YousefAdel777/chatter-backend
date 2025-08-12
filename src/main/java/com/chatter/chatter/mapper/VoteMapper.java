package com.chatter.chatter.mapper;


import com.chatter.chatter.dto.VoteDto;
import com.chatter.chatter.model.Vote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VoteMapper {

    private final UserMapper userMapper;

    public VoteDto toDto(Vote vote) {
        if (vote == null) return null;
        return VoteDto.builder()
                .id(vote.getId())
                .user(userMapper.toDto(vote.getUser()))
                .optionId(vote.getOption().getId())
                .build();
    }

    public List<VoteDto> toDtoList(List<Vote> votes) {
        if (votes == null) return null;
        return votes.stream().map(this::toDto).collect(Collectors.toList());
    }

}
