package com.chatter.chatter.service;

import com.chatter.chatter.dto.InviteDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.InviteMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.InviteRepository;
import com.chatter.chatter.request.InvitePostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final MemberService memberService;
    private final ChatService chatService;
    private final InviteMapper inviteMapper;

    @Transactional
    public Invite createInvite(String email, InvitePostRequest invitePostRequest) {
        Instant expiresAt = invitePostRequest.getExpiresAt();
        Chat chat = chatService.getChatEntityIfMember(email, invitePostRequest.getInviteChatId());
        if (!ChatType.GROUP.equals(chat.getChatType())) {
            throw new BadRequestException("message", "Only group chats can have invites.");
        }
        GroupChat groupChat = (GroupChat) chat;
        if (groupChat.getOnlyAdminsCanInvite() && !memberService.isAdmin(email, groupChat.getId())) {
            throw new ForbiddenException("Only group admins can invite.");
        }
        Invite invite = Invite.builder()
                .groupChat(groupChat)
                .expiresAt(expiresAt)
                .canUseLink(invitePostRequest.getCanUseLink())
                .build();
        return inviteRepository.save(invite);
    }

    public Invite getInviteEntity(Long inviteId) {
        return inviteRepository.findById(inviteId).orElseThrow(() -> new NotFoundException("message", "Invite not found."));
    }

    @Cacheable(value = "invites", key = "'email:' + #email + ':inviteId:' + #inviteId")
    public InviteDto getInvite(String email, Long inviteId) {
        return inviteMapper.toDto(getInviteEntity(inviteId), email);
    }

    @Transactional
    public void acceptInvite(String email, Long inviteId, boolean fromMessage) {
        Invite invite = getInviteEntity(inviteId);
        if (!invite.isValid()) {
            throw new BadRequestException("message", "Invite expired.");
        }
        if (!fromMessage && !invite.getCanUseLink()) {
            throw new BadRequestException("message", "Invite cannot be accepted through link.");
        }
        Member createdMember = memberService.createMember(email, invite.getGroupChat(), MemberRole.MEMBER);
        chatService.broadcastCreatedChat(createdMember.getUser(), createdMember.getChat());
    }

}
