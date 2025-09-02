package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Attachment;
import com.chatter.chatter.model.AttachmentType;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileUploadService fileUploadService;

    @Transactional
    public List<Attachment> createAttachments(MediaMessage mediaMessage, Iterable<MultipartFile> files) {
        ArrayList<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            Attachment attachment = createAttachment(file);
            attachment.setMessage(mediaMessage);
            attachments.add(attachment);
        }
        return attachments;
    }

    public Attachment createAttachment(MultipartFile file) {
        Attachment attachment = new Attachment();
        if (fileUploadService.isImage(file)) {
            attachment.setAttachmentType(AttachmentType.IMAGE);
        }
        else if (fileUploadService.isVideo(file)) {
            attachment.setAttachmentType(AttachmentType.VIDEO);
        }
        else {
            throw new BadRequestException("mediaFiles", "Invalid media file. Only images or videos allowed.");
        }
        try {
            String filePath = fileUploadService.uploadFile(file);
            attachment.setFilePath(filePath);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
        return attachment;
    }

    public Attachment createAttachment(Attachment attachment, MediaMessage mediaMessage) {
        Attachment createdAttachment = Attachment.builder()
                .filePath(attachment.getFilePath())
                .attachmentType(attachment.getAttachmentType())
                .message(mediaMessage)
                .build();
        return attachmentRepository.save(createdAttachment);
    }
}
