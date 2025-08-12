package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.LastMessageDto;
import com.chatter.chatter.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LastMessageMapper {

    private final UserMapper userMapper;

    public LastMessageDto toDto(Message message) {
        if (message == null) return null;
        LastMessageDto lastMessage = LastMessageDto.builder()
                .id(message.getId())
                .user(userMapper.toDto(message.getUser()))
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .content(message.getContent())
                .build();
        if (message instanceof PollMessage) {
            lastMessage.setTitle(((PollMessage) message).getTitle());
        }
        if (message instanceof CallMessage) {
            lastMessage.setMissed(((CallMessage) message).getIsMissed());
            lastMessage.setDuration(((CallMessage) message).getDuration());
        }
        if (message instanceof FileMessage) {
            lastMessage.setOriginalFileName(((FileMessage) message).getOriginalFileName());
        }
        if (message instanceof MediaMessage) {
            lastMessage.setAttachmentsCount(((MediaMessage) message).getAttachments().size());
        }
        return lastMessage;
    }

    public List<LastMessageDto> toDtoList(List<Message> messages) {
        if (messages == null) return null;
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

}
