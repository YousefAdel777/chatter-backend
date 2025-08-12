package com.chatter.chatter.listener;

import com.chatter.chatter.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class WebsocketEventListener {

    private final OnlineUserService onlineUserService;
    private final RedisTemplate<String, String> redisTemplate;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal != null && sessionId != null) {

            String key = "user_sessions:" + principal.getName();
            List<Object> results = redisTemplate.execute(new SessionCallback<>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForSet().add(key, sessionId);
                    operations.opsForSet().size(key);
                    operations.expire(key, 24, TimeUnit.HOURS);
                    return operations.exec();
                }
            });
            if (results.size() >= 2) {
                Long added = (Long) results.getFirst();
                Long sessionCount = (Long) results.get(1);
                if (added != null && added > 0 && sessionCount != null && sessionCount == 1) {
                    onlineUserService.userConnected(principal.getName());
                }
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal != null && sessionId != null) {
            String key = "user_sessions:" + principal.getName();
            redisTemplate.opsForSet().remove(key, sessionId);
            Long size = redisTemplate.opsForSet().size(key);
            if (size == null || size == 0) {
                redisTemplate.delete(key);
                onlineUserService.userDisconnected(principal.getName());
            }
        }
    }

}