package com.chatter.chatter.service;

import com.chatter.chatter.creator.MessageCreator;
import com.chatter.chatter.dto.*;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.factory.MessageFactory;
import com.chatter.chatter.mapper.MessageMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MessageRepository;
import com.chatter.chatter.request.BatchMessageRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.specification.MessageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final BlockService blockService;
    private final AttachmentService attachmentService;
    private final MemberService memberService;
    private final ChatService chatService;
    private final MessageFactory messageFactory;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MessageMapper messageMapper;
    private final OptionService optionService;
    private final InviteService inviteService;
    //    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public Message createMessage(String email, SingleMessageRequest request) {
        User sender = userService.getUserEntityByEmail(email);
        Chat chat;
        Long chatId = request.getChatId();
        if (chatId == null && request.getUserId() == null) {
            throw new BadRequestException("message", "chatId and userId cannot be both null");
        }
        if (chatId != null) {
            chat = chatService.getChatEntityIfMember(email, chatId);
        } else {
            Set<Long> usersIds = new HashSet<>(List.of(sender.getId(), request.getUserId()));
            chat = chatService.findOrCreateChat(usersIds);
        }
        validateChat(email, chat);
        Message replyMessage = null;
        if (request.getReplyMessageId() != null) {
            replyMessage = messageRepository.findByIdAndChat(request.getReplyMessageId(), chat).orElseThrow(() -> new NotFoundException("replyMessageId", "Invalid reply message"));
        }
        MessageCreator messageCreator = messageFactory.getMessageCreator(request.getMessageType());
        Message message = messageCreator.createMessage(request, email);
        message.setReplyMessage(replyMessage);
        message.setChat(chat);
        message.setUser(sender);
        message.setContent(request.getContent());
        message.setMessageType(message.getMessageType());
        Message createdMessage = messageRepository.save(message);

        MessageDto messageDto = messageMapper.toDto(message, email, true);
        simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".created-messages", messageDto);
        chatService.broadcastChatUpdate(chat);
        chatService.evictChatCache(chat);

        evictMessageCaches(createdMessage);
        return createdMessage;
    }

    @Cacheable(
            value = "messages",
            key = "'email:' + #email + ':chat:' + (#chatId != null ? #chatId : 'null') + " +
                    "':content:' + (#content != null ? #content : 'null') + " +
                    "':type:' + (#messageType != null ? #messageType.name() : 'null') + " +
                    "':pinned:' + (#pinned != null ? #pinned : 'null') + " +
                    "':starred:' + (#starred != null ? #starred : 'null') + " +
                    "':before:' + (#before != null ? #before : 'null') + " +
                    "':after:' + (#after != null ? #after : 'null') + " +
                    "':size:' + #size"
    )
    public List<MessageDto> getAllMessages(
            String email,
            Long chatId,
            String content,
            MessageType messageType,
            Boolean pinned,
            Boolean starred,
            Long before,
            Long after,
            int size
    ) {
        Pageable pageable = PageRequest.of(0, size, after != null ? Sort.by("id").ascending() : Sort.by("id").descending());
        Specification<Message> specification = MessageSpecification.withFilters(chatId, content, messageType, pinned, starred, email, before, after);
        Page<Message> messages = messageRepository.findAll(specification, pageable);
        return messages.map(message -> messageMapper.toDto(message, email, true)).getContent();
    }

    public Message getMessageEntity(String email, Long id) {
        Message message = messageRepository.findById(id).orElseThrow(() -> new NotFoundException("message", "not found"));
        boolean isMember = memberService.isMember(email, message.getChat().getId());
        if (!isMember) {
            throw new ForbiddenException("You are not member of this chat");
        }
        return message;
//        return messageRepository.findMessageById(principal.getName(), id).orElseThrow(() -> new NotFoundException("message", "not found"));
    }

    @Cacheable(value = "messages", key = "'email:' + #email + ':messageId:' + #id")
    public MessageDto getMessage(String email, Long id) {
        return messageMapper.toDto(getMessageEntity(email, id), email, true);
    }

    @Transactional
    public Message updateMessage(String email, Long id, MessagePatchRequest messagePatchRequest) {
        Message message = messageRepository.findByIdAndUserEmail(id, email).orElseThrow(() -> new NotFoundException("message", "Invalid message"));
        Chat chat = message.getChat();
        validateChat(email, chat);
        if (messagePatchRequest.getContent() != null) {
            message.setContent(messagePatchRequest.getContent());
        }
        message.setEdited(true);
        messageRepository.save(message);
        simpMessagingTemplate.convertAndSend("/topic/chat." + message.getChat().getId() + ".edited-messages", messageMapper.toDto(message, email, true));
        chatService.broadcastChatUpdate(chat);
        chatService.evictChatCache(chat);
        evictMessageCaches(message);
        return message;
    }

    @Transactional
    public List<Message> forwardMessage(String email, Long id, Set<Long> chatIds) {
        List<Message> messages = new ArrayList<>();
        Message message = getMessageEntity(email, id);
        User user = userService.getUserEntityByEmail(email);

        List<Chat> chats = chatService.getChats(email, chatIds);
        for (Chat chat : chats) {
            validateChat(email, chat);
            Message createdMessage;
            if (message.getMessageType().equals(MessageType.TEXT)) {
                createdMessage = new TextMessage();
                createdMessage.setContent(message.getContent());
            } else if (message.getMessageType().equals(MessageType.FILE)) {
                FileMessage fileMessage = (FileMessage) message;
                createdMessage = new FileMessage();
                ((FileMessage) createdMessage).setFilePath(fileMessage.getFilePath());
                ((FileMessage) createdMessage).setOriginalFileName(fileMessage.getOriginalFileName());
            } else if (message.getMessageType().equals(MessageType.MEDIA)) {
                MediaMessage mediaMessage = (MediaMessage) message;
                createdMessage = new MediaMessage();
                List<Attachment> attachments = new ArrayList<>();
                for (Attachment attachment : mediaMessage.getAttachments()) {
                    Attachment createdAttachment = attachmentService.createAttachment(attachment, (MediaMessage) createdMessage);
                    attachments.add(createdAttachment);
                }
                ((MediaMessage) createdMessage).setAttachments(attachments);
            } else if (message.getMessageType().equals(MessageType.AUDIO)) {
                AudioMessage audioMessage = (AudioMessage) message;
                createdMessage = new AudioMessage();
                ((AudioMessage) createdMessage).setFileUrl(audioMessage.getFileUrl());
            } else if (message.getMessageType().equals(MessageType.POLL)) {
                PollMessage pollMessage = (PollMessage) message;
                createdMessage = new PollMessage();
                ((PollMessage) createdMessage).setOptions(optionService.createOptions(pollMessage.getOptions(), (PollMessage) createdMessage));
                ((PollMessage) createdMessage).setEndsAt(pollMessage.getEndsAt());
                ((PollMessage) createdMessage).setTitle(pollMessage.getTitle());
                ((PollMessage) createdMessage).setMultiple(pollMessage.getMultiple());
            } else {
                throw new BadRequestException("messageType", "Message with this type cannot be forwarded");
            }
            createdMessage.setChat(chat);
            createdMessage.setForwarded(true);
            createdMessage.setUser(user);
            createdMessage.setMessageType(message.getMessageType());
            simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".created-messages", messageMapper.toDto(createdMessage, email, true));
            messages.add(createdMessage);
            evictMessageCaches(createdMessage);
        }
        List<Message> createdMessages = messageRepository.saveAll(messages);
        for (Chat chat : chats) {
            chatService.broadcastChatUpdate(chat);
            chatService.evictChatCache(chat);
        }
        return createdMessages;
    }

    @Transactional
    public Message updateMessagePin(String email, Long messageId, boolean isPinned) {
        Message message = getMessageEntity(email, messageId);
        if (message.getChat().getChatType().equals(ChatType.GROUP)) {
            GroupChat groupChat = (GroupChat) message.getChat();
            if (groupChat.getOnlyAdminsCanPin()) {
                boolean isAdmin = memberService.isAdmin(email, message.getChat().getId());
                if (!isAdmin) {
                    throw new ForbiddenException("Only admins can update messages pin");
                }
            }
        }
        message.setPinned(isPinned);
        Message updatedMessage = messageRepository.save(message);
        broadcastMessageUpdate(updatedMessage);
        evictMessageCaches(updatedMessage);
        return updatedMessage;
    }

    @Transactional
    public List<Message> createMessages(String email, BatchMessageRequest request) {
        List<Message> messages = new ArrayList<>();
        User sender = userService.getUserEntityByEmail(email);
        List<Chat> chats = chatService.getChats(email, request.getChatsIds());
        for (Chat targetChat : chats) {
            validateChat(email, targetChat);
            Message message = messageFactory.getMessageCreator(request.getMessageType()).createMessage(request, email);
            message.setChat(targetChat);
            message.setUser(sender);
            message.setContent(request.getContent());
            message.setMessageType(message.getMessageType());
            messages.add(message);
            evictMessageCaches(message);
            simpMessagingTemplate.convertAndSend("/topic/chat." + targetChat.getId() + ".created-messages", messageMapper.toDto(message, email, true));
        }
        messageRepository.saveAll(messages);
        for (Chat targetChat : chats) {
            chatService.broadcastChatUpdate(targetChat);
            chatService.evictChatCache(targetChat);
        }
        return messages;
    }

    public Member acceptInvite(String email, Long messageId) {
        Message message = getMessageEntity(email, messageId);
        if (!message.getMessageType().equals(MessageType.INVITE)) {
            throw new BadRequestException("invite", "invalid invite");
        }
        InviteMessage inviteMessage = (InviteMessage) message;
        evictMessageCaches(message);
        return inviteService.acceptInvite(email, inviteMessage.getInvite().getId(), true);
    }

    @Transactional
    public void deleteMessage(String email, Long id) {
        Message message = messageRepository.findById(id).orElseThrow(() -> new NotFoundException("message", "not found"));
        if (!message.getUser().getEmail().equals(email)) {
            boolean isAdmin = memberService.isAdmin(email, message.getChat().getId());
            if (!isAdmin) {
                throw new ForbiddenException("Only admins can remove messages");
            }
        }
        Chat chat = message.getChat();
        simpMessagingTemplate.convertAndSend("/topic/chat." + message.getChat().getId() + ".deleted-messages", message.getId());
        chatService.evictChatCache(chat);

        evictMessageCaches(message);
        messageRepository.delete(message);
        messageRepository.flush();
        chatService.broadcastChatUpdate(chat);
    }

    private void validateChat(String email, Chat chat) {
        boolean isMember = memberService.isMember(email, chat.getId());
        if (!isMember) {
            throw new ForbiddenException("You are not a member of this chat");
        }
        if (chat.getChatType().equals(ChatType.GROUP)) {
            GroupChat groupChat = (GroupChat) chat;
            if (groupChat.getOnlyAdminsCanSend()) {
                boolean isAdmin = memberService.isAdmin(email, chat.getId());
                if (!isAdmin) {
                    throw new ForbiddenException("Only admins can send messages in this chat");
                }
            }
        } else {
            User otherUser = chat.getOtherUser(email);
            if (otherUser == null) {
                throw new BadRequestException("message", "Cannot send messages to this user");
            }
            boolean isBlocked = blockService.isBlocked(email, otherUser.getId());
            if (isBlocked) {
                throw new ForbiddenException("Cannot send messages to this user");
            }
        }
    }

    public void broadcastMessageUpdate(Message message) {
        for (Member member : message.getChat().getMembers()) {
            simpMessagingTemplate.convertAndSend("/topic/chat." + message.getChat().getId() + ".edited-messages?userId=" + member.getUser().getId(), messageMapper.toDto(message, member.getUser().getEmail(), true));
        }
    }

    public void batchBroadcastMessageUpdate(Chat chat, List<Message> messages) {
        if (messages.isEmpty()) return;
        for (Member member : chat.getMembers()) {
            simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".edited-messages-batch?userId=" + member.getUser().getId(), messageMapper.toDtoList(messages, member.getUser().getEmail()));
        }
    }

    public List<Message> getUnreadMessages(String email, Long chatId) {
        return messageRepository.findUnreadMessages(email, chatId);
    }

    public List<Message> getUnreadMessagesByIds(String email, Iterable<Long> messagesIds) {
        return messageRepository.findUnreadMessagesByIds(email, messagesIds);
    }

    public void evictMessageCaches(Message message) {
        Set<Member> members = message.getChat().getMembers();
        for (Member member : members) {
            String pattern = "messages::email:" + member.getUser().getEmail() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    public void evictMessagesCachesForUser(String email) {
        String pattern = "messages::email:" + email + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}