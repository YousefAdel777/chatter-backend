package com.chatter.chatter.service;

import com.chatter.chatter.dto.MemberDto;
import com.chatter.chatter.dto.MemberPatchRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.MemberMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.specification.MemberSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MemberMapper memberMapper;

    public List<Member> getMembersEntitiesByChat(Long chatId, String username, String email) {
        Specification<Member> specification = MemberSpecification.withFilters(chatId, username, email);
        return memberRepository.findAll(specification);
    }

    public List<MemberDto> getMembersByChat(Long chatId, String username, String email) {
        return memberMapper.toDtoList(getMembersEntitiesByChat(chatId, username, email));
    }

    public Member createMemberEntity(Long userId, Chat chat, MemberRole role) {
        if (isMember(userId, chat.getId())) {
            throw new BadRequestException("message", "You are already a member of this chat.");
        }
        User user = userService.getUserEntity(userId);
        Member member = Member.builder().chat(chat).user(user).memberRole(role).build();
        if (ChatType.INDIVIDUAL.equals(chat.getChatType())) {
            userService.evictCurrentUserContactsCache(user.getEmail());
        }
        return member;
    }

    public Member createMemberEntity(String email, Chat chat, MemberRole role) {
        if (isMember(email, chat.getId())) {
            throw new BadRequestException("message", "You are already a member of this chat.");
        }
        User user = userService.getUserEntityByEmail(email);
        return Member.builder().chat(chat).user(user).memberRole(role).build();
    }

    @Transactional
    public Member createMember(String email, Chat chat, MemberRole role) {
        Member createdMember = memberRepository.save(createMemberEntity(email, chat, role));
        if (ChatType.INDIVIDUAL.equals(chat.getChatType())) {
            userService.evictCurrentUserContactsCache(email);
        }
        return createdMember;
    }

    @Transactional
    public Member updateMember(String email, Long memberId, MemberPatchRequest request) {
        Member member = getMemberEntityById(memberId);
        Member admin = getCurrentChatMemberEntity(email, member.getChat().getId());
        if (!ChatType.GROUP.equals(member.getChat().getChatType())) {
            throw new BadRequestException("message", "Only members of group chats can be deleted.");
        }
        if (!admin.isAdmin()) throw new ForbiddenException("Only chat admins can update members.");
        if (request.getMemberRole() != null) {
            if (member.isOwner()) throw new ForbiddenException("Members with owner role cannot be changed");
            if (member.isAdmin() && !admin.isOwner()) throw new ForbiddenException("Only owner can change role of other admins");
            member.setMemberRole(request.getMemberRole());
        }
        Member updatedMember = memberRepository.save(member);
        evictMemberCaches(member);
        return updatedMember;
    }

    @Transactional
    public void deleteMember(String email, Long memberId) {
        Member member = getMemberEntityById(memberId);
        if (!ChatType.GROUP.equals(member.getChat().getChatType())) {
            throw new BadRequestException("message", "Only members of group chats can be deleted.");
        }
        if (!member.getUser().getEmail().equals(email)) {
            Member currentMember = getCurrentChatMemberEntity(email, member.getChat().getId());
            if (!currentMember.isAdmin()) {
                throw new ForbiddenException("Only chat admins can remove members.");
            }
        }
        simpMessagingTemplate.convertAndSend("/topic/users." + member.getUser().getId() + ".deleted-chats", member.getChat().getId());
        memberRepository.delete(member);
        evictMemberCaches(member);
    }

    public Member getMemberEntityById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new NotFoundException("member", "not found"));
    }

    @Cacheable(value = "members", key = "'id:' + #memberId")
    public MemberDto getMemberById(Long memberId) {
        return memberMapper.toDto(getMemberEntityById(memberId));
    }

    public Member getCurrentChatMemberEntity(String email, Long chatId) {
        return memberRepository.findByChatIdAndUserEmail(chatId, email)
                .orElseThrow(() -> new NotFoundException("member", "not found"));
    }

    @Cacheable(value = "members", key = "'chatId:' + #chatId + ':email:' + #email")
    public MemberDto getCurrentChatMember(String email, Long chatId) {
        return memberMapper.toDto(getCurrentChatMemberEntity(email, chatId));
    }

    public boolean isMember(String email, Long chatId) {
        return memberRepository.existsByChatIdAndUserEmail(chatId, email);
    }

    public boolean isMember(Long userId, Long chatId) {
        return memberRepository.existsByChatIdAndUserId(chatId, userId);
    }

    public boolean isAdmin(String email, Long chatId) {
        return memberRepository.existsByChatIdAndUserEmailAndMemberRole(chatId, email, MemberRole.OWNER) ||
                memberRepository.existsByChatIdAndUserEmailAndMemberRole(chatId, email, MemberRole.ADMIN);
    }

    @Transactional
    public void replaceOwner(Member owner) {
        if (!owner.isOwner() || ChatType.INDIVIDUAL.equals(owner.getChat().getChatType())) {
            return;
        }
        Member firstAdmin = memberRepository.findFirstMemberExcludingMember(owner.getChat().getId(), owner.getId(), MemberRole.ADMIN).orElse(null);
        if (firstAdmin != null) {
            firstAdmin.setMemberRole(MemberRole.OWNER);
            memberRepository.save(firstAdmin);
            evictMemberCaches(owner);
            evictMemberCaches(firstAdmin);
            return;
        }
        Member firstMember = memberRepository.findFirstMemberExcludingMember(owner.getChat().getId(), owner.getId(), MemberRole.MEMBER).orElse(null);
        if (firstMember != null) {
            firstMember.setMemberRole(MemberRole.OWNER);
            memberRepository.save(firstMember);
            evictMemberCaches(owner);
            evictMemberCaches(firstMember);
        }
    }

    private void evictMemberCaches(Member member) {
        String email = member.getUser().getEmail();
        Long chatId = member.getChat().getId();
        Cache cache = cacheManager.getCache("members");
        if (cache != null) {
            cache.evict("id:" + member.getId());
            cache.evict("chatId:" + chatId + ":email:" + email);
        }
    }

}