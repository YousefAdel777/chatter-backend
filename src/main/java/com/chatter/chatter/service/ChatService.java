package com.chatter.chatter.service;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.ChatStatusProjection;
import com.chatter.chatter.request.GroupChatPatchRequest;
import com.chatter.chatter.request.GroupChatPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.ChatMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.specification.ChatSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${app.group.default-image}")
    private String defaultGroupImage;

    @Value("${app.upload.max-image-size}")
    private Long maxImageSize;

    private final ChatRepository chatRepository;
    private final MemberService memberService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMapper chatMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileValidationService fileValidationService;
    private final FileUploadService fileUploadService;

    @Transactional
    public Chat createChat(Set<Long> usersIds) {
        Chat chat = new Chat();
        chat.setChatType(ChatType.INDIVIDUAL);
        for (Long id : usersIds) {
            chat.addMember(memberService.createMemberEntity(id, chat,  MemberRole.MEMBER));
        }
        Chat createdChat = chatRepository.save(chat);
        for (Member member : createdChat.getMembers()) {
            broadcastCreatedChat(member.getUser(), createdChat);
        }
        evictChatCache(createdChat);
        return createdChat;
    }

    @Transactional
    public GroupChat createGroupChat(String email, GroupChatPostRequest request) {
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
            if (!fileValidationService.isImage(imageFile)) {
                throw new BadRequestException("groupImage", "Invalid image file");
            }
            if (!fileValidationService.isSizeValid(imageFile, maxImageSize)) {
                throw new BadRequestException("groupImage", "File size exceeds maximum size");
            }
            String filePath = fileUploadService.uploadFile(imageFile);
            groupChat.setImage(filePath);
        }
        else {
            groupChat.setImage(defaultGroupImage);
        }
        GroupChat createdChat = chatRepository.save(groupChat);
        Member member = memberService.createMember(email, createdChat, MemberRole.OWNER);
        evictChatCache(createdChat);
        broadcastCreatedChat(member.getUser(), createdChat);
        return createdChat;
    }

    @Transactional
    public Chat updateGroupChat(
            String email,
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
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        if (!ChatType.GROUP.equals(chat.getChatType())) {
            throw new BadRequestException("chatType", "Invalid chat type");
        }
        GroupChat groupChat = (GroupChat) chat;
        Member member = memberService.getCurrentChatMemberEntity(email, chatId);
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
            if (!fileValidationService.isImage(imageFile)) {
                throw new BadRequestException("groupImage", "Invalid image file");
            }
            if (!fileValidationService.isSizeValid(imageFile, maxImageSize)) {
                throw new BadRequestException("groupImage", imageFile.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxImageSize / (1024 * 1024)) + " MB.");
            }
            String filePath = fileUploadService.uploadFile(imageFile);
            groupChat.setImage(filePath);
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
    public void deleteGroupChat(String email, Long chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
        if (!ChatType.GROUP.equals(chat.getChatType())) {
            throw new BadRequestException("message", "Only group chats can be deleted");
        }
        GroupChat groupChat = (GroupChat) chat;
        Member currentMember = memberService.getCurrentChatMemberEntity(email, chatId);
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
        List<Chat> chats = chatRepository.findAll(ChatSpecification.withFilters(userEmail, email, description));
        Set<Long> chatsIds = chats.stream().map(Chat::getId).collect(Collectors.toSet());
        List<ChatStatusProjection> chatStatusProjections = getChatStatusProjections(Set.of(userEmail), chatsIds);
        return chatMapper.toDtoList(chats, chatStatusProjections, userEmail);
    }

    @Cacheable(value = "chats", key = "'email:' + #email + ':chatId:' + #chatId")
    public ChatDto getChat(String email, Long chatId) {
        Chat chat = getChatEntityIfMember(email, chatId);
        List<ChatStatusProjection> chatStatusProjections = getChatStatusProjections(Set.of(email), Set.of(chatId));
        return chatMapper.toDto(chat, chatStatusProjections.getFirst(), email);
    }

    public Chat getChatEntity(Long chatId) {
        return chatRepository.findById(chatId).orElseThrow(() -> new NotFoundException("chat", "not found"));
    }

    public Chat getChatEntityIfMember(String email, Long chatId) {
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
        List<Member> members = memberService.getMembersEntitiesByChat(chat.getId(), null, null);
        Set<String> emails = members.stream().map(member -> member.getUser().getEmail()).collect(Collectors.toSet());
        List<ChatStatusProjection> projections = getChatStatusProjections(emails, Set.of(chat.getId()));
        for (ChatStatusProjection projection : projections) {
            simpMessagingTemplate.convertAndSend("/topic/users." + projection.getUserId() + ".updated-chats", chatMapper.toDto(chat, projection, projection.getUserEmail()));
        }
    }

    public void broadcastChatDelete(User user, Chat chat) {
        simpMessagingTemplate.convertAndSend("/topic/users." + user.getId() + ".deleted-chats", chat.getId());
    }

    public void broadcastCreatedChat(User user, Chat chat) {
        List<ChatStatusProjection> projections = getChatStatusProjections(Set.of(user.getEmail()), Set.of(chat.getId()));
        if (!projections.isEmpty()) {
            simpMessagingTemplate.convertAndSend("/topic/users." + user.getId() + ".created-chats", chatMapper.toDto(chat, projections.getFirst(), user.getEmail()));
        }
    }

    public List<ChatStatusProjection> getChatStatusProjections(Set<String> emails, Set<Long> chatsIds) {
        return chatRepository.findChatStatus(emails, chatsIds);
    }

    public void evictChatCache(Chat chat) {
        Set<Member> members = chat.getMembers();
        for (Member member : members) {
            String pattern = "chats::email:" + member.getUser().getEmail() + "*";
            evictCache(pattern);
        }
    }

    public void evictChatCacheForUser(String email) {
        String pattern = "chats::email:" + email + "*";
        evictCache(pattern);
    }

    private void evictCache(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                if (keys.size() >= 100) {
                    redisTemplate.delete(keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

}