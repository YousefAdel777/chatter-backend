package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.OptionDto;
import com.chatter.chatter.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OptionMapper {

    private final VoteMapper voteMapper;

    public OptionDto toDto(Option option) {
        if (option == null) return null;
        return OptionDto.builder()
                .id(option.getId())
                .title(option.getTitle())
                .votes(voteMapper.toDtoList(option.getVotes()))
                .messageId(option.getPollMessage().getId())
                .build();
    }

    public List<OptionDto> toDtoList(List<Option> options) {
        if (options == null) return null;
        return options.stream().map(this::toDto).collect(Collectors.toList());
    }

}
