package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.MessageDto;
import com.chatter.chatter.dto.MessageProjection;
import com.chatter.chatter.dto.ReactDto;
import com.chatter.chatter.model.*;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
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
    private final MentionMapper mentionMapper;
    private final MessagePreviewMapper messagePreviewMapper;

//    public MessageDto toDto(Message message, String email, Boolean showOriginalMessage) {
//        if (message == null) return null;
//        return toDtoCommon(
//                message.getId(),
//                message.getChat().getId(),
//                message.getUser(),
//                message.getContent(),
//                message.getContentJson(),
//                new ArrayList<>(message.getReacts()),
//                new ArrayList<>(message.getMentions()),
//                message.getIsEveryoneMentioned(),
//                message.getCreatedAt(),
//                message.getMessageType(),
//                message.isForwarded(),
//                message.isEdited(),
//                message.getPinned(),
//                message.isSeen(email),
//                message.isStarred(email),
//                message.getReplyMessage(),
//                message,
//                email,
//                showOriginalMessage
//        );
//    }

//    public List<MessageDto> toDtoList(List<Message> messages, String email) {
//        return messages.stream().map(message -> toDto(message, email, true)).collect(Collectors.toList());
//    }

    
    public MessageDto toDto(MessageProjection mp, String email, Boolean showOriginalMessage) {
        if (mp == null) return null;
        Message message = mp.getMessage();
        return toDtoCommon(
                message.getId(),
                message.getChat().getId(),
                message.getUser(),
                message.getContent(),
                message.getContentJson(),
                new ArrayList<>(message.getReacts()),
                new ArrayList<>(message.getMentions()),
                message.getIsEveryoneMentioned(),
                message.getCreatedAt(),
                message.getMessageType(),
                message.isForwarded(),
                message.isEdited(),
                message.getPinned(),
                mp.getIsSeen(),
                mp.getIsStarred(),
                message.getReplyMessage(),
                message,
                email,
                showOriginalMessage
        );
    }

    public List<MessageDto> toDtoListFromProjections(List<MessageProjection> messageProjections, String email) {
        return messageProjections.stream().map(mp -> toDto(mp, email, true)).collect(Collectors.toList());
    }
    
    private MessageDto toDtoCommon(
            Long id,
            Long chatId,
            User user,
            String content,
            String contentJson,
            List<React> reacts,
            List<Mention> mentions,
            Boolean isEveryoneMentioned,
            Instant createdAt,
            MessageType messageType,
            boolean isForwarded,
            boolean isEdited,
            Boolean pinned,
            Boolean isSeen,
            Boolean isStarred,
            Message replyMessage,
            Message originalMessage,
            String email,
            Boolean showOriginalMessage
    ) {
        List<ReactDto> reactDtos = reactMapper.toDtoList(reacts);
        MessageDto messageDto = MessageDto.builder()
                .id(id)
                .chatId(chatId)
                .user(userMapper.toDto(user))
                .content(content)
                .contentJson(contentJson)
                .reacts(reactDtos)
                .createdAt(createdAt)
                .messageType(messageType)
                .isSeen(isSeen)
                .isForwarded(isForwarded)
                .isEdited(isEdited)
                .pinned(pinned)
                .starred(isStarred)
                .isEveryoneMentioned(isEveryoneMentioned)
                .mentions(mentionMapper.toDtoList(new ArrayList<>(mentions)))
                .build();

        if (showOriginalMessage && replyMessage != null) {
            messageDto.setReplyMessage(messagePreviewMapper.toDto(replyMessage));
        }

        if (messageType.equals(MessageType.MEDIA)) {
            messageDto.setAttachments(attachmentMapper.toDtoList(((MediaMessage) originalMessage).getAttachments()));
        } else if (messageType.equals(MessageType.FILE)) {
            FileMessage fileMessage = (FileMessage) originalMessage;
            messageDto.setFileUrl(fileUploadService.getFileUrl(fileMessage.getFilePath()));
            messageDto.setOriginalFileName(fileMessage.getOriginalFileName());
            messageDto.setFileSize(fileMessage.getFileSize());
        } else if (messageType.equals(MessageType.INVITE)) {
            InviteMessage inviteMessage = (InviteMessage) originalMessage;
            messageDto.setInvite(inviteMapper.toDto(inviteMessage.getInvite()));
        } else if (messageType.equals(MessageType.CALL)) {
            CallMessage callMessage = (CallMessage) originalMessage;
            messageDto.setDuration(callMessage.getDuration());
            messageDto.setMissed(callMessage.getIsMissed());
        } else if (messageType.equals(MessageType.AUDIO)) {
            AudioMessage audioMessage = (AudioMessage) originalMessage;
            messageDto.setFileUrl(fileUploadService.getFileUrl(audioMessage.getFileUrl()));
        } else if (messageType.equals(MessageType.POLL)) {
            PollMessage pollMessage = (PollMessage) originalMessage;
            messageDto.setOptions(optionMapper.toDtoList(pollMessage.getOptions()));
            messageDto.setMultiple(pollMessage.getMultiple());
            messageDto.setTitle(pollMessage.getTitle());
            messageDto.setEndsAt(pollMessage.getEndsAt());
        } else if (messageType.equals(MessageType.STORY)) {
            StoryMessage storyMessage = (StoryMessage) originalMessage;
            messageDto.setStory(storyMapper.toDto(storyMessage.getStory(), null, email));
        }

        return messageDto;
    }
}
