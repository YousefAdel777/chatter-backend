package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MessagePreviewDto;
import com.chatter.chatter.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessagePreviewMapper {

    private final UserMapper userMapper;

    public MessagePreviewDto toDto(Message message) {
        if (message == null) return null;
        MessagePreviewDto messagePreview = MessagePreviewDto.builder()
                .id(message.getId())
                .user(userMapper.toDto(message.getUser()))
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .content(message.getContent())
                .build();
        if (message instanceof PollMessage pollMessage) {
            messagePreview.setTitle(pollMessage.getTitle());
        }
        if (message instanceof CallMessage callMessage) {
            messagePreview.setMissed(callMessage.getIsMissed());
            messagePreview.setDuration(callMessage.getDuration());
        }
        if (message instanceof FileMessage fileMessage) {
            messagePreview.setOriginalFileName(fileMessage.getOriginalFileName());
            messagePreview.setFileSize(fileMessage.getFileSize());
        }
        if (message instanceof MediaMessage mediaMessage) {
            messagePreview.setAttachmentsCount(mediaMessage.getAttachments().size());
        }
        return messagePreview;
    }

    public List<MessagePreviewDto> toDtoList(List<Message> messages) {
        if (messages == null) return null;
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

}
