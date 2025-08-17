package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.StarredMessage;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.StarredMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class StarredMessageService {

    private final StarredMessageRepository starredMessageRepository;
    private final MessageService messageService;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(
            value = "starredMessages",
            key = "'email:' + #email + ':chatId:' + (#chatId == null ? 'null' : #chatId) + ':pageNumber:' + #pageable.pageNumber + ':pageSize:' + #pageable.pageSize + ':pageSort:' + (#pageable.sort != null ? #pageable.sort : 'unsorted')"
    )
    public Page<StarredMessage> getStarredMessages(String email, Long chatId, Pageable pageable) {
        if (chatId == null) {
            return starredMessageRepository.findByUserEmail(email, pageable);
        }
        return starredMessageRepository.findByUserEmailAndMessageChatId(email, chatId, pageable);
    }

    @Transactional
    public StarredMessage starMessage(String email, Long messageId) {
        Message message = messageService.getMessageEntity(email, messageId);
        boolean isStarred = starredMessageRepository.existsByUserEmailAndMessageId(email, messageId);
        if (isStarred) {
            throw new BadRequestException("message", "Message already starred");
        }
        User user = userService.getUserEntityByEmail(email);
        StarredMessage starredMessage = StarredMessage.builder()
                .message(message)
                .user(user)
                .build();
        starredMessageRepository.save(starredMessage);
        messageService.evictMessagesCachesForUser(email);
        evictStarredMessagesCache(email, message.getChat().getId());
        return starredMessage;
    }

    @Transactional
    public void unstarMessage(String email, Long messageId) {
        StarredMessage starredMessage = starredMessageRepository.findByUserEmailAndMessageId(email, messageId).orElseThrow(() -> new BadRequestException("message", "You have not starred this message"));
        starredMessageRepository.delete(starredMessage);
        messageService.evictMessagesCachesForUser(email);
        evictStarredMessagesCache(email, starredMessage.getMessage().getChat().getId());
    }

    private void evictStarredMessagesCache(String email, Long chatId) {
        Set<String> keys = redisTemplate.keys("starredMessages::email:" + email + ":chatId:null" + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        if (chatId != null) {
            keys = redisTemplate.keys("starredMessages::email:" + email + ":chatId:" + chatId + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

}
