package com.chatter.chatter.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private static final Set<String> allowedImageMimeTypes = Set.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff", "image/svg+xml");
    private static final Set<String> allowedVideoMimeTypes = Set.of("video/mp4", "video/quicktime", "video/x-msvideo", "video/x-matroska", "video/webm");
    private static final Set<String> allowedAudioMimeTypes = Set.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/webm");

    private final Tika tika;

    public boolean isImage(MultipartFile file) {
        return validateType(file, allowedImageMimeTypes);
    }

    public boolean isVideo(MultipartFile file) {
        return validateType(file, allowedVideoMimeTypes);
    }

    public boolean isAudio(MultipartFile file) {
        return validateType(file, allowedAudioMimeTypes);
    }

    public boolean isSizeValid(MultipartFile file, Long size) {
        return file.getSize() <= size;
    }

    public String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private boolean validateType(MultipartFile file, Set<String> mimeTypes) {
        if (file == null || file.getContentType() == null || file.isEmpty()) return false;
        try {
            String mimeType = tika.detect(file.getInputStream());
            return mimeTypes.contains(mimeType);
        }
        catch (IOException e) {
            return false;
        }
    }

}
