package com.chatter.chatter.creator;

import com.chatter.chatter.dto.StoryPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.MediaStory;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryType;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MediaStoryCreator implements StoryCreator {

    private final FileUploadService fileUploadService;

    @Override
    public Story createStory(StoryPostRequest request) {
        validateRequest(request);
        String filePath;
        try {
            filePath = fileUploadService.uploadFile(request.getFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            if (!fileUploadService.isImage(request.getFile())) {
                throw new BadRequestException("file", "Provided file is not a valid image");
            }
        }
        if (request.getStoryType().equals(StoryType.VIDEO) && !fileUploadService.isVideo(request.getFile())) {
            throw new BadRequestException("file", "Provided file is not a valid video");
        }
    }

}
