package com.chatter.chatter.service;

import com.chatter.chatter.dto.MessageReadDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.mapper.MessageReadMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageReadService {

    private final MessageReadRepository messageReadRepository;
    private final UserService userService;
    private final MessageService messageService;
    private final ChatService chatService;
    private final MessageReadMapper messageReadMapper;

    @Transactional
    @CacheEvict(value = "messageReads", key = "'messageId:' + #messageId")
    public MessageRead createMessageRead(String email, Long messageId) {
        if (existsByEmailAndMessageId(email, messageId)) {
            throw new BadRequestException("messageRead", "User already read this message");
        }
        User user = userService.getUserEntityByEmail(email);
        Message message = messageService.getMessageEntity(email, messageId);
        if (email.equals(message.getUser().getEmail())) {
            throw new BadRequestException("messageRead", "User cannot mark their own messages as read");
        }
        MessageRead messageRead = MessageRead.builder()
                .user(user)
                .message(message)
                .showRead((user.getShowMessageReads() && message.getUser().getShowMessageReads()) || message.getChat().getChatType().equals(ChatType.GROUP))
                .build();
        MessageRead createdMessageRead = messageReadRepository.save(messageRead);
        messageService.broadcastMessageUpdate(createdMessageRead.getMessage());
        messageService.evictMessagesCachesForUser(email);
        if (message.getChat().getChatType().equals(ChatType.INDIVIDUAL)) {
            User otherUser = message.getChat().getOtherUser(email);
            if (otherUser != null) {
                messageService.evictMessagesCachesForUser(message.getChat().getOtherUser(email).getEmail());
            }
        }
        chatService.broadcastChatUpdate(createdMessageRead.getMessage().getChat());
        chatService.evictChatCacheForUser(email);
        return createdMessageRead;
    }

    @Transactional
    public List<MessageRead> batchCreateMessageReads(String email, Iterable<Long> messageIds) {
        User user = userService.getUserEntityByEmail(email);
        List<Message> messages = messageService.getUnreadMessagesByIds(email, messageIds);
        List<MessageRead> messageReads = new ArrayList<>();
        for (Message message : messages) {
            MessageRead messageRead = MessageRead.builder()
                    .user(user)
                    .message(message)
                    .showRead((user.getShowMessageReads() && (message.getUser() == null || message.getUser().getShowMessageReads())) || message.getChat().getChatType().equals(ChatType.GROUP))
                    .build();
            messageReads.add(messageRead);
        }
        List<MessageRead> createdMessageReads = new ArrayList<>();
        if (!messageReads.isEmpty()) {
            createdMessageReads = messageReadRepository.saveAll(messageReads);
        }
        Map<Chat, List<Message>> messagesByChat = createdMessageReads.stream()
            .collect(Collectors.groupingBy(
                    read -> read.getMessage().getChat(),
                    Collectors.mapping(MessageRead::getMessage, Collectors.toList())
            ));
        messagesByChat.forEach((chat, readMessages) -> {
            messageService.batchBroadcastMessageUpdate(chat, readMessages);
            messageService.evictMessagesCachesForUser(email);
            if (chat.getChatType().equals(ChatType.INDIVIDUAL)) {
                User otherUser = chat.getOtherUser(email);
                if (otherUser != null) {
                    messageService.evictMessagesCachesForUser(otherUser.getEmail());
                }
            }
            chatService.broadcastChatUpdate(chat);
            chatService.evictChatCacheForUser(email);
        });
        return createdMessageReads;
    }

    @Transactional
    public List<MessageRead> readChatMessages(String email, Long chatId) {
        List<MessageRead> messageReads = new ArrayList<>();
        User user = userService.getUserEntityByEmail(email);
        List<Message> unreadMessages = messageService.getUnreadMessages(email, chatId);
        if (unreadMessages.isEmpty()) {
            return Collections.emptyList();
        }
        for (Message message : unreadMessages) {
            MessageRead messageRead = MessageRead.builder()
                    .user(user)
                    .message(message)
                    .showRead(user.getShowMessageReads())
                    .build();
            messageReads.add(messageRead);
        }
        messageReadRepository.saveAll(messageReads);
        for (Message message : unreadMessages) {
            messageService.broadcastMessageUpdate(message);
            messageService.evictMessagesCachesForUser(email);
            if (message.getChat().getChatType().equals(ChatType.INDIVIDUAL)) {
                User otherUser = message.getChat().getOtherUser(email);
                if (otherUser != null) {
                    messageService.evictMessagesCachesForUser(otherUser.getEmail());
                }
            }
        }
        Chat chat = messageReads.getFirst().getMessage().getChat();
        chatService.broadcastChatUpdate(chat);
        chatService.evictChatCacheForUser(email);
        return messageReads;
    }

    @Cacheable(value = "messageReads", key = "'messageId:' + #messageId")
    public List<MessageReadDto> getMessageReads(String email, Long messageId) {
        messageService.getMessageEntity(email, messageId);
        return messageReadMapper.toDtoList(messageReadRepository.findByMessageId(messageId));
    }

    public boolean existsByEmailAndMessageId(String email, Long messageId) {
        return messageReadRepository.existsByUserEmailAndMessageId(email, messageId);
    }

}
