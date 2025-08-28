package com.chatter.chatter.controller;

import com.chatter.chatter.dto.StoryViewDto;
import com.chatter.chatter.request.StoryViewPostRequest;
import com.chatter.chatter.mapper.StoryViewMapper;
import com.chatter.chatter.model.StoryView;
import com.chatter.chatter.service.StoryViewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/story-views")
@RequiredArgsConstructor
public class StoryViewController {

    private final StoryViewService storyViewService;
    private final StoryViewMapper storyViewMapper;

    @GetMapping
    public ResponseEntity<List<StoryViewDto>> getStoryViews(
            @RequestParam Long storyId,
            Principal principal
    ) {
        List<StoryViewDto> storyViews = storyViewService.getStoryViews(principal.getName(), storyId);
        return ResponseEntity.ok(storyViews);
    }

    @PostMapping
    public ResponseEntity<StoryViewDto> createStoryView(
        @RequestBody @Valid StoryViewPostRequest request,
        Principal principal
    ) {
        StoryView storyView = storyViewService.createStoryView(principal.getName(), request.getStoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(storyViewMapper.toDto(storyView));
    }

}
