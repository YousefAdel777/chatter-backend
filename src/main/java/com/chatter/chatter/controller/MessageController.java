package com.chatter.chatter.controller;

import com.chatter.chatter.dto.*;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.mapper.MemberMapper;
import com.chatter.chatter.mapper.MessageMapper;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.MessageType;
import com.chatter.chatter.request.BatchMessageRequest;
import com.chatter.chatter.request.ForwardMessagePostRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.service.MessageService;
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
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private final MemberMapper memberMapper;

    @GetMapping
    public ResponseEntity<CursorResponseDto<MessageDto>> getAllMessages(
            @RequestParam(required = false) Long chatId,
            @RequestParam(required = false) MessageType messageType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        if (after != null && before != null) {
            throw new BadRequestException("message", "Only before or after can be used");
        }
        List<MessageDto> messages = messageService.getAllMessages(principal.getName(), chatId, search, messageType, pinned, starred, before, after, size);

        Long nextCursor = messages.isEmpty() ? null : messages.getLast().getId();
        Long previousCursor = messages.isEmpty() ? null : messages.getFirst().getId();
        CursorResponseDto<MessageDto> cursorResponseDto = new CursorResponseDto<>(messages, nextCursor, previousCursor);
        return ResponseEntity.ok(cursorResponseDto);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDto> getMessage(Principal principal, @PathVariable Long messageId) {
        Message message = messageService.getMessageEntity(principal.getName(), messageId);
        return ResponseEntity.ok(messageMapper.toDto(message, principal.getName(), true));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDto> createMessage(
            Principal principal,
            @RequestPart(name = "message") @Valid SingleMessageRequest request,
            @RequestPart(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        request.setMediaFiles(mediaFiles);
        request.setFile(file);
        Message message = messageService.createMessage(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toDto(message, principal.getName(), true));
    }

    @PatchMapping("/{messageId}/pin")
    public ResponseEntity<MessageDto> pinMessage(
            Principal principal,
            @PathVariable Long messageId
    ) {
        Message message = messageService.updateMessagePin(principal.getName(), messageId, true);
        return ResponseEntity.ok(messageMapper.toDto(message, principal.getName(), true));
    }

    @PatchMapping("/{messageId}/unpin")
    public ResponseEntity<MessageDto> unpinMessage(
            Principal principal,
            @PathVariable Long messageId
    ) {
        Message message = messageService.updateMessagePin(principal.getName(), messageId, false);
        return ResponseEntity.ok(messageMapper.toDto(message, principal.getName(), true));
    }

    @PostMapping(value = "/batch", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<List<MessageDto>> createMessages(
            Principal principal,
            @RequestPart(name = "message") @Valid BatchMessageRequest request,
            @RequestPart(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        request.setMediaFiles(mediaFiles);
        request.setFile(file);
        List<Message> messages = messageService.createMessages(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toDtoList(messages, principal.getName()));
    }

    @PostMapping("/forward-message")
    public ResponseEntity<List<MessageDto>> forwardMessage(
            Principal principal,
            @RequestBody @Valid ForwardMessagePostRequest forwardMessagePostRequest
    ) {
        List<Message> messages = messageService.forwardMessage(principal.getName(), forwardMessagePostRequest.getMessageId(), forwardMessagePostRequest.getChatIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toDtoList(messages, principal.getName()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId, Principal principal) {
        messageService.deleteMessage(principal.getName(), messageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageDto> updateMessage(@PathVariable Long messageId, @Valid @RequestBody MessagePatchRequest messagePatchRequest, Principal principal) {
        Message message = messageService.updateMessage(principal.getName(), messageId, messagePatchRequest);
        return ResponseEntity.ok(messageMapper.toDto(message, principal.getName(), true));
    }

    @PostMapping("/accept-invite/{messageId}")
    public ResponseEntity<MemberDto> acceptInvite(Principal principal, @PathVariable Long messageId) {
        Member member = messageService.acceptInvite(principal.getName(), messageId);
        return ResponseEntity.ok(memberMapper.toDto(member));
    }
}
