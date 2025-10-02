package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.GroupChatPreviewDto;
import com.chatter.chatter.model.GroupChat;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GroupChatPreviewMapper {

    private final FileUploadService fileUploadService;
    
    GroupChatPreviewDto toDto(GroupChat groupChat) {
        if (groupChat == null) return null;
        return GroupChatPreviewDto.builder()
                .id(groupChat.getId())
                .chatType(groupChat.getChatType())
                .createdAt(groupChat.getCreatedAt())
                .image(fileUploadService.getFileUrl(groupChat.getImage()))
                .name(groupChat.getName())
                .description(groupChat.getDescription())
                .membersCount((long) groupChat.getMembers().size()).build();
    }
    
    List<GroupChatPreviewDto> toDtoList(List<GroupChat> groupChats) {
        if (groupChats == null) return null;
        return groupChats.stream().map(this::toDto).collect(Collectors.toList());
    }
    
}
