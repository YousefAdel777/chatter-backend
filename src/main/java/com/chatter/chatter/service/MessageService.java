package com.chatter.chatter.service;

import com.chatter.chatter.creator.MessageCreator;
import com.chatter.chatter.dto.*;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.factory.MessageFactory;
import com.chatter.chatter.mapper.MessageMapper;
import com.chatter.chatter.mapper.MessageProjectionMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MessageRepository;
import com.chatter.chatter.request.BatchMessageRequest;
import com.chatter.chatter.request.MessagePatchRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.specification.MessageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.expression.spel.ast.Projection;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final MentionService mentionService;

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
        }
        else {
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
        message.setContentJson(request.getContentJson());
        if (request.getIsEveryoneMentioned() != null) message.setIsEveryoneMentioned(request.getIsEveryoneMentioned());
        if (request.getMentionedUsersIds() != null) {
            message.setMentions(mentionService.createMentions(message, request.getMentionedUsersIds()));
        }
        Message createdMessage = messageRepository.save(message);
        chat.addMessage(createdMessage);

        MessageDto messageDto = messageMapper.toDto(new MessageProjection(createdMessage, false, false), email, true);
        simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".created-messages", messageDto);
        chatService.broadcastChatUpdate(chat);
        broadcastLastMessageId(chat);

        chatService.evictChatCache(chat);
        evictMessageCaches(createdMessage);
        return createdMessage;
    }

    @Cacheable(
            value = "messages",
            key = "'email:' + #email + ':chatId:' + (#chatId != null ? #chatId : 'null') + " +
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
        Pageable pageable = PageRequest.of(0, size,
                after != null ? Sort.by("id").ascending() : Sort.by("id").descending());

        Specification<Message> specification = MessageSpecification.withFilters(
                chatId, content, messageType, pinned, starred, email, before, after
        );
        Page<Message> messages = messageRepository.findAll(specification, pageable);
        List<Message> messageList = messages.getContent();
        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> messageIds = messageList.stream()
                .map(Message::getId)
                .collect(Collectors.toSet());
        List<MessageStatusProjection> statusProjections = getMessagesProjections(Set.of(email), messageIds);
        Map<Long, MessageStatusProjection> statusMap = statusProjections.stream()
                .collect(Collectors.toMap(
                        MessageStatusProjection::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<MessageProjection> messageProjections = new ArrayList<>(messageList.size());

        for (Message message : messageList) {
            MessageStatusProjection status = statusMap.get(message.getId());
            messageProjections.add(new MessageProjection(
                    message,
                    status != null ? status.getIsStarred() : false,
                    status != null ? status.getIsSeen() : false
            ));
        }
        return messageMapper.toDtoListFromProjections(messageProjections, email);
    }

    public Message getMessageEntity(String email, Long id) {
        Message message = messageRepository.findById(id).orElseThrow(() -> new NotFoundException("message", "not found"));
        boolean isMember = memberService.isMember(email, message.getChat().getId());
        if (!isMember) {
            throw new ForbiddenException("You are not member of this chat");
        }
        return message;
    }

    @Cacheable(value = "messages", key = "'email:' + #email + ':messageId:' + #id")
    public MessageDto getMessage(String email, Long id) {
        Message message = getMessageEntity(email, id);
        MessageStatusProjection projection =  getMessagesProjections(Set.of(email), Set.of(id)).getFirst();
        return messageMapper.toDto(new MessageProjection(message, projection.getIsStarred(), projection.getIsSeen()), email, true);
    }

    @Transactional
    public Message updateMessage(String email, Long id, MessagePatchRequest messagePatchRequest) {
        Message message = messageRepository.findByIdAndUserEmail(id, email).orElseThrow(() -> new NotFoundException("message", "Invalid message"));
        Chat chat = message.getChat();
        validateChat(email, chat);
        if (messagePatchRequest.getContent() != null && messagePatchRequest.getContentJson() != null) {
            message.setContent(messagePatchRequest.getContent());
            message.setContentJson(messagePatchRequest.getContentJson());
        }
        message.setEdited(true);
        Message updatedMessage = messageRepository.save(message);
        broadcastMessageUpdate(updatedMessage);
        chatService.broadcastChatUpdate(chat);
        chatService.evictChatCache(chat);
        evictMessageCaches(message);
        return updatedMessage;
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
                createdMessage.setContentJson(message.getContentJson());
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
            simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".created-messages", messageMapper.toDto(new MessageProjection(createdMessage, false, false), email, true));
            messages.add(createdMessage);
            evictMessageCaches(createdMessage);
        }
        List<Message> createdMessages = messageRepository.saveAll(messages);
        for (Chat chat : chats) {
            chatService.broadcastChatUpdate(chat);
            broadcastLastMessageId(chat);
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
            message.setContentJson(request.getContentJson());
            message.setMessageType(message.getMessageType());
            messages.add(message);
            evictMessageCaches(message);
            simpMessagingTemplate.convertAndSend("/topic/chat." + targetChat.getId() + ".created-messages", messageMapper.toDto(new MessageProjection(message, false, false), email, true));
        }
        messageRepository.saveAll(messages);
        for (Chat targetChat : chats) {
            chatService.broadcastChatUpdate(targetChat);
            broadcastLastMessageId(targetChat);
            chatService.evictChatCache(targetChat);
        }
        return messages;
    }

    @Transactional
    public void acceptInvite(String email, Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new NotFoundException("message", "not found"));
        if (!message.getMessageType().equals(MessageType.INVITE)) {
            throw new BadRequestException("invite", "invalid invite");
        }
        InviteMessage inviteMessage = (InviteMessage) message;
        inviteService.acceptInvite(email, inviteMessage.getInvite().getId(), true);
        evictMessageCaches(message);
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
        broadcastLastMessageId(chat);
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
        }
        else {
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
        List<Member> members =  memberService.getMembersEntitiesByChat(message.getChat().getId(), null, null);
        Set<String> emails = members.stream().map(member -> member.getUser().getEmail()).collect(Collectors.toSet());
        List<MessageStatusProjection> projections = getMessagesProjections(emails, Set.of(message.getId()));
        for (MessageStatusProjection projection : projections) {
            simpMessagingTemplate.convertAndSend("/topic/chat." + message.getChat().getId() + ".edited-messages?userId=" + projection.getUserId(), messageMapper.toDto(new MessageProjection(message, projection.getIsStarred(), projection.getIsSeen()), projection.getEmail(), true));
        }
    }

    public void batchBroadcastMessageUpdate(Chat chat, List<Message> messages) {
        if (messages.isEmpty()) return;

        List<Member> members = memberService.getMembersEntitiesByChat(chat.getId(), null, null);
        Set<String> emails = members.stream()
                .map(member -> member.getUser().getEmail())
                .collect(Collectors.toSet());

        Set<Long> messageIds = messages.stream()
                .map(Message::getId)
                .collect(Collectors.toSet());

        List<MessageStatusProjection> messageStatusProjections = getMessagesProjections(emails, messageIds);

        Map<Long, Message> messageById = messages.stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));

        Map<Long, String> userEmailById = members.stream()
                .collect(Collectors.toMap(
                        member -> member.getUser().getId(),
                        member -> member.getUser().getEmail()
                ));

        Map<Long, List<MessageStatusProjection>> projectionsByUserId = messageStatusProjections.stream()
                .collect(Collectors.groupingBy(MessageStatusProjection::getUserId));

        for (Map.Entry<Long, List<MessageStatusProjection>> entry : projectionsByUserId.entrySet()) {
            Long userId = entry.getKey();
            List<MessageStatusProjection> userProjections = entry.getValue();
            String userEmail = userEmailById.get(userId);

            List<MessageProjection> messageProjections = userProjections.stream()
                    .map(projection -> new MessageProjection(
                            messageById.get(projection.getId()),
                            projection.getIsStarred(),
                            projection.getIsSeen()
                    ))
                    .collect(Collectors.toList());

            simpMessagingTemplate.convertAndSend(
                    "/topic/chat." + chat.getId() + ".edited-messages-batch?userId=" + userId,
                    messageMapper.toDtoListFromProjections(messageProjections, userEmail)
            );
        }
    }

    public List<Message> getUnreadMessages(String email, Long chatId) {
        return messageRepository.findUnreadMessages(email, chatId);
    }

    public List<Message> getUnreadMessagesByIds(String email, Iterable<Long> messagesIds) {
        return messageRepository.findUnreadMessagesByIds(email, messagesIds);
    }

    public void evictMessageCaches(Message message) {
        Long chatId = message.getChat().getId();
        evictCache("messages::*:chatId:" + chatId + "*");
        evictCache("messages::*:messageId:" + message.getId() + "*");
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

    public List<MessageStatusProjection> getMessagesProjections(Set<String> emails, Set<Long> messagesIds) {
        return messageRepository.findMessageStatus(emails, messagesIds);
    }

    private void broadcastLastMessageId(Chat chat) {
        Message lastMessage = chat.getLastMessage();
        if (lastMessage != null) {
            simpMessagingTemplate.convertAndSend("/topic/chat." + chat.getId() + ".last-message-id", lastMessage.getId());
        }
    }

    public void evictMessagesCachesForUser(String email) {
        String pattern = "messages::email:" + email + "*";
        evictCache(pattern);
    }
}