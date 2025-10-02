package com.chatter.chatter.unit.service;

import com.chatter.chatter.service.FileValidationService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileValidationServiceTests {

    @Mock
    private Tika tika;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileValidationService fileValidationService;

    @Test
    void isImage_ShouldReturnTrue_WhenFileIsValidImage() throws IOException {
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");

        boolean result = fileValidationService.isImage(multipartFile);

        assertTrue(result);
        verify(tika).detect(any(InputStream.class));
    }

    @Test
    void isImage_ShouldReturnFalse_WhenFileIsNotImage() throws IOException {
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");

        boolean result = fileValidationService.isImage(multipartFile);

        assertFalse(result);
    }

    @Test
    void isImage_ShouldReturnFalse_WhenFileIsNull() {
        boolean result = fileValidationService.isImage(null);

        assertFalse(result);
    }

    @Test
    void isImage_ShouldReturnFalse_WhenFileIsEmpty() {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.isEmpty()).thenReturn(true);
        boolean result = fileValidationService.isImage(multipartFile);
        assertFalse(result);
    }

    @Test
    void isImage_ShouldReturnFalse_WhenContentTypeIsNull() {
        when(multipartFile.getContentType()).thenReturn(null);
        boolean result = fileValidationService.isImage(multipartFile);
        assertFalse(result);
    }

    @Test
    void isImage_ShouldReturnFalse_WhenIOExceptionOccurs() throws IOException {
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenThrow(new IOException("File error"));

        boolean result = fileValidationService.isImage(multipartFile);
        assertFalse(result);
    }

    @Test
    void isVideo_ShouldReturnTrue_WhenFileIsValidVideo() throws IOException {
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("video/mp4");

        boolean result = fileValidationService.isVideo(multipartFile);
        assertTrue(result);
    }

    @Test
    void isVideo_ShouldReturnFalse_WhenFileIsNotVideo() throws IOException {
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("image/jpeg");

        boolean result = fileValidationService.isVideo(multipartFile);
        assertFalse(result);
    }

    @Test
    void isAudio_ShouldReturnTrue_WhenFileIsValidAudio() throws IOException {
        when(multipartFile.getContentType()).thenReturn("audio/mpeg");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("audio/mpeg");

        boolean result = fileValidationService.isAudio(multipartFile);
        assertTrue(result);
    }

    @Test
    void isAudio_ShouldReturnFalse_WhenFileIsNotAudio() throws IOException {
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("video/mp4");

        boolean result = fileValidationService.isAudio(multipartFile);
        assertFalse(result);
    }

    @Test
    void isSizeValid_ShouldReturnTrue_WhenFileSizeIsWithinLimit() {
        when(multipartFile.getSize()).thenReturn(1024L);
        boolean result = fileValidationService.isSizeValid(multipartFile, 2048L);
        assertTrue(result);
    }

    @Test
    void isSizeValid_ShouldReturnFalse_WhenFileSizeExceedsLimit() {
        when(multipartFile.getSize()).thenReturn(3072L);
        boolean result = fileValidationService.isSizeValid(multipartFile, 2048L);
        assertFalse(result);
    }

    @Test
    void isSizeValid_ShouldReturnTrue_WhenFileSizeEqualsLimit() {
        when(multipartFile.getSize()).thenReturn(2048L);
        boolean result = fileValidationService.isSizeValid(multipartFile, 2048L);
        assertTrue(result);
    }

    @Test
    void getFileExtension_ShouldReturnExtension_WhenFileNameHasExtension() {
        String result = fileValidationService.getFileExtension("document.pdf");
        assertEquals("pdf", result);
    }

    @Test
    void getFileExtension_ShouldReturnEmptyString_WhenFileNameHasNoExtension() {
        String result = fileValidationService.getFileExtension("filename");
        assertEquals("", result);
    }

    @Test
    void getFileExtension_ShouldReturnEmptyString_WhenFileNameIsNull() {
        String result = fileValidationService.getFileExtension(null);
        assertEquals("", result);
    }

    @Test
    void getFileExtension_ShouldReturnEmptyString_WhenFileNameEndsWithDot() {
        String result = fileValidationService.getFileExtension("filename.");
        assertEquals("", result);
    }

    @Test
    void validateType_ShouldHandleAllAllowedImageMimeTypes() throws IOException {
        String[] imageMimeTypes = {"image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff", "image/svg+xml"};

        for (String mimeType : imageMimeTypes) {
            when(multipartFile.getContentType()).thenReturn(mimeType);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
            when(tika.detect(any(InputStream.class))).thenReturn(mimeType);

            boolean result = fileValidationService.isImage(multipartFile);

            assertTrue(result, "Should return true for MIME type: " + mimeType);
        }
    }

    @Test
    void validateType_ShouldHandleAllAllowedVideoMimeTypes() throws IOException {
        String[] videoMimeTypes = {"video/mp4", "video/quicktime", "video/x-msvideo", "video/x-matroska", "video/webm"};

        for (String mimeType : videoMimeTypes) {
            when(multipartFile.getContentType()).thenReturn(mimeType);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
            when(tika.detect(any(InputStream.class))).thenReturn(mimeType);

            boolean result = fileValidationService.isVideo(multipartFile);

            assertTrue(result, "Should return true for MIME type: " + mimeType);
        }
    }

    @Test
    void validateType_ShouldHandleAllAllowedAudioMimeTypes() throws IOException {
        String[] audioMimeTypes = {"audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/webm"};

        for (String mimeType : audioMimeTypes) {
            when(multipartFile.getContentType()).thenReturn(mimeType);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
            when(tika.detect(any(InputStream.class))).thenReturn(mimeType);

            boolean result = fileValidationService.isAudio(multipartFile);

            assertTrue(result, "Should return true for MIME type: " + mimeType);
        }
    }

    @Test
    void validateType_ShouldUseTikaDetectionOverContentType() throws IOException {
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(mock(InputStream.class));
        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");

        boolean result = fileValidationService.isImage(multipartFile);

        assertFalse(result);
        verify(tika).detect(any(InputStream.class));
    }
}