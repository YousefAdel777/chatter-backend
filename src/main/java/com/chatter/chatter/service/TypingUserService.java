package com.chatter.chatter.service;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TypingUserService {

    private final String TYPING_USERS_KEY = "typing_users_chat:";
    private final MemberService memberService;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserMapper userMapper;

    public void addTypingUser(Long chatId, String email) {
        boolean isMember = memberService.isMember(email, chatId);
        if (!isMember) return;
        User user = userService.getUserEntityByEmail(email);
        String key = TYPING_USERS_KEY + chatId.toString();
        Long added = redisTemplate.opsForSet().add(key, user.getId().toString());
        if (added != null && added > 0) {
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
            broadcastNewTypingUser(chatId, userMapper.toDto(user));
        }
    }

    public void removeTypingUser(Long chatId, String email) {
        User user = userService.getUserEntityByEmail(email);
        String key = TYPING_USERS_KEY + chatId.toString();
        Long removed = redisTemplate.opsForSet().remove(key, user.getId().toString());
        if (removed != null && removed > 0) {
            broadcastRemoveTypingUser(chatId, user.getId());
        }
    }

    public List<User> getTypingUsers(Long chatId, String email) {
        if (email == null || !memberService.isMember(email, chatId)) {
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

    private void broadcastNewTypingUser(Long chatId, UserDto user) {
        simpMessagingTemplate.convertAndSend("/topic/chat." + chatId + ".added-typing-users", user);
    }

    private void broadcastRemoveTypingUser(Long chatId, Long userId) {
        simpMessagingTemplate.convertAndSend("/topic/chat." + chatId + ".removed-typing-users", userId);
    }

}
