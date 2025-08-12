package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.MemberDto;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final UserMapper userMapper;
    private final LastMessageMapper lastMessageMapper;
    private final FileUploadService fileUploadService;

    public ChatDto toDto(Chat chat, String email) {
        if (chat == null) return null;

        ChatDto chatDto = ChatDto.builder()
                .id(chat.getId())
                .lastMessage(lastMessageMapper.toDto(chat.getLastMessage()))
                .unreadMessagesCount(chat.getUnreadMessagesCount(email))
                .firstUnreadMessageId(chat.getFirstUnreadMessageId(email))
                .otherUser(userMapper.toDto(chat.getOtherUser(email)))
                .chatType(chat.getChatType())
                .createdAt(chat.getCreatedAt())
                .membersCount(chat.getMembers().size())
                .build();
        if (chat instanceof GroupChat groupChat) {
//            String[] splitted = ((GroupChat) chat).getImage().split("/");
            chatDto.setImage(fileUploadService.getFileUrl(groupChat.getImage()));
            chatDto.setName(groupChat.getName());
            chatDto.setDescription(groupChat.getDescription());
            chatDto.setChatType(ChatType.GROUP);
            chatDto.setOnlyAdminsCanSend(groupChat.getOnlyAdminsCanSend());
            chatDto.setOnlyAdminsCanInvite(groupChat.getOnlyAdminsCanInvite());
            chatDto.setOnlyAdminsCanEditGroup(groupChat.getOnlyAdminsCanEditGroup());
            chatDto.setOnlyAdminsCanPin(groupChat.getOnlyAdminsCanPin());
        }
        return chatDto;
    }

    public List<ChatDto> toDtoList(List<Chat> chats, String email) {
        if (chats == null) return null;
        return chats.stream().map(chat -> toDto(chat, email)).collect(Collectors.toList());
    }

}
