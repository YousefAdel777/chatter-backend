package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.AttachmentDto;
import com.chatter.chatter.model.Attachment;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.repository.MessageRepository;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AttachmentMapper {

    private final FileUploadService fileUploadService;

    public AttachmentDto toDto(Attachment attachment) {
        if (attachment == null) return null;
        return AttachmentDto.builder()
                .id(attachment.getId())
                .messageId(attachment.getMessage().getId())
                .filePath(fileUploadService.getFileUrl(attachment.getFilePath()))
                .attachmentType(attachment.getAttachmentType())
                .build();
    }

    public List<AttachmentDto> toDtoList(List<Attachment> attachments) {
        if (attachments == null) return null;
        return attachments.stream().map(this::toDto).collect(Collectors.toList());
    }

}
