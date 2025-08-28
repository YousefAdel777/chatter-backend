package com.chatter.chatter.controller;

import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.mapper.StoryMapper;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.request.StoryPatchRequest;
import com.chatter.chatter.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
@CrossOrigin
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final StoryMapper storyMapper;

    @GetMapping
    public ResponseEntity<List<StoryDto>> getStories(
        Principal principal
    ) {
        return ResponseEntity.ok(storyService.getStories(principal.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<StoryDto>> getCurrentUserStories(
            Principal principal
    ) {
        return ResponseEntity.ok(storyService.getCurrentUserStories(principal.getName()));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<StoryDto> getStory(
        @PathVariable Long storyId,
        Principal principal
    ) {
        return ResponseEntity.ok(storyService.getStory(principal.getName(), storyId));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<StoryDto> createStory(
        @RequestPart("story") @Valid StoryPostRequest storyPostRequest,
        @RequestPart(value = "file", required = false) MultipartFile file,
        Principal principal
    ) {
        storyPostRequest.setFile(file);
        Story story = storyService.createStory(principal.getName(), storyPostRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(storyMapper.toDto(story, principal.getName()));
    }

    @PatchMapping("/{storyId}")
    public ResponseEntity<StoryDto> updateStory(
        @RequestBody @Valid StoryPatchRequest request,
        @PathVariable Long storyId,
        Principal principal
    ) {
        Story story = storyService.updateStory(principal.getName(), storyId, request);
        return ResponseEntity.ok(storyMapper.toDto(story, principal.getName()));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable Long storyId,
            Principal principal
    ) {
        storyService.deleteStory(principal.getName(), storyId);
        return ResponseEntity.noContent().build();
    }

}
