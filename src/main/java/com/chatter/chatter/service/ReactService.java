package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.React;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.ReactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactService {
    private final ReactRepository reactRepository;
    private final MessageService messageService;
    private final UserService userService;

    @Transactional
    public React createReact(String email, Long messageId, String emoji) {
        Message message = messageService.getMessageEntity(email, messageId);
        if (reactRepository.existsByMessageIdAndUserEmail(messageId, email)) {
            throw new BadRequestException("message", "You already have a react for this message.");
        }
        User user = userService.getUserEntityByEmail(email);
        React react = React.builder()
                        .emoji(emoji)
                        .message(message)
                        .user(user)
                        .build();
        React createdReact = reactRepository.save(react);
        messageService.broadcastMessageUpdate(createdReact.getMessage());
        messageService.evictMessageCaches(createdReact.getMessage());
        return createdReact;
    }

    @Transactional
    public void deleteReact(String email, Long reactId) {
        React react = reactRepository.findByIdAndUserEmail(reactId, email).orElseThrow(() -> new NotFoundException("react", "not found"));
        Message message = react.getMessage();
        reactRepository.delete(react);
        reactRepository.flush();
        messageService.broadcastMessageUpdate(message);
        messageService.evictMessageCaches(message);
    }

    @Transactional
    public React updateReact(Long reactId, String email, String emoji) {
        React react = reactRepository.findByIdAndUserEmail(reactId, email).orElseThrow(() -> new NotFoundException("react", "not found"));
        if (emoji != null && !emoji.isBlank()) {
            react.setEmoji(emoji);
        }
        React updatedReact = reactRepository.save(react);
        messageService.broadcastMessageUpdate(updatedReact.getMessage());
        messageService.evictMessageCaches(updatedReact.getMessage());
        return updatedReact;
    }
}