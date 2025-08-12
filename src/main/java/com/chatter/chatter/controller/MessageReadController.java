package com.chatter.chatter.controller;

import com.chatter.chatter.dto.MessageReadDto;
import com.chatter.chatter.mapper.MessageReadMapper;
import com.chatter.chatter.model.MessageRead;
import com.chatter.chatter.request.MarkChatMessagesAsReadRequest;
import com.chatter.chatter.request.MessageReadBatchPostRequest;
import com.chatter.chatter.request.MessageReadPostRequest;
import com.chatter.chatter.service.MessageReadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/message-reads")
public class MessageReadController {

    private final MessageReadService messageReadService;
    private final MessageReadMapper messageReadMapper;

    public MessageReadController(
            MessageReadService messageReadService,
            MessageReadMapper messageReadMapper
    ) {
        this.messageReadService = messageReadService;
        this.messageReadMapper = messageReadMapper;
    }

    @GetMapping
    public ResponseEntity<List<MessageReadDto>> getMessageReads(
            Principal principal,
            @RequestParam Long messageId
    ) {
        return ResponseEntity.ok(messageReadService.getMessageReads(principal.getName(), messageId));
    }

    @PostMapping
    public ResponseEntity<MessageReadDto> createMessageRead(
            Principal principal,
            @Valid @RequestBody MessageReadPostRequest request
    ) {
        MessageRead messageRead = messageReadService.createMessageRead(principal.getName(), request.getMessageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(messageReadMapper.toDto(messageRead));
    }

    @PostMapping("/chat")
    public ResponseEntity<List<MessageReadDto>> readAllChatMessages(
            Principal principal,
            @Valid @RequestBody MarkChatMessagesAsReadRequest request
    ) {
        List<MessageRead> messageReads = messageReadService.readChatMessages(principal.getName(), request.getChatId());
        return ResponseEntity.status(HttpStatus.CREATED).body(messageReadMapper.toDtoList(messageReads));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<MessageReadDto>> batchCreateMessageReads(
            Principal principal,
            @Valid @RequestBody MessageReadBatchPostRequest request
    ) {
       List<MessageRead> messageReads = messageReadService.batchCreateMessageReads(principal.getName(), request.getMessagesIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(messageReadMapper.toDtoList(messageReads));
    }

}
