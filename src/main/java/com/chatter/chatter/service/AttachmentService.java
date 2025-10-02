package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Attachment;
import com.chatter.chatter.model.AttachmentType;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    @Value("${app.upload.max-file-size}")
    private Long maxImageSize;

    @Value("${app.upload.max-file-size}")
    private Long  maxVideoSize;

    private final AttachmentRepository attachmentRepository;
    private final FileValidationService fileValidationService;
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
        if (fileValidationService.isImage(file)) {
            if (!fileValidationService.isSizeValid(file, maxImageSize)) {
                throw new BadRequestException("mediaFiles", file.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxImageSize / (1024 * 1024)) + " MB.");            }
            attachment.setAttachmentType(AttachmentType.IMAGE);
        }
        else if (fileValidationService.isVideo(file)) {
            if (!fileValidationService.isSizeValid(file, maxVideoSize)) {
                throw new BadRequestException("mediaFiles", file.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxVideoSize / (1024 * 1024)) + " MB.");
            }
            attachment.setAttachmentType(AttachmentType.VIDEO);
        }
        else {
            throw new BadRequestException("mediaFiles", file.getOriginalFilename() + " is not a supported file type. Only images and videos are allowed.");
        }
        attachment.setFilePath(fileUploadService.uploadFile(file));
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
