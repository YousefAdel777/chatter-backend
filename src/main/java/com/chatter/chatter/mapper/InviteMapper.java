package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.InviteDto;
import com.chatter.chatter.model.Invite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InviteMapper {

    private final GroupChatPreviewMapper groupChatPreviewMapper;

    public InviteDto toDto(Invite invite) {
        if (invite == null) return null;
        return InviteDto.builder()
                .id(invite.getId())
                .inviteChat(groupChatPreviewMapper.toDto(invite.getGroupChat()))
                .canUseLink(invite.getCanUseLink())
                .createdAt(invite.getCreatedAt())
                .expiresAt(invite.getExpiresAt())
                .build();
    }

    public List<InviteDto> toDtoList(List<Invite> invites) {
        if (invites == null) return null;
        return invites.stream().map(this::toDto).collect(Collectors.toList());
    }

}
