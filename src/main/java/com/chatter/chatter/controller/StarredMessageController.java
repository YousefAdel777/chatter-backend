package com.chatter.chatter.controller;

import com.chatter.chatter.dto.StarredMessageDto;
import com.chatter.chatter.mapper.StarredMessageMapper;
import com.chatter.chatter.model.StarredMessage;
import com.chatter.chatter.service.StarredMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/starred-messages")
public class StarredMessageController {

    private final StarredMessageService starredMessageService;

    public StarredMessageController(
        StarredMessageService starredMessageService
    ) {
        this.starredMessageService = starredMessageService;
    }

    @GetMapping
    public ResponseEntity<Page<StarredMessageDto>> getStarredMessages(
            Principal principal,
            @RequestParam(value = "chatId", required = false) Long chatId,
            @PageableDefault Pageable pageable
    ) {
        Page<StarredMessage> starredMessages = starredMessageService.getStarredMessages(principal.getName(), chatId, pageable);
        return ResponseEntity.ok(starredMessages.map(StarredMessageMapper::toDto));
    }

    @PostMapping("/{messageId}/star")
    public ResponseEntity<StarredMessageDto> starMessage(
        @PathVariable Long messageId,
        Principal principal
    ) {
        StarredMessage starredMessage = starredMessageService.starMessage(principal.getName(), messageId);
        return ResponseEntity.status(HttpStatus.CREATED).body(StarredMessageMapper.toDto(starredMessage));
    }

    @DeleteMapping("/{messageId}/unstar")
    public ResponseEntity<StarredMessageDto> unstarMessage(
            @PathVariable Long messageId,
            Principal principal
    ) {
        starredMessageService.unstarMessage(principal.getName(), messageId);
        return ResponseEntity.noContent().build();
    }

}
