package com.chatter.chatter.unit.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Attachment;
import com.chatter.chatter.model.AttachmentType;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.repository.AttachmentRepository;
import com.chatter.chatter.service.AttachmentService;
import com.chatter.chatter.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private AttachmentService attachmentService;

    @Mock
    private MultipartFile imageFile;

    @Mock
    private MultipartFile videoFile;

    @Mock
    private MultipartFile invalidFile;

    @Mock
    private MediaMessage mediaMessage;

    private Attachment attachment;

    @BeforeEach
    public void setup() {
        attachment = Attachment.builder()
                .id(1L)
                .filePath("file.jpg")
                .message(mediaMessage)
                .attachmentType(AttachmentType.IMAGE)
                .build();
    }

    @Test
    void createAttachments_ShouldCreateMultipleAttachments() throws IOException {
        when(fileUploadService.isImage(imageFile)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenReturn("image.jpg");
        when(fileUploadService.isVideo(videoFile)).thenReturn(true);
        when(fileUploadService.uploadFile(videoFile)).thenReturn("video.mp4");

        List<Attachment> result = attachmentService.createAttachments(mediaMessage, List.of(imageFile, videoFile));

        assertEquals(2, result.size());
        verify(fileUploadService, times(2)).uploadFile(any(MultipartFile.class));
        result.forEach(att -> assertEquals(mediaMessage, att.getMessage()));
    }

    @Test
    void createAttachments_ShouldHandleEmptyFilesList() throws IOException {
        List<Attachment> result = attachmentService.createAttachments(mediaMessage, List.of());

        assertTrue(result.isEmpty());
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachments_ShouldThrowException_WhenInvalidFileType() throws IOException {
        when(fileUploadService.isImage(invalidFile)).thenReturn(false);
        when(fileUploadService.isVideo(invalidFile)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachments(mediaMessage, List.of(invalidFile));
        });

        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachments_ShouldThrowRuntimeException_WhenUploadFails() throws IOException {
        when(fileUploadService.isImage(imageFile)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenThrow(new IOException("Upload failed"));

        assertThrows(RuntimeException.class, () -> {
            attachmentService.createAttachments(mediaMessage, List.of(imageFile));
        });
    }

    @Test
    void createAttachment_ShouldCreateAttachmentFromMultipartFile() throws IOException {
        when(fileUploadService.isImage(imageFile)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenReturn("image.jpg");

        Attachment result = attachmentService.createAttachment(imageFile);

        assertNotNull(result);
        assertEquals(AttachmentType.IMAGE, result.getAttachmentType());
        assertEquals("image.jpg", result.getFilePath());
        verify(fileUploadService).uploadFile(imageFile);
    }

    @Test
    void createAttachment_ShouldSetVideoType_ForVideoFile() throws IOException {
        when(fileUploadService.isImage(videoFile)).thenReturn(false);
        when(fileUploadService.isVideo(videoFile)).thenReturn(true);
        when(fileUploadService.uploadFile(videoFile)).thenReturn("video.mp4");

        Attachment result = attachmentService.createAttachment(videoFile);

        assertEquals(AttachmentType.VIDEO, result.getAttachmentType());
        assertEquals("video.mp4", result.getFilePath());
    }

    @Test
    void createAttachment_ShouldThrowException_ForUnsupportedFileType() throws IOException {
        when(fileUploadService.isImage(invalidFile)).thenReturn(false);
        when(fileUploadService.isVideo(invalidFile)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachment(invalidFile);
        });

        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachmentFromEntity_ShouldCreateAttachment() {
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

        Attachment result = attachmentService.createAttachment(attachment, mediaMessage);

        assertNotNull(result);
        assertEquals(mediaMessage, result.getMessage());
        assertEquals(attachment.getFilePath(), result.getFilePath());
        assertEquals(attachment.getAttachmentType(), result.getAttachmentType());
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void createAttachmentFromEntity_ShouldCopyAllProperties() {
        Attachment sourceAttachment = Attachment.builder()
                .filePath("test.jpg")
                .attachmentType(AttachmentType.VIDEO)
                .build();

        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Attachment result = attachmentService.createAttachment(sourceAttachment, mediaMessage);

        assertEquals("test.jpg", result.getFilePath());
        assertEquals(AttachmentType.VIDEO, result.getAttachmentType());
        assertEquals(mediaMessage, result.getMessage());
    }

    @Test
    void createAttachment_ShouldHandleIOException() throws IOException {
        when(fileUploadService.isImage(imageFile)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenThrow(new IOException("Network error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            attachmentService.createAttachment(imageFile);
        });

        assertTrue(exception.getMessage().contains("Failed to upload file"));
        assertTrue(exception.getMessage().contains("Network error"));
    }
}