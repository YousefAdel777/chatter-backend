package com.chatter.chatter.controller;


import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.GroupChatPatchRequest;
import com.chatter.chatter.dto.GroupChatPostRequest;
import com.chatter.chatter.mapper.ChatMapper;
import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.GroupChat;
import com.chatter.chatter.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final ChatMapper chatMapper;

    @Autowired
    public ChatController(
            ChatService chatService,
            ChatMapper chatMapper
    ) {
        this.chatService = chatService;
        this.chatMapper = chatMapper;
    }

    @GetMapping
    public ResponseEntity<List<ChatDto>> getAllChats(
            Principal principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description
    ) {
        return ResponseEntity.ok(chatService.getAllChatsByEmail(principal.getName(), name, description));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto> getChat(@PathVariable Long chatId, Principal principal) {
        return ResponseEntity.ok(chatService.getChat(principal.getName(), chatId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatDto> createGroupChat(
            Principal principal,
            @RequestPart(value = "groupImage", required = false) MultipartFile groupImage,
            @RequestPart("group") @Valid GroupChatPostRequest request
    ) {
        request.setGroupImage(groupImage);
        GroupChat groupChat = chatService.createGroupChat(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMapper.toDto(groupChat, principal.getName()));
    }

    @PatchMapping(value = "/{chatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatDto> updateGroupChat(
            Principal principal,
            @PathVariable Long chatId,
            @RequestPart(value = "groupImage", required = false) MultipartFile groupImage,
            @RequestPart("group") GroupChatPatchRequest request
    ) {
        request.setGroupImage(groupImage);
        GroupChat groupChat = (GroupChat) chatService.updateGroupChat(principal, chatId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMapper.toDto(groupChat, principal.getName()));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteGroupChat(Principal principal, @PathVariable  Long chatId) {
        chatService.deleteGroupChat(principal, chatId);
        return ResponseEntity.noContent().build();
    }

}
