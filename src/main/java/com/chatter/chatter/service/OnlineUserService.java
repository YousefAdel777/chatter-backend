package com.chatter.chatter.service;

import com.chatter.chatter.model.Option;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String ONLINE_USERS_KEY = "online_users";

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void userConnected(String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) return;
        User user = optUser.get();
        if (!user.getShowOnlineStatus()) return;

        Long addedCount = redisTemplate.opsForSet().add(ONLINE_USERS_KEY, user.getId().toString());
        if (addedCount != null && addedCount > 0) {
            user.setLastOnline(null);
            userRepository.save(user);
            broadcastUserOnlineStatus(user.getId(), true);
        }
    }

    public void userDisconnected(String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) return;
        User user = optUser.get();
        Long removed = redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, user.getId().toString());
        if (removed != null && removed > 0) {
            user.setLastOnline(Instant.now());
            userRepository.save(user);
            broadcastUserOnlineStatus(user.getId(), false);
        }
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString()));
    }

    private void broadcastUserOnlineStatus(Long userId, boolean isOnline) {
        simpMessagingTemplate.convertAndSend("/topic/users." + userId, isOnline);
    }
}
