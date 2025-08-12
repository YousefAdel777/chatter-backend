package com.chatter.chatter.creator;

import com.chatter.chatter.request.BaseMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.AudioMessage;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@Component
public class AudioMessageCreator implements MessageCreator  {

    private final FileUploadService fileUploadService;

    @Autowired
    public AudioMessageCreator(
            FileUploadService fileUploadService
    ) {
        this.fileUploadService = fileUploadService;
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType.equals(MessageType.AUDIO);
    }

    @Override
    public Message createMessage(BaseMessageRequest request, String email) {
        validateRequest(request);
        return AudioMessage.builder()
                .fileUrl(uploadFile(request.getFile()))
                .messageType(MessageType.AUDIO)
                .build();
    }

    @Override
    public void validateRequest(BaseMessageRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("message", "File is required");
        }
        if (!fileUploadService.isAudio(file)) {
            throw new BadRequestException("message", "Invalid audio file.");
        }
    }

    private String uploadFile(MultipartFile file) {
        try {
            return fileUploadService.uploadFile(file);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename());
        }
    }

}
