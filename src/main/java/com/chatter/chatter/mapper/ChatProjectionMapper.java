package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.MessagePreviewDto;
import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatProjectionMapper {

    private final FileUploadService fileUploadService;

    public ChatDto toDto(Object[] row) {
        if (row == null) return null;
        int i = 0;

        Long chatId = (Long) row[i++];
        ChatType chatType = (ChatType) row[i++];
        Instant chatCreatedAt = (Instant) row[i++];

        UserDto otherUser = null;
        Long otherUserId = (Long) row[i++];                     
        if (otherUserId != null) {
            String otherUsername = (String) row[i++];
            String otherEmail = (String) row[i++];
            Instant otherCreatedAt = (Instant) row[i++];
            String otherImage = fileUploadService.getFileUrl((String) row[i++]);
            String otherBio = (String) row[i++];
            Instant otherLastOnline = (Instant) row[i++];
            Boolean otherShowOnlineStatus = (Boolean) row[i++];
            Boolean otherShowMessageReads = (Boolean) row[i++];

            otherUser = new UserDto(
                    otherUserId, otherUsername, otherEmail, otherCreatedAt,
                    otherImage, otherBio, otherLastOnline,
                    otherShowOnlineStatus, otherShowMessageReads
            );
        } else {
            i += 8; 
        }

        MessagePreviewDto lastMessage = null;
        Long lastMessageId = (Long) row[i++];
        if (lastMessageId != null) {
            MessageType messageType = (MessageType) row[i++];             
            String messageContent = (String) row[i++];          
            Instant messageCreatedAt = (Instant) row[i++];      

            Long messageUserId = (Long) row[i++];               
            String messageUsername = (String) row[i++];
            String messageUserEmail = (String) row[i++];        
            Instant messageUserCreatedAt = (Instant) row[i++];  
            String messageUserImage = fileUploadService.getFileUrl((String) row[i++]);        
            String messageUserBio = (String) row[i++];          
            Instant messageUserLastOnline = (Instant) row[i++]; 
            Boolean messageUserShowOnlineStatus = (Boolean) row[i++]; 
            Boolean messageUserShowMessageReads = (Boolean) row[i++]; 

            UserDto messageUser = new UserDto(
                    messageUserId, messageUsername, messageUserEmail, messageUserCreatedAt,
                    messageUserImage, messageUserBio, messageUserLastOnline,
                    messageUserShowOnlineStatus, messageUserShowMessageReads
            );

            
            String pollTitle = (String) row[i++];               
            Boolean callIsMissed = (Boolean) row[i++];          
            Long callDuration = row[i++] != null ? ((Number) row[i-1]).longValue() : null; 
            String fileName = (String) row[i++];                
            Integer mediaCount = row[i++] != null ? ((Number) row[i-1]).intValue() : null;

        } else {
            i += 17; 
        }
        
        Long unreadCount = row[i++] != null ? ((Number) row[i-1]).longValue() : null;     
        Long memberCount = row[i++] != null ? ((Number) row[i-1]).longValue() : null;     
        Long firstUnreadId = row[i++] != null ? ((Number) row[i-1]).longValue() : null;   

        Boolean isMentioned = (Boolean) row[i++];
        String groupName = (String) row[i++];                   
        String groupDescription = (String) row[i++];            
        String groupImage = fileUploadService.getFileUrl((String) row[i++]);                  
        Boolean onlyAdminsCanSend = (Boolean) row[i++];         
        Boolean onlyAdminsCanInvite = (Boolean) row[i++];       
        Boolean onlyAdminsCanEditGroup = (Boolean) row[i++];    
        Boolean onlyAdminsCanPin = (Boolean) row[i];            

        return new ChatDto(
                chatId,
                otherUser,
                lastMessage,
                unreadCount != null ? unreadCount : 0L,
                memberCount != null ? memberCount : 0L,
                firstUnreadId,
                isMentioned,
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
        return rows.stream().map(this::toDto).collect(Collectors.toList());
    }
}