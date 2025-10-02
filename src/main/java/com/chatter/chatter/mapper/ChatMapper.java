package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.ChatStatusProjection;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final UserMapper userMapper;
    private final MessagePreviewMapper messagePreviewMapper;
    private final FileUploadService fileUploadService;

    public ChatDto toDto(Chat chat, ChatStatusProjection statusProjection, String email) {
        if (chat == null) return null;

        ChatDto.ChatDtoBuilder chatDtoBuilder = ChatDto.builder()
                .id(chat.getId())
                .lastMessage(messagePreviewMapper.toDto(chat.getLastMessage()))
                .otherUser(userMapper.toDto(chat.getOtherUser(email)))
                .chatType(chat.getChatType())
                .createdAt(chat.getCreatedAt());
        if (statusProjection != null) {
            chatDtoBuilder
                    .membersCount(statusProjection.getMembersCount())
                    .firstUnreadMessageId(statusProjection.getFirstUnreadMessageId())
                    .isMentioned(statusProjection.getIsMentioned())
                    .unreadMessagesCount(statusProjection.getUnreadMessagesCount());
        }
        if (chat instanceof GroupChat groupChat) {
            chatDtoBuilder
                    .image(fileUploadService.getFileUrl(groupChat.getImage()))
                    .name(groupChat.getName())
                    .description(groupChat.getDescription())
                    .chatType(ChatType.GROUP)
                    .onlyAdminsCanSend(groupChat.getOnlyAdminsCanSend())
                    .onlyAdminsCanSend(groupChat.getOnlyAdminsCanSend())
                    .onlyAdminsCanEditGroup(groupChat.getOnlyAdminsCanEditGroup())
                    .onlyAdminsCanPin(groupChat.getOnlyAdminsCanPin());
        }
        return chatDtoBuilder.build();
    }

    public List<ChatDto> toDtoList(List<Chat> chats, List<ChatStatusProjection> projections, String email) {
        if (chats == null) return null;
        if (projections != null) {
            Map<Long, ChatStatusProjection> projectionMap = projections.stream().collect(Collectors.toMap(ChatStatusProjection::getId, projection -> projection));
            return chats.stream().map(chat -> toDto(chat, projectionMap.get(chat.getId()), email)).collect(Collectors.toList());
        }
        return chats.stream().map(chat -> toDto(chat, null, email)).collect(Collectors.toList());
    }

}
