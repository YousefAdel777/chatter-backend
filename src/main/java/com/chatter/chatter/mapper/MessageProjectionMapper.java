package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MessageDto;
import com.chatter.chatter.dto.MessageProjection;
import com.chatter.chatter.dto.ReactDto;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageProjectionMapper {

    private final UserMapper userMapper;
    private final ReactMapper reactMapper;
    private final MessagePreviewMapper messagePreviewMapper;
    private final AttachmentMapper attachmentMapper;
    private final StoryMapper storyMapper;
    private final OptionMapper optionMapper;
    private final FileUploadService fileUploadService;
    private final InviteMapper inviteMapper;
    private final MessageMapper messageMapper;

    public MessageDto toDto(MessageProjection mp, String email, Boolean showOriginalMessage) {
        if (mp == null) return null;
        Message messageProjection = mp.getMessage();
        List<ReactDto> reactDtos = reactMapper.toDtoList(new ArrayList<>(messageProjection.getReacts()));
        MessageDto messageDto = MessageDto.builder()
                .id(messageProjection.getId())
                .chatId(messageProjection.getChat().getId())
                .user(userMapper.toDto(messageProjection.getUser()))
                .content(messageProjection.getContent())
                .reacts(reactDtos)
                .createdAt(messageProjection.getCreatedAt())
                .messageType(messageProjection.getMessageType())
                .isSeen(mp.getIsSeen())
                .isForwarded(messageProjection.isForwarded())
                .isEdited(messageProjection.isEdited())
                .pinned(messageProjection.getPinned())
                .starred(mp.getIsStarred())
                .build();
        if (showOriginalMessage) {
            messageDto.setReplyMessage(messagePreviewMapper.toDto(messageProjection.getReplyMessage().getReplyMessage()));
        }
//        if (messageProjection.getMessageType().equals(MessageType.MEDIA)) {
//            messageDto.setAttachments(attachmentMapper.toDtoList(messageProjection.getAttachments()));
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.FILE)) {
//            messageDto.setFileUrl(fileUploadService.getFileUrl(messageProjection.getFilePath()));
//            messageDto.setOriginalFileName(messageProjection.getOriginalFileName());
//            messageDto.setFileSize(messageProjection.getFileSize());
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.INVITE)) {
//            messageDto.setInvite(inviteMapper.toDto(messageProjection.getInvite(), email));
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.CALL)) {
//            messageDto.setDuration(messageProjection.getDuration());
//            messageDto.setMissed(messageProjection.getMissed());
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.AUDIO)) {
//            messageDto.setFileUrl(fileUploadService.getFileUrl(messageProjection.getFileUrl()));
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.POLL)) {
//            messageDto.setOptions(optionMapper.toDtoList(messageProjection.getOptions()));
//            messageDto.setMultiple(messageProjection.getMultiple());
//            messageDto.setTitle(messageProjection.getTitle());
//            messageDto.setEndsAt(messageProjection.getEndsAt());
//        }
//        else if (messageProjection.getMessageType().equals(MessageType.STORY)) {
//            messageDto.setStory(storyMapper.toDto(messageProjection.getStory(), email));
//        }
        return messageDto;
    }

    public List<MessageDto> toDtoList(List<MessageProjection> messageProjections, String email) {
        return messageProjections.stream().map(messageProjection -> toDto(messageProjection, email, true)).collect(Collectors.toList());
    }
    
}
