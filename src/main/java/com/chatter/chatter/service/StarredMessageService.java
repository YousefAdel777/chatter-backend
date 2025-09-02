package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
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
        return starredMessage;
    }

    @Transactional
    public void unstarMessage(String email, Long messageId) {
        StarredMessage starredMessage = starredMessageRepository.findByUserEmailAndMessageId(email, messageId).orElseThrow(() -> new NotFoundException("message", "starred message not found"));
        starredMessageRepository.delete(starredMessage);
        messageService.evictMessagesCachesForUser(email);
    }

}
