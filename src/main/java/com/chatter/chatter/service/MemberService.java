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

import java.security.Principal;
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

    @Cacheable(value = "members", key = "'chatId:' + #chatId + ':username:' + #username + ':email:' + #email")
    public List<MemberDto> getMembersByChat(Long chatId, String username, String email) {
        return memberMapper.toDtoList(getMembersEntitiesByChat(chatId, username, email));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "members:isMember", key = "'chatId:' + #chat.id + ':email:' + @userService.getUserEntity(#userId).email"),
            @CacheEvict(value = "members:isAdmin", key = "'chatId:' + #chat.id + ':email:' + @userService.getUserEntity(#userId).email"),
            @CacheEvict(value = "members", key = "'chatId:' + #chat.id + ':email:' + #userId"),
            @CacheEvict(value = "members:isMember", key = "'chatId:' + #chat.id + ':userId:' + #userId")
    })
    public Member createMember(Long userId, Chat chat, MemberRole role) {
        if (isMember(userId, chat.getId())) {
            throw new BadRequestException("message", "You are already a member of this chat.");
        }
        User user = userService.getUserEntity(userId);
        Member member = Member.builder().chat(chat).user(user).memberRole(role).build();
        if (ChatType.INDIVIDUAL.equals(chat.getChatType())) {
            userService.evictCurrentUserContactsCache(user.getEmail());
        }
        return memberRepository.save(member);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "members:isMember", key = "'chatId:' + #chat.id + ':email:' + #email"),
            @CacheEvict(value = "members:isAdmin", key = "'chatId:' + #chat.id + ':email:' + #email"),
            @CacheEvict(value = "members", key = "'chatId:' + #chat.id + ':email:' + #email"),
            @CacheEvict(value = "members:isMember", key = "'chatId:' + #chat.id + ':userId:' + @userService.getUserEntityByEmail(#email).id")
    })
    public Member createMember(String email, Chat chat, MemberRole role) {
        if (isMember(email, chat.getId())) {
            throw new BadRequestException("message", "You are already a member of this chat.");
        }
        User user = userService.getUserEntityByEmail(email);
        Member member = Member.builder().chat(chat).user(user).memberRole(role).build();
        if (ChatType.INDIVIDUAL.equals(chat.getChatType())) {
            userService.evictCurrentUserContactsCache(user.getEmail());
        }
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateMember(Principal principal, Long memberId, MemberPatchRequest request) {
        Member member = getMemberEntityById(memberId);
        Member admin = getCurrentChatMemberEntity(principal.getName(), member.getChat().getId());
        if (!admin.isAdmin()) throw new ForbiddenException("Only chat admins can update members.");
        if (request.getMemberRole() != null) {
            if (member.isOwner()) throw new ForbiddenException("Members with owner role cannot be changed");
            if (member.isAdmin() && !admin.isOwner()) throw new ForbiddenException("Only owner can change role of other admins");
            member.setMemberRole(request.getMemberRole());
        }
        evictMemberCaches(member);
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Principal principal, Long memberId) {
        Member member = getMemberEntityById(memberId);
        if (member.getChat().getChatType().equals(ChatType.INDIVIDUAL)) {
            throw new BadRequestException("message", "Members of individual chats cannot be deleted.");
        }
        if (!member.getUser().getEmail().equals(principal.getName())) {
            Member currentMember = getCurrentChatMemberEntity(principal.getName(), member.getChat().getId());
            if (!currentMember.isAdmin()) {
                throw new ForbiddenException("Only chat admins can remove members.");
            }
        }
        evictMemberCaches(member);
        simpMessagingTemplate.convertAndSend("/topic/users." + member.getUser().getId() + ".deleted-chats", member.getChat().getId());
        memberRepository.delete(member);
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

    @Cacheable(value = "members:isMember", key = "'chatId:' + #chatId + ':email:' + #email")
    public boolean isMember(String email, Long chatId) {
        return memberRepository.existsByChatIdAndUserEmail(chatId, email);
    }

    @Cacheable(value = "members:isMember", key = "'chatId:' + #chatId + ':userId:' + #userId")
    public boolean isMember(Long userId, Long chatId) {
        return memberRepository.existsByChatIdAndUserId(chatId, userId);
    }

    @Cacheable(value = "members:isAdmin", key = "'chatId:' + #chatId + ':email:' + #email")
    public boolean isAdmin(String email, Long chatId) {
        return memberRepository.existsByChatIdAndUserEmailAndMemberRole(chatId, email, MemberRole.ADMIN) ||
                memberRepository.existsByChatIdAndUserEmailAndMemberRole(chatId, email, MemberRole.OWNER);
    }

    @Transactional
    public void replaceOwner(Member owner) {
        if (owner.getChat().getChatType().equals(ChatType.INDIVIDUAL) || !owner.isOwner()) {
            return;
        }
        evictMemberCaches(owner);
        boolean found = false;
        List<Member> members = owner.getChat().getMembers().stream().filter(member -> !member.getId().equals(owner.getId())).toList();
        for (Member member : members) {
            if (!member.getId().equals(owner.getId()) && member.isAdmin()) {
                member.setMemberRole(MemberRole.OWNER);
                evictMemberCaches(member);
                memberRepository.save(member);
                found = true;
                break;
            }
        }
        if (!found && !members.isEmpty()) {
            Member firstMember = members.getFirst();
            evictMemberCaches(firstMember);
            firstMember.setMemberRole(MemberRole.OWNER);
            memberRepository.save(firstMember);
        }
    }

    private void evictMemberCaches(Member member) {
        String email = member.getUser().getEmail();
        Long chatId = member.getChat().getId();
        Long userId = member.getUser().getId();
        evictFromCache("members",
                "id:" + member.getId(),
                "chatId:" + chatId,
                "chatId:" + chatId + ":email:" + email
        );

        evictFromCache("members:isMember", "chatId:" + chatId + ":email:" + email);
        evictFromCache("members:isAdmin", "chatId:" + chatId + ":email:" + email);
        evictFromCache("members:isMember", "chatId:" + chatId + ":userId:" + userId);
    }

    private void evictFromCache(String cacheName, String... keys) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            for (String key : keys) {
                cache.evict(key);
            }
        }
    }

}