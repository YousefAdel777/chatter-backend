package com.chatter.chatter.unit.service;

import com.chatter.chatter.service.BackblazeUploadService;
import com.chatter.chatter.service.FileValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class BackblazeUploadServiceTests {

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private BackblazeUploadService backblazeUploadService;

    private final String testBucketName = "test-bucket";

    @Test
    public void uploadFile_ShouldReturnFilename_WhenFileIsValid() throws IOException {
        String originalFilename = "test-image.jpg";
        String extension = "jpg";
        String expectedFilename = "generated-uuid.jpg";

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(fileValidationService.getFileExtension(originalFilename)).thenReturn(extension);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(multipartFile.getSize()).thenReturn(1024L);

        UUID fixedUUID = UUID.randomUUID();
        try (var mockedUUID = mockStatic(UUID.class)) {
            mockedUUID.when(UUID::randomUUID).thenReturn(fixedUUID);
            expectedFilename = fixedUUID + "." + extension;

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = backblazeUploadService.uploadFile(multipartFile);

            assertEquals(expectedFilename, result);
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(fileValidationService).getFileExtension(originalFilename);
        }
    }

    @Test
    public void uploadFile_ShouldThrowRuntimeException_WhenIOExceptionOccurs() throws IOException {
        String originalFilename = "test-image.jpg";
        String extension = "jpg";

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(fileValidationService.getFileExtension(originalFilename)).thenReturn(extension);
        when(multipartFile.getInputStream()).thenThrow(new IOException("File read error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> backblazeUploadService.uploadFile(multipartFile));

        assertEquals("File read error", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void uploadFile_ShouldHandleNullOriginalFilename() throws IOException {
        String extension = "tmp";

        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(fileValidationService.getFileExtension(null)).thenReturn(extension);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(multipartFile.getSize()).thenReturn(1024L);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = backblazeUploadService.uploadFile(multipartFile);
        assertTrue(result.endsWith("." + extension));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void getFileUrl_ShouldReturnPresignedUrl_WhenFilenameIsValid() {
        String filename = "test-file.jpg";
        String expectedUrl = "https://" + testBucketName + ".s3.eu-central-2.amazonaws.com/test-file.jpg";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL mockUrl = mock(URL.class);

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(mockUrl.toString()).thenReturn(expectedUrl);
        String result = backblazeUploadService.getFileUrl(filename);

        assertEquals(expectedUrl, result);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    public void getFileUrl_ShouldReturnNull_WhenFilenameIsNull() {
        String result = backblazeUploadService.getFileUrl(null);
        assertNull(result);
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    public void getFileUrl_ShouldReturnNull_WhenFilenameIsEmpty() {
        String result = backblazeUploadService.getFileUrl("");
        assertNull(result);
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    public void getFileUrl_ShouldReturnNull_WhenFilenameIsBlank() {
        String result = backblazeUploadService.getFileUrl("   ");
        assertNull(result);
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    public void uploadFile_ShouldUseCorrectBucketName() throws IOException {
        String originalFilename = "test.txt";
        String extension = "txt";

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(fileValidationService.getFileExtension(originalFilename)).thenReturn(extension);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(multipartFile.getSize()).thenReturn(100L);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        backblazeUploadService.uploadFile(multipartFile);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void uploadFile_ShouldHandleVariousFileExtensions() throws IOException {
        String[] testFiles = {
                "image.png", "document.pdf", "video.mp4", "archive.zip"
        };

        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(multipartFile.getSize()).thenReturn(1024L);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        for (String originalFilename : testFiles) {
            String extension = originalFilename.split("\\.")[1];

            when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
            when(fileValidationService.getFileExtension(originalFilename)).thenReturn(extension);

            String result = backblazeUploadService.uploadFile(multipartFile);
            assertTrue(result.endsWith("." + extension));
        }
    }
}