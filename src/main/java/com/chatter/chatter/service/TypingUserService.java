package com.chatter.chatter.service;

import com.chatter.chatter.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class TypingUserService {

    private final String TYPING_USERS_KEY = "typing_users_chat:";
    private final MemberService memberService;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TypingUserService(
            MemberService memberService,
            SimpMessagingTemplate simpMessagingTemplate,
            UserService userService,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.memberService = memberService;
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    public void addTypingUser(Long chatId, Principal principal) {
        boolean isMember = memberService.isMember(principal.getName(), chatId);
        if (!isMember) return;
        User user = userService.getUserEntityByEmail(principal.getName());
        String key = TYPING_USERS_KEY + chatId.toString();
        Long added = redisTemplate.opsForSet().add(key, user.getId().toString());
        if (added != null && added > 0) {
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
            broadcastNewTypingUser(chatId, user);
        }
    }

    public void removeTypingUser(Long chatId, Principal principal) {
        User user = userService.getUserEntityByEmail(principal.getName());
        String key = TYPING_USERS_KEY + chatId.toString();
        Long removed = redisTemplate.opsForSet().remove(key, user.getId().toString());
        if (removed != null && removed > 0) {
            broadcastRemoveTypingUser(chatId, user.getId());
        }
    }

    public List<User> getTypingUsers(Long chatId, Principal principal) {
        if (principal == null || !memberService.isMember(principal.getName(), chatId)) {
            return Collections.emptyList();
        }
        String key = TYPING_USERS_KEY + chatId;
        Set<String> usersIdsStrings = redisTemplate.opsForSet().members(key);
        if (usersIdsStrings == null || usersIdsStrings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = usersIdsStrings.stream().map(Long::parseLong).toList();
        return userService.getUsersEntities(userIds);
    }

    private void broadcastNewTypingUser(Long chatId, User user) {
        simpMessagingTemplate.convertAndSend("/topic/chat." + chatId + ".added-typing-users", user);
    }

    private void broadcastRemoveTypingUser(Long chatId, Long userId) {
        simpMessagingTemplate.convertAndSend("/topic/chat." + chatId + ".removed-typing-users", userId);
    }

}
