package com.chatter.chatter.service;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.GroupChatPatchRequest;
import com.chatter.chatter.dto.GroupChatPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.ChatMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.specification.ChatSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${app.group.default-image}")
    private String defaultGroupImage;

    private final ChatRepository chatRepository;
    private final MemberService memberService;
    private final FileUploadService fileUploadService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMapper chatMapper;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "chats", key = "'usersIds:' + T(java.util.Objects).hash(#usersIds.toArray())")
    })
    public Chat createChat(Set<Long> usersIds) {
        Chat chat = new Chat();
        chat.setChatType(ChatType.INDIVIDUAL);
        for (Long id : usersIds) {
            Member member = memberService.createMember(id, chat, MemberRole.MEMBER);
            chat.addMember(member);
        }
        Chat createdChat = chatRepository.save(chat);
        for (Member member : createdChat.getMembers()) {
            broadcastCreatedChat(member.getUser(), createdChat);
        }
        evictChatCache(createdChat);
        return createdChat;
    }

    @Transactional
    public GroupChat createGroupChat(Principal principal, GroupChatPostRequest request) {
        GroupChat groupChat = GroupChat.builder()
                .description(request.getDescription())
                .name(request.getName())
                .chatType(ChatType.GROUP)
                .onlyAdminsCanSend(request.getOnlyAdminsCanSend())
                .onlyAdminsCanEditGroup(request.getOnlyAdminsCanEditGroup())
                .onlyAdminsCanInvite(request.getOnlyAdminsCanInvite())
                .onlyAdminsCanPin(request.getOnlyAdminsCanPin())
                .build();
        MultipartFile imageFile = request.getGroupImage();
        if (imageFile != null) {
            if (!fileUploadService.isImage(imageFile)) {
                throw new BadRequestException("groupImage", "Invalid image file");
            }
            try {
                String filePath = fileUploadService.uploadFile(imageFile);
                groupChat.setImage(filePath);
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to upload group image");
            }
        }
        GroupChat createdChat = chatRepository.save(groupChat);
        Member member = memberService.createMember(principal.getName(), createdChat, MemberRole.OWNER);
        evictChatCache(createdChat);
        broadcastCreatedChat(member.getUser(), createdChat);
        return createdChat;
    }

    @Transactional
    public Chat updateGroupChat(
            Principal principal,
            Long chatId,
            GroupChatPatchRequest groupChatPatchRequest
    ) {
        MultipartFile imageFile = groupChatPatchRequest.getGroupImage();
        String name = groupChatPatchRequest.getName();
        String description = groupChatPatchRequest.getDescription();
        Boolean onlyAdminsCanSend = groupChatPatchRequest.getOnlyAdminsCanSend();
        Boolean onlyAdminsCanPin = groupChatPatchRequest.getOnlyAdminsCanPin();
        Boolean onlyAdminsCanEditGroup = groupChatPatchRequest.getOnlyAdminsCanEditGroup();
        Boolean onlyAdminsCanInvite = groupChatPatchRequest.getOnlyAdminsCanInvite();
        GroupChat groupChat = (GroupChat) chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        Member member = memberService.getCurrentChatMemberEntity(principal.getName(), chatId);
        if (groupChat.getOnlyAdminsCanEditGroup() && !member.isAdmin()) {
            throw new ForbiddenException("Only admins can update group chats");
        }
        if (name != null) {
            groupChat.setName(name);
        }
        if (description != null) {
            groupChat.setDescription(description);
        }
        if (onlyAdminsCanSend != null) {
            groupChat.setOnlyAdminsCanSend(onlyAdminsCanSend);
        }
        if (onlyAdminsCanPin != null) {
            groupChat.setOnlyAdminsCanPin(onlyAdminsCanPin);
        }
        if (onlyAdminsCanEditGroup != null) {
            groupChat.setOnlyAdminsCanEditGroup(onlyAdminsCanEditGroup);
        }
        if (onlyAdminsCanInvite != null) {
            groupChat.setOnlyAdminsCanInvite(onlyAdminsCanInvite);
        }
        if (imageFile != null) {
            if (!fileUploadService.isImage(imageFile)) {
                throw new BadRequestException("groupImage", "Invalid image file");
            }
            try {
                String filePath = fileUploadService.uploadFile(imageFile);
                groupChat.setImage(filePath);
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to upload group image");
            }
        }
        else {
            groupChat.setImage(defaultGroupImage);
        }
        GroupChat updatedChat = chatRepository.save(groupChat);
        evictChatCache(updatedChat);
        broadcastChatUpdate(updatedChat);
        return groupChat;
    }

    @Transactional
    public void deleteGroupChat(Principal principal, Long chatId) {
        GroupChat groupChat = (GroupChat) chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        Member currentMember = memberService.getCurrentChatMemberEntity(principal.getName(), chatId);
        if (!currentMember.isOwner()) {
            throw new ForbiddenException("Only owner can delete group chat.");
        }
        evictChatCache(groupChat);
        for (Member member : groupChat.getMembers()) {
            broadcastChatDelete(member.getUser(), groupChat);
        }
        chatRepository.delete(groupChat);
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        evictChatCache(chat);
        chatRepository.delete(chat);
    }

    @Cacheable(
            value = "chats",
            key = "'email:' + #userEmail + ':searchEmail:' + (#email != null ? #email : 'null') + ':description:' + (#description != null ? #description : 'null')"
    )
    public List<ChatDto> getAllChatsByEmail(String userEmail, String email, String description) {
        return chatMapper.toDtoList(chatRepository.findAll(ChatSpecification.withFilters(userEmail, email, description)), userEmail);
    }

    @Cacheable(value = "chats", key = "'email:' + #email + ':chatId:' + #chatId")
    public ChatDto getChat(String email, Long chatId) {
        Chat chat = chatRepository.findChatById(email, chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        return chatMapper.toDto(chat, email);
    }

    public Chat getChatEntity(String email, Long chatId) {
        return chatRepository.findChatById(email, chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
    }

    public Chat getChatByUsers(Set<Long> usersIds) {
        return chatRepository.findByUsers(2L, ChatType.INDIVIDUAL, usersIds).orElse(null);
    }

    public Chat findOrCreateChat(Set<Long> usersIds) {
        Chat chat = getChatByUsers(usersIds);
        return chat != null ? chat : createChat(usersIds);
    }

    public List<Chat> getChats(String email, Set<Long> chatIds) {
        return chatRepository.findChatsByIds(email, chatIds);
    }

    public void broadcastChatUpdate(Chat chat) {
        for (Member member : chat.getMembers()) {
            simpMessagingTemplate.convertAndSend("/topic/users." + member.getUser().getId() + ".updated-chats", chatMapper.toDto(chat, member.getUser().getEmail()));
        }
    }

    public void broadcastChatDelete(User user, Chat chat) {
        simpMessagingTemplate.convertAndSend("/topic/users." + user.getId() + ".deleted-chats", chat.getId());
    }

    public void broadcastCreatedChat(User user, Chat chat) {
        simpMessagingTemplate.convertAndSend("/topic/users." + user.getId() + ".created-chats", chatMapper.toDto(chat, user.getEmail()));
    }


    public void evictChatCache(Chat chat) {
        Set<Member> members = chat.getMembers();
        for (Member member : members) {
            String pattern = "chats::email:" + member.getUser().getEmail() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    public void evictChatCacheForUser(String email) {
        String pattern = "chats::email:" + email + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}