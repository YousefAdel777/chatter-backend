package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Component
public class MediaMessageCreator implements MessageCreator {

    private final AttachmentService attachmentService;

    @Autowired
    public MediaMessageCreator(
            AttachmentService attachmentService
    ) {
        this.attachmentService = attachmentService;
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.MEDIA);
    }

    @Transactional
    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        MediaMessage mediaMessage = new MediaMessage();
        mediaMessage.setAttachments(attachmentService.createAttachments(mediaMessage, request.getMediaFiles()));
        mediaMessage.setMessageType(MessageType.MEDIA);
        return mediaMessage;
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        List<MultipartFile> files = request.getMediaFiles();
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("mediaFiles", "At least 1 file is required");
        }
    }
}
