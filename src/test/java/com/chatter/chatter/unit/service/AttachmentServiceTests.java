package com.chatter.chatter.unit.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Attachment;
import com.chatter.chatter.model.AttachmentType;
import com.chatter.chatter.model.MediaMessage;
import com.chatter.chatter.repository.AttachmentRepository;
import com.chatter.chatter.service.AttachmentService;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.FileValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTests {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private FileValidationService fileValidationService;

    @InjectMocks
    private AttachmentService attachmentService;

    @Mock
    private MultipartFile imageFile;

    @Mock
    private MultipartFile videoFile;

    @Mock
    private MultipartFile invalidFile;

    @Mock
    private MultipartFile oversizedFile;

    @Mock
    private MediaMessage mediaMessage;

    private Attachment attachment;
    private final Long maxFileSize = 10 * 1024 * 1024L;

    @BeforeEach
    public void setup() {
        attachment = Attachment.builder()
                .id(1L)
                .filePath("file.jpg")
                .message(mediaMessage)
                .attachmentType(AttachmentType.IMAGE)
                .build();

        ReflectionTestUtils.setField(attachmentService, "maxImageSize", maxFileSize);
        ReflectionTestUtils.setField(attachmentService, "maxVideoSize", maxFileSize);
    }

    @Test
    void createAttachments_ShouldCreateMultipleAttachments()  {
        when(fileValidationService.isImage(imageFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(imageFile, maxFileSize)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenReturn("image.jpg");
        when(fileValidationService.isVideo(videoFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(videoFile, maxFileSize)).thenReturn(true);
        when(fileUploadService.uploadFile(videoFile)).thenReturn("video.mp4");

        List<Attachment> result = attachmentService.createAttachments(mediaMessage, List.of(imageFile, videoFile));

        assertEquals(2, result.size());
        verify(fileUploadService, times(2)).uploadFile(any(MultipartFile.class));
        result.forEach(att -> assertEquals(mediaMessage, att.getMessage()));
    }

    @Test
    void createAttachments_ShouldHandleEmptyFilesList() {
        List<Attachment> result = attachmentService.createAttachments(mediaMessage, List.of());

        assertTrue(result.isEmpty());
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachments_ShouldThrowException_WhenInvalidFileType()  {
        when(fileValidationService.isImage(invalidFile)).thenReturn(false);
        when(fileValidationService.isVideo(invalidFile)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> attachmentService.createAttachments(mediaMessage, List.of(invalidFile)));
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachments_ShouldThrowException_WhenImageExceedsSize()  {
        when(fileValidationService.isImage(imageFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(imageFile, maxFileSize)).thenReturn(false);
        when(imageFile.getOriginalFilename()).thenReturn("large-image.jpg");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> attachmentService.createAttachments(mediaMessage, List.of(imageFile)));

        assertTrue(exception.getMessage().contains("exceeds the maximum allowed size"));
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachments_ShouldThrowException_WhenVideoExceedsSize()  {
        when(fileValidationService.isImage(videoFile)).thenReturn(false);
        when(fileValidationService.isVideo(videoFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(videoFile, maxFileSize)).thenReturn(false);
        when(videoFile.getOriginalFilename()).thenReturn("large-video.mp4");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachments(mediaMessage, List.of(videoFile));
        });

        assertTrue(exception.getMessage().contains("exceeds the maximum allowed size"));
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachment_ShouldCreateAttachmentFromMultipartFile()  {
        when(fileValidationService.isImage(imageFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(imageFile, maxFileSize)).thenReturn(true);
        when(fileUploadService.uploadFile(imageFile)).thenReturn("image.jpg");

        Attachment result = attachmentService.createAttachment(imageFile);

        assertNotNull(result);
        assertEquals(AttachmentType.IMAGE, result.getAttachmentType());
        assertEquals("image.jpg", result.getFilePath());
        verify(fileUploadService).uploadFile(imageFile);
    }

    @Test
    void createAttachment_ShouldSetVideoType_ForVideoFile()  {
        when(fileValidationService.isImage(videoFile)).thenReturn(false);
        when(fileValidationService.isVideo(videoFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(videoFile, maxFileSize)).thenReturn(true);
        when(fileUploadService.uploadFile(videoFile)).thenReturn("video.mp4");

        Attachment result = attachmentService.createAttachment(videoFile);

        assertEquals(AttachmentType.VIDEO, result.getAttachmentType());
        assertEquals("video.mp4", result.getFilePath());
    }

    @Test
    void createAttachment_ShouldThrowException_ForUnsupportedFileType()  {
        when(fileValidationService.isImage(invalidFile)).thenReturn(false);
        when(fileValidationService.isVideo(invalidFile)).thenReturn(false);
        when(invalidFile.getOriginalFilename()).thenReturn("invalid.exe");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachment(invalidFile);
        });

        assertTrue(exception.getMessage().contains("is not a supported file type"));
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachment_ShouldThrowException_ForOversizedImage()  {
        when(fileValidationService.isImage(oversizedFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(oversizedFile, maxFileSize)).thenReturn(false);
        when(oversizedFile.getOriginalFilename()).thenReturn("huge-image.jpg");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachment(oversizedFile);
        });

        assertTrue(exception.getMessage().contains("exceeds the maximum allowed size"));
        verify(fileUploadService, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void createAttachment_ShouldThrowException_ForOversizedVideo()  {
        when(fileValidationService.isImage(oversizedFile)).thenReturn(false);
        when(fileValidationService.isVideo(oversizedFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(oversizedFile, maxFileSize)).thenReturn(false);
        when(oversizedFile.getOriginalFilename()).thenReturn("huge-video.mp4");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            attachmentService.createAttachment(oversizedFile);
        });

        assertTrue(exception.getMessage().contains("exceeds the maximum allowed size"));
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
}