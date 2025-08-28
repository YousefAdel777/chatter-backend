package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.LastMessageDto;
import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.MessageType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatProjectionMapper {

    public ChatDto toDto(Object[] row) {
        if (row == null) return null;
        int i = 0;

        Long chatId = (Long) row[i++];
        ChatType chatType = (ChatType) row[i++];
        Instant chatCreatedAt = (Instant) row[i++];

        UserDto otherUser = null;
        Long otherUserId = (Long) row[i++];                     // 3
        if (otherUserId != null) {
            String otherUsername = (String) row[i++];           // 4
            String otherEmail = (String) row[i++];              // 5
            Instant otherCreatedAt = (Instant) row[i++];        // 6
            String otherImage = (String) row[i++];              // 7
            String otherBio = (String) row[i++];                // 8
            Instant otherLastOnline = (Instant) row[i++];       // 9
            Boolean otherShowOnlineStatus = (Boolean) row[i++]; // 10
            Boolean otherShowMessageReads = (Boolean) row[i++]; // 11

            otherUser = new UserDto(
                    otherUserId, otherUsername, otherEmail, otherCreatedAt,
                    otherImage, otherBio, otherLastOnline,
                    otherShowOnlineStatus, otherShowMessageReads
            );
        } else {
            i += 8; // Skip the other user fields (4-11)
        }

        // Last message info
        LastMessageDto lastMessage = null;
        Long lastMessageId = (Long) row[i++];                   // 12
        if (lastMessageId != null) {
            MessageType messageType = (MessageType) row[i++];             // 13
            String messageContent = (String) row[i++];          // 14
            Instant messageCreatedAt = (Instant) row[i++];      // 15

            // Last message user info
            Long messageUserId = (Long) row[i++];               // 16
            String messageUsername = (String) row[i++];         // 17
            String messageUserEmail = (String) row[i++];        // 18
            Instant messageUserCreatedAt = (Instant) row[i++];  // 19
            String messageUserImage = (String) row[i++];        // 20
            String messageUserBio = (String) row[i++];          // 21
            Instant messageUserLastOnline = (Instant) row[i++]; // 22
            Boolean messageUserShowOnlineStatus = (Boolean) row[i++]; // 23
            Boolean messageUserShowMessageReads = (Boolean) row[i++]; // 24

            UserDto messageUser = new UserDto(
                    messageUserId, messageUsername, messageUserEmail, messageUserCreatedAt,
                    messageUserImage, messageUserBio, messageUserLastOnline,
                    messageUserShowOnlineStatus, messageUserShowMessageReads
            );

            // Message type specific fields
            String pollTitle = (String) row[i++];               // 25
            Boolean callIsMissed = (Boolean) row[i++];          // 26
            Long callDuration = row[i++] != null ? ((Number) row[i-1]).longValue() : null; // 27
            String fileName = (String) row[i++];                // 28
            Integer mediaCount = row[i++] != null ? ((Number) row[i-1]).intValue() : null;   // 29

            lastMessage = new LastMessageDto(
                    lastMessageId, messageUser, messageType, messageContent, pollTitle,
                    messageCreatedAt, callIsMissed, callDuration, fileName, mediaCount
            );
        } else {
            i += 17; // Skip last message fields (13-29)
        }

        // Stats
        Long unreadCount = row[i++] != null ? ((Number) row[i-1]).longValue() : null;     // 30
        Long memberCount = row[i++] != null ? ((Number) row[i-1]).longValue() : null;     // 31
        Long firstUnreadId = row[i++] != null ? ((Number) row[i-1]).longValue() : null;   // 32

        // Group info
        String groupName = (String) row[i++];                   // 33
        String groupDescription = (String) row[i++];            // 34
        String groupImage = (String) row[i++];                  // 35
        Boolean onlyAdminsCanSend = (Boolean) row[i++];         // 36
        Boolean onlyAdminsCanInvite = (Boolean) row[i++];       // 37
        Boolean onlyAdminsCanEditGroup = (Boolean) row[i++];    // 38
        Boolean onlyAdminsCanPin = (Boolean) row[i];            // 39

        return new ChatDto(
                chatId,
                otherUser,
                lastMessage,
                unreadCount != null ? unreadCount : 0L,
                memberCount != null ? memberCount : 0L,
                firstUnreadId,
                groupName,
                groupDescription,
                groupImage,
                chatType,
                onlyAdminsCanSend != null ? onlyAdminsCanSend : false,
                onlyAdminsCanInvite != null ? onlyAdminsCanInvite : false,
                onlyAdminsCanEditGroup != null ? onlyAdminsCanEditGroup : false,
                onlyAdminsCanPin != null ? onlyAdminsCanPin : false,
                chatCreatedAt
        );
    }

    public List<ChatDto> toDtoList(List<Object[]> rows) {
        if (rows == null) return null;
        return rows.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}