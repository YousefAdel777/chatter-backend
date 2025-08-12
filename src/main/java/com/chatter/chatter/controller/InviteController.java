package com.chatter.chatter.controller;

import com.chatter.chatter.dto.InviteDto;
import com.chatter.chatter.mapper.InviteMapper;
import com.chatter.chatter.model.Invite;
import com.chatter.chatter.request.InvitePostRequest;
import com.chatter.chatter.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;
    private final InviteMapper inviteMapper;

    @GetMapping("/{inviteId}")
    public ResponseEntity<InviteDto> getInvite(
        @PathVariable Long inviteId,
        Principal principal
    ) {
        return ResponseEntity.ok(inviteService.getInvite(principal.getName(), inviteId));
    }

    @PostMapping
    public ResponseEntity<InviteDto> createInvite(
            @RequestBody @Valid InvitePostRequest request,
            Principal principal
    ) {
       Invite invite = inviteService.createInvite(principal.getName(), request);
       return ResponseEntity.status(HttpStatus.CREATED).body(inviteMapper.toDto(invite, principal.getName()));
    }

    @PostMapping("/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(
            @PathVariable Long inviteId,
            Principal principal
    ) {
        inviteService.acceptInvite(principal.getName(), inviteId, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

}
