package com.chatter.chatter.creator;

import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.MediaStory;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryType;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MediaStoryCreator implements StoryCreator {

    @Value("${app.story.max-video-size}")
    private Long maxVideoSize;

    @Value("${app.story.max-image-size}")
    private Long maxImageSize;

    private final FileValidationService fileValidationService;
    private final FileUploadService fileUploadService;

    @Override
    public Story createStory(StoryPostRequest request) {
        validateRequest(request);
        String filePath = fileUploadService.uploadFile(request.getFile());
        return MediaStory.builder()
                .filePath(filePath)
                .content(request.getContent())
                .storyType(request.getStoryType())
                .build();
    }

    @Override
    public boolean supports(StoryType storyType) {
        return storyType.equals(StoryType.VIDEO) || storyType.equals(StoryType.IMAGE);
    }

    private void validateRequest(StoryPostRequest request) {
        MultipartFile file = request.getFile();
        if (file == null) {
            throw new BadRequestException("file", "File is required");
        }
        if (request.getStoryType().equals(StoryType.IMAGE)) {
            if (!fileValidationService.isImage(file)) {
                throw new BadRequestException("file", "Provided file is not a valid image");
            }
            if (!fileValidationService.isSizeValid(file, maxImageSize)) {
                throw new BadRequestException("file", file.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxImageSize / (1024 * 1024)) + " MB.");}
        }
        if (request.getStoryType().equals(StoryType.VIDEO)) {
            if (!fileValidationService.isVideo(file)) {
                throw new BadRequestException("file", "Provided file is not a valid video");
            }
            if (!fileValidationService.isSizeValid(file, maxVideoSize)) {
                throw new BadRequestException("file", file.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxVideoSize / (1024 * 1024)) + " MB.");
            }
        }
    }

}
