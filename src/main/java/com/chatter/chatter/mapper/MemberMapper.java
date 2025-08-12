package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MemberDto;
import com.chatter.chatter.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final UserMapper userMapper;

    public MemberDto toDto(Member member) {
        if (member == null) return null;
        return MemberDto.builder()
                .id(member.getId())
                .user(userMapper.toDto(member.getUser()))
                .chatId(member.getChat().getId())
                .memberRole(member.getMemberRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public List<MemberDto> toDtoList(List<Member> members) {
        if (members == null) return null;
        return members.stream().map(this::toDto).collect(Collectors.toList());
    }
}
