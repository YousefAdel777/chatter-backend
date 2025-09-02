package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MessageDto;
import com.chatter.chatter.dto.ReactDto;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserMapper userMapper;
    private final ReactMapper reactMapper;
    private final AttachmentMapper attachmentMapper;
    private final StoryMapper storyMapper;
    private final OptionMapper optionMapper;
    private final FileUploadService fileUploadService;
    private final InviteMapper inviteMapper;

    public MessageDto toDto(Message message, String email, Boolean showOriginalMessage) {
        if (message == null) return null;
        List<ReactDto> reactDtos = reactMapper.toDtoList(message.getReacts().stream().toList());
        MessageDto messageDto = MessageDto.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .user(userMapper.toDto(message.getUser()))
                .content(message.getContent())
                .reacts(reactDtos.stream().toList())
                .createdAt(message.getCreatedAt())
                .messageType(message.getMessageType())
                .isSeen(message.isSeen(email))
                .isForwarded(message.isForwarded())
                .isEdited(message.isEdited())
                .pinned(message.getPinned())
                .starred(message.isStarred(email))
                .build();
        if (showOriginalMessage) {
            messageDto.setReplyMessage(toDto(message.getReplyMessage(), email, false));
        }
        if (message.getMessageType().equals(MessageType.MEDIA)) {
            MediaMessage mediaMessage = (MediaMessage) message;
            messageDto.setAttachments(attachmentMapper.toDtoList(mediaMessage.getAttachments()));
        }
        else if (message.getMessageType().equals(MessageType.FILE)) {
            FileMessage fileMessage = (FileMessage) message;
            messageDto.setFileUrl(fileUploadService.getFileUrl(fileMessage.getFilePath()));
            messageDto.setOriginalFileName(fileMessage.getOriginalFileName());
            messageDto.setFileSize(fileMessage.getFileSize());
        }
        else if (message.getMessageType().equals(MessageType.INVITE)) {
            InviteMessage inviteMessage = (InviteMessage) message;
            messageDto.setInvite(inviteMapper.toDto(inviteMessage.getInvite(), email));
        }
        else if (message.getMessageType().equals(MessageType.CALL)) {
            CallMessage callMessage = (CallMessage) message;
            messageDto.setDuration(callMessage.getDuration());
            messageDto.setMissed(callMessage.getIsMissed());
        }
        else if (message.getMessageType().equals(MessageType.AUDIO)) {
            AudioMessage audioMessage = (AudioMessage) message;
            messageDto.setFileUrl(fileUploadService.getFileUrl(audioMessage.getFileUrl()));
        }
        else if (message.getMessageType().equals(MessageType.POLL)) {
            PollMessage pollMessage = (PollMessage) message;
            messageDto.setOptions(optionMapper.toDtoList(pollMessage.getOptions()));
            messageDto.setMultiple(pollMessage.getMultiple());
            messageDto.setTitle(pollMessage.getTitle());
            messageDto.setEndsAt(pollMessage.getEndsAt());
        }
        else if (message.getMessageType().equals(MessageType.STORY)) {
            StoryMessage storyMessage = (StoryMessage) message;
            messageDto.setStory(storyMapper.toDto(storyMessage.getStory(), email));
        }
        return messageDto;
    }

    public List<MessageDto> toDtoList(List<Message> messages, String email) {
        return messages.stream().map(message -> toDto(message, email, true)).collect(Collectors.toList());
    }

}
