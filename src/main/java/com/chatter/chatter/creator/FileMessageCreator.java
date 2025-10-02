package com.chatter.chatter.creator;


import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.FileMessage;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@Component
@RequiredArgsConstructor
public class FileMessageCreator implements MessageCreator {

    @Value("${app.upload.max-file-size}")
    private Long maxFileSize;

    private final FileUploadService fileUploadService;
    private final FileValidationService fileValidationService;

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.FILE);
    }

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        MultipartFile file = request.getFile();
        return FileMessage.builder()
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .filePath(fileUploadService.uploadFile(file))
                .messageType(MessageType.FILE)
                .build();
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file", "File is required");
        }
        if (!fileValidationService.isSizeValid(file, maxFileSize)) {
            throw new BadRequestException("file", file.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxFileSize / (1024 * 1024)) + " MB.");
        }
    }

}
