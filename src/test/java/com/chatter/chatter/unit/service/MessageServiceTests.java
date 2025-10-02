package com.chatter.chatter.unit.service;

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
import com.chatter.chatter.request.MessagePatchRequest;
import com.chatter.chatter.request.SingleMessageRequest;
import com.chatter.chatter.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTests {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private BlockService blockService;

    @Mock
    private MemberService memberService;

    @Mock
    private ChatService chatService;

    @Mock
    private MessageFactory messageFactory;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private InviteService inviteService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private MessageService messageService;

    private User user;
    private Chat individualChat;
    private GroupChat groupChat;
    private Message message;
    private MessageDto messageDto;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        User otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        individualChat = Chat.builder()
                .id(1L)
                .chatType(ChatType.INDIVIDUAL)
                .build();
        individualChat.addMember(Member
                .builder()
                .user(user)
                .chat(individualChat)
                .build());
        individualChat.addMember(Member
                .builder()
                .user(otherUser)
                .chat(individualChat)
                .build());

        groupChat = GroupChat.builder()
                .id(2L)
                .chatType(ChatType.GROUP)
                .onlyAdminsCanSend(false)
                .onlyAdminsCanPin(false)
                .build();

        groupChat.addMember(Member
                .builder()
                .user(user)
                .chat(groupChat)
                .build());

        message = TextMessage.builder()
                .id(1L)
                .user(user)
                .chat(individualChat)
                .content("Test message")
                .messageType(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        messageDto = MessageDto.builder()
                .id(1L)
                .content("Test message")
                .build();
    }

    @Test
    void createMessage_ShouldCreateMessage_WhenValidIndividualChat() {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setChatId(1L);
        request.setContent("Test message");
        request.setMessageType(MessageType.TEXT);

        MessageCreator textMessageCreator = mock(MessageCreator.class);
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(textMessageCreator.createMessage(any(), anyString())).thenReturn(message);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.getChatEntityIfMember("test@example.com", 1L)).thenReturn(individualChat);
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(messageFactory.getMessageCreator(MessageType.TEXT)).thenReturn(textMessageCreator);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toDto(any(MessageProjection.class), anyString(), anyBoolean())).thenReturn(messageDto);

        Message result = messageService.createMessage("test@example.com", request);

        assertNotNull(result);
        assertEquals("Test message", result.getContent());
        verify(messageRepository).save(message);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.created-messages", messageDto);
        verify(textMessageCreator).createMessage(any(), eq("test@example.com"));
    }

    @Test
    void createMessage_ShouldCreateMessage_WhenValidUserIdCreatesIndividualChat() {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setUserId(2L);
        request.setContent("Test message");
        request.setMessageType(MessageType.TEXT);

        MessageCreator textMessageCreator = mock(MessageCreator.class);
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(textMessageCreator.createMessage(any(), anyString())).thenReturn(message);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.findOrCreateChat(anySet())).thenReturn(individualChat);
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(blockService.isBlocked("test@example.com", 2L)).thenReturn(false);
        when(messageFactory.getMessageCreator(MessageType.TEXT)).thenReturn(textMessageCreator);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toDto(any(MessageProjection.class), anyString(), anyBoolean())).thenReturn(messageDto);

        Message result = messageService.createMessage("test@example.com", request);

        assertNotNull(result);
        verify(chatService).findOrCreateChat(Set.of(1L, 2L));
        verify(messageRepository).save(message);
        verify(textMessageCreator).createMessage(any(), eq("test@example.com"));
    }

    @Test
    void createMessage_ShouldThrowException_WhenChatIdAndUserIdBothNull() {
        SingleMessageRequest request = new SingleMessageRequest();
        assertThrows(BadRequestException.class, () -> messageService.createMessage("test@example.com", request));
    }

    @Test
    void createMessage_ShouldThrowException_WhenNotMemberOfChat() {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setChatId(1L);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.getChatEntityIfMember("test@example.com", 1L)).thenReturn(individualChat);
        when(memberService.isMember("test@example.com", 1L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.createMessage("test@example.com", request));
    }

    @Test
    void createMessage_ShouldThrowException_WhenAdminOnlyAndUserNotAdmin() {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setChatId(2L);

        GroupChat adminOnlyChat = GroupChat.builder()
                .id(2L)
                .chatType(ChatType.GROUP)
                .onlyAdminsCanSend(true)
                .build();

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.getChatEntityIfMember("test@example.com", 2L)).thenReturn(adminOnlyChat);
        when(memberService.isMember("test@example.com", 2L)).thenReturn(true);
        when(memberService.isAdmin("test@example.com", 2L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.createMessage("test@example.com", request));
    }

    @Test
    void createMessage_ShouldThrowException_WhenUserBlocked() {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setUserId(2L);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.findOrCreateChat(anySet())).thenReturn(individualChat);
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(blockService.isBlocked("test@example.com", 2L)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> messageService.createMessage("test@example.com", request));
    }

    @Test
    void getAllMessages_ShouldReturnMessages_WhenValidRequest() {
        Page<Message> messagePage = new PageImpl<>(List.of(message));
        MessageStatusProjection status = new MessageStatusProjection(message.getId(), user.getId(), "test@example.com", false, false);
        List<MessageDto> expectedDtos = List.of(messageDto);

        when(messageRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(messagePage);
        when(messageRepository.findMessageStatus(anySet(), anySet())).thenReturn(List.of(status));
        when(messageMapper.toDtoListFromProjections(anyList(), eq("test@example.com"))).thenReturn(expectedDtos);

        List<MessageDto> result = messageService.getAllMessages(
                "test@example.com", 1L, null, null, null, null, null, null, 10
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDtos, result);
        verify(messageRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(messageRepository).findMessageStatus(anySet(), anySet());
        verify(messageMapper).toDtoListFromProjections(anyList(), eq("test@example.com"));
    }

    @Test
    void getAllMessages_ShouldReturnEmpty_WhenNoMessagesFound() {
        Page<Message> emptyPage = new PageImpl<>(List.of());

        when(messageRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        List<MessageDto> result = messageService.getAllMessages(
                "test@example.com", 1L, null, null, null, null, null, null, 10
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void getMessageEntity_ShouldReturnMessage_WhenValid() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        Message result = messageService.getMessageEntity("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getMessageEntity_ShouldThrowException_WhenMessageNotFound() {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageService.getMessageEntity("test@example.com", 1L));
    }

    @Test
    void getMessageEntity_ShouldThrowException_WhenNotMember() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.getMessageEntity("test@example.com", 1L));
    }

    @Test
    void getMessage_ShouldReturnMessageDto() {
        MessageStatusProjection projection = new MessageStatusProjection(1L, 1L, "test@example.com", false, false);

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(messageRepository.findMessageStatus(Set.of("test@example.com"), Set.of(1L))).thenReturn(List.of(projection));
        when(messageMapper.toDto(any(MessageProjection.class), anyString(), anyBoolean())).thenReturn(messageDto);

        MessageDto result = messageService.getMessage("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void updateMessage_ShouldUpdateMessage_WhenUserIsOwner() {
        MessagePatchRequest patchRequest = new MessagePatchRequest();
        patchRequest.setContent("Updated content");

        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        Message result = messageService.updateMessage("test@example.com", 1L, patchRequest);

        assertNotNull(result);
        assertTrue(result.isEdited());
        verify(messageRepository).save(message);
    }

    @Test
    void updateMessage_ShouldThrowException_WhenMessageNotFound() {
        MessagePatchRequest patchRequest = new MessagePatchRequest();

        when(messageRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageService.updateMessage("test@example.com", 1L, patchRequest));
    }

    @Test
    void deleteMessage_ShouldDeleteMessage_WhenUserIsOwner() {
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        messageService.deleteMessage("test@example.com", 1L);

        verify(messageRepository).delete(message);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.deleted-messages", 1L);
    }

    @Test
    void deleteMessage_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        User otherUser = User.builder().email("other@example.com").build();
        message.setUser(otherUser);

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isAdmin("test@example.com", 1L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.deleteMessage("test@example.com", 1L));
    }

    @Test
    void deleteMessage_ShouldDeleteMessage_WhenAdminButNotOwner() {
        User otherUser = User.builder().email("other@example.com").build();
        message.setUser(otherUser);

        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isAdmin("test@example.com", 1L)).thenReturn(true);

        messageService.deleteMessage("test@example.com", 1L);

        verify(messageRepository).delete(message);
    }

    @Test
    void updateMessagePin_ShouldUpdatePin_WhenUserHasPermission() {
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        Message result = messageService.updateMessagePin("test@example.com", 1L, true);

        assertNotNull(result);
        assertTrue(result.getPinned());
        verify(messageRepository).save(message);
    }

    @Test
    void updateMessagePin_ShouldThrowException_WhenAdminOnlyAndUserNotAdmin() {
        GroupChat adminOnlyChat = GroupChat.builder()
                .id(2L)
                .chatType(ChatType.GROUP)
                .onlyAdminsCanPin(true)
                .build();

        message.setChat(adminOnlyChat);

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 2L)).thenReturn(true);
        when(memberService.isAdmin("test@example.com", 2L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> messageService.updateMessagePin("test@example.com", 1L, true));
    }

    @Test
    void getUnreadMessages_ShouldReturnUnreadMessages() {
        when(messageRepository.findUnreadMessages("test@example.com", 1L))
                .thenReturn(List.of(message));

        List<Message> result = messageService.getUnreadMessages("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getUnreadMessagesByIds_ShouldReturnUnreadMessages() {
        when(messageRepository.findUnreadMessagesByIds("test@example.com", List.of(1L)))
                .thenReturn(List.of(message));

        List<Message> result = messageService.getUnreadMessagesByIds("test@example.com", List.of(1L));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void acceptInvite_ShouldAcceptInvite_WhenValidInviteMessage() {
        Invite invite = Invite.builder().id(1L).build();
        InviteMessage inviteMessage = InviteMessage.builder()
                .id(1L)
                .invite(invite)
                .messageType(MessageType.INVITE)
                .chat(individualChat)
                .build();

        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(inviteMessage));

        messageService.acceptInvite("test@example.com", 1L);
        verify(inviteService).acceptInvite("test@example.com", 1L, true);
    }

    @Test
    void acceptInvite_ShouldThrowException_WhenNotInviteMessage() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        assertThrows(BadRequestException.class, () -> messageService.acceptInvite("test@example.com", 1L));
    }

    @Test
    void evictMessageCaches_ShouldEvictCache() {
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);

        messageService.evictMessageCaches(message);

        verify(redisTemplate, atLeastOnce()).scan(any(ScanOptions.class));
    }

    @Test
    void evictMessagesCachesForUser_ShouldEvictUserCache() {
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);

        messageService.evictMessagesCachesForUser("test@example.com");

        verify(redisTemplate, atLeastOnce()).scan(any(ScanOptions.class));
        verify(cursorMock).close();
    }

    @Test
    void createMessages_ShouldCreateBatchMessages() {
        BatchMessageRequest request = new BatchMessageRequest();
        request.setChatsIds(Set.of(1L, 2L));
        request.setContent("Batch message");
        request.setMessageType(MessageType.TEXT);

        MessageCreator textMessageCreator = mock(MessageCreator.class);
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(textMessageCreator.createMessage(any(), anyString())).thenReturn(message);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.getChats("test@example.com", Set.of(1L, 2L))).thenReturn(List.of(individualChat, groupChat));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(memberService.isMember("test@example.com", 2L)).thenReturn(true);
        when(messageFactory.getMessageCreator(MessageType.TEXT)).thenReturn(textMessageCreator);
        when(messageRepository.saveAll(anyList())).thenReturn(List.of(message, message));
        when(messageMapper.toDto(any(MessageProjection.class), anyString(), anyBoolean())).thenReturn(messageDto);

        List<Message> result = messageService.createMessages("test@example.com", request);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(messageRepository).saveAll(anyList());
        verify(simpMessagingTemplate, times(2)).convertAndSend(anyString(), any(MessageDto.class));
        verify(textMessageCreator, times(2)).createMessage(any(), eq("test@example.com"));
    }

    @Test
    void forwardMessage_ShouldForwardMessage_ToMultipleChats() {
        Set<Long> chatIds = Set.of(1L, 2L);
        Cursor<String> cursorMock = mock(Cursor.class);
        when(cursorMock.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursorMock);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(chatService.getChats("test@example.com", chatIds)).thenReturn(List.of(individualChat, groupChat));
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);
        when(memberService.isMember("test@example.com", 2L)).thenReturn(true);
        when(messageRepository.saveAll(anyList())).thenReturn(List.of(message, message));
        when(messageMapper.toDto(any(MessageProjection.class), anyString(), anyBoolean())).thenReturn(messageDto);

        List<Message> result = messageService.forwardMessage("test@example.com", 1L, chatIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(messageRepository).saveAll(anyList());
    }

    @Test
    void getMessagesProjections_ShouldReturnProjections() {
        Set<String> emails = Set.of("test@example.com");
        Set<Long> messageIds = Set.of(1L, 2L);
        List<MessageStatusProjection> projections = Arrays.asList(
                new MessageStatusProjection(1L, 1L, "test@example.com", false, false),
                new MessageStatusProjection(2L, 1L, "test@example.com", true, true)
        );

        when(messageRepository.findMessageStatus(emails, messageIds)).thenReturn(projections);

        List<MessageStatusProjection> result = messageService.getMessagesProjections(emails, messageIds);

        assertEquals(projections, result);
        verify(messageRepository).findMessageStatus(emails, messageIds);
    }

    @Test
    void batchBroadcastMessageUpdate_ShouldSendBatchUpdates() {
        Chat chat = individualChat;
        List<Message> messages = Arrays.asList(message);
        List<Member> members = Arrays.asList(
                Member.builder().user(user).chat(chat).build()
        );
        List<MessageStatusProjection> statusProjections = List.of(
                new MessageStatusProjection(1L, 1L, "test@example.com", true, true)
        );

        when(memberService.getMembersEntitiesByChat(chat.getId(), null, null)).thenReturn(members);
        when(messageRepository.findMessageStatus(Set.of("test@example.com"), Set.of(1L))).thenReturn(statusProjections);
        when(messageMapper.toDtoListFromProjections(anyList(), eq("test@example.com"))).thenReturn(List.of(messageDto));

        messageService.batchBroadcastMessageUpdate(chat, messages);
        verify(simpMessagingTemplate).convertAndSend(contains("/topic/chat.1.edited-messages-batch"), anyList());
    }

    @Test
    void batchBroadcastMessageUpdate_ShouldNotSend_WhenEmptyMessages() {
        messageService.batchBroadcastMessageUpdate(individualChat, List.of());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyList());
    }
}