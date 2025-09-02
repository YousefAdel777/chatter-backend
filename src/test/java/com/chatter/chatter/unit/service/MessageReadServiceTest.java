package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.MessageReadDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.MessageReadMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MessageReadRepository;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.MessageReadService;
import com.chatter.chatter.service.MessageService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageReadServiceTest {

    @Mock
    private MessageReadRepository messageReadRepository;

    @Mock
    private UserService userService;

    @Mock
    private MessageService messageService;

    @Mock
    private ChatService chatService;

    @Mock
    private MessageReadMapper messageReadMapper;

    @InjectMocks
    private MessageReadService messageReadService;

    private User user;
    private User messageUser;
    private User otherUser;
    private Message message;
    private Chat individualChat;
    private Chat groupChat;
    private MessageRead messageRead;

    @BeforeEach
    public void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .showMessageReads(true)
                .build();

        messageUser = User.builder()
                .id(2L)
                .email("sender@example.com")
                .username("sender")
                .showMessageReads(true)
                .build();

        otherUser = User.builder()
                .id(3L)
                .email("other@example.com")
                .username("other")
                .build();

        individualChat = Chat.builder()
                .id(1L)
                .chatType(ChatType.INDIVIDUAL)
                .build();

        individualChat.addMember(Member.builder().id(1L).user(user).build());
        individualChat.addMember(Member.builder().id(2L).user(otherUser).build());

        groupChat = Chat.builder()
                .id(2L)
                .chatType(ChatType.GROUP)
                .build();

        groupChat.addMember(Member.builder().id(3L).user(user).build());
        groupChat.addMember(Member.builder().id(4L).user(otherUser).build());
        groupChat.addMember(Member.builder().id(5L).user(messageUser).build());

        message = Message.builder()
                .id(1L)
                .content("Test message")
                .user(messageUser)
                .chat(individualChat)
                .build();

        messageRead = MessageRead.builder()
                .id(1L)
                .user(user)
                .message(message)
                .showRead(true)
                .build();
    }

    @Test
    void createMessageRead_ShouldCreateMessageRead_WhenValid() {
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.save(any(MessageRead.class))).thenReturn(messageRead);

        MessageRead result = messageReadService.createMessageRead("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(message, result.getMessage());
        verify(messageReadRepository).save(any(MessageRead.class));
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessagesCachesForUser("test@example.com");
        verify(chatService).broadcastChatUpdate(individualChat);
        verify(chatService).evictChatCacheForUser("test@example.com");
    }

    @Test
    void createMessageRead_ShouldThrowException_WhenAlreadyRead() {
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> messageReadService.createMessageRead("test@example.com", 1L));

        verify(messageReadRepository, never()).save(any(MessageRead.class));
    }

    @Test
    void createMessageRead_ShouldThrowException_WhenOwnMessage() {
        message.setUser(user);
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);

        assertThrows(BadRequestException.class, () -> messageReadService.createMessageRead("test@example.com", 1L));

        verify(messageReadRepository, never()).save(any(MessageRead.class));
    }

    @Test
    void createMessageRead_ShouldSetShowReadFalse_WhenUserDisabledReads() {
        user.setShowMessageReads(false);
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.save(any(MessageRead.class))).thenReturn(messageRead);

        MessageRead result = messageReadService.createMessageRead("test@example.com", 1L);

        assertNotNull(result);
        verify(messageReadRepository).save(argThat(read -> !read.getShowRead()));
    }

    @Test
    void createMessageRead_ShouldSetShowReadTrue_ForGroupChat() {
        message.setChat(groupChat);
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.save(any(MessageRead.class))).thenReturn(messageRead);

        MessageRead result = messageReadService.createMessageRead("test@example.com", 1L);

        assertNotNull(result);
        verify(messageReadRepository).save(argThat(MessageRead::getShowRead));
    }

    @Test
    void batchCreateMessageReads_ShouldCreateMultipleReads() {
        List<Long> messageIds = List.of(1L, 2L);
        Message message2 = Message.builder()
                .id(2L)
                .content("Another message")
                .user(messageUser)
                .chat(individualChat)
                .build();

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getUnreadMessagesByIds("test@example.com", messageIds)).thenReturn(List.of(message, message2));
        when(messageReadRepository.saveAll(anyList())).thenReturn(List.of(messageRead, messageRead));

        List<MessageRead> result = messageReadService.batchCreateMessageReads("test@example.com", messageIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(messageReadRepository).saveAll(anyList());
        verify(messageService).batchBroadcastMessageUpdate(individualChat, List.of(messageRead.getMessage(), messageRead.getMessage()));
        verify(messageService).evictMessagesCachesForUser("test@example.com");
        verify(chatService).broadcastChatUpdate(individualChat);
        verify(chatService).evictChatCacheForUser("test@example.com");
    }

    @Test
    void batchCreateMessageReads_ShouldReturnEmpty_WhenNoUnreadMessages() {
        List<Long> messageIds = List.of(1L, 2L);

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getUnreadMessagesByIds("test@example.com", messageIds))
                .thenReturn(Collections.emptyList());

        List<MessageRead> result = messageReadService.batchCreateMessageReads("test@example.com", messageIds);

        assertTrue(result.isEmpty());
        verify(messageReadRepository, never()).saveAll(anyList());
    }

    @Test
    void readChatMessages_ShouldReadAllUnreadMessages() {
        List<Message> unreadMessages = List.of(message);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getUnreadMessages("test@example.com", 1L)).thenReturn(unreadMessages);
        when(messageReadRepository.saveAll(anyList())).thenReturn(List.of(messageRead));

        List<MessageRead> result = messageReadService.readChatMessages("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(messageReadRepository).saveAll(anyList());
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessagesCachesForUser("test@example.com");
        verify(chatService).broadcastChatUpdate(individualChat);
        verify(chatService).evictChatCacheForUser("test@example.com");
    }

    @Test
    void readChatMessages_ShouldReturnEmpty_WhenNoUnreadMessages() {
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getUnreadMessages("test@example.com", 1L)).thenReturn(Collections.emptyList());

        List<MessageRead> result = messageReadService.readChatMessages("test@example.com", 1L);

        assertTrue(result.isEmpty());
        verify(messageReadRepository, never()).saveAll(anyList());
    }

    @Test
    void getMessageReads_ShouldReturnReads() {
        List<MessageRead> messageReads = List.of(messageRead);
        List<MessageReadDto> messageReadDtos = List.of(new MessageReadDto());

        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.findByMessageId(1L)).thenReturn(messageReads);
        when(messageReadMapper.toDtoList(messageReads)).thenReturn(messageReadDtos);

        List<MessageReadDto> result = messageReadService.getMessageReads("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(messageService).getMessageEntity("test@example.com", 1L);
        verify(messageReadRepository).findByMessageId(1L);
        verify(messageReadMapper).toDtoList(messageReads);
    }

    @Test
    void getMessageReads_ShouldThrowException_WhenMessageNotFound() {
        when(messageService.getMessageEntity("test@example.com", 1L))
                .thenThrow(new NotFoundException("message", "not found"));

        assertThrows(NotFoundException.class, () -> messageReadService.getMessageReads("test@example.com", 1L));

        verify(messageReadRepository, never()).findByMessageId(anyLong());
    }

    @Test
    void existsByEmailAndMessageId_ShouldReturnTrue() {
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(true);

        boolean result = messageReadService.existsByEmailAndMessageId("test@example.com", 1L);

        assertTrue(result);
    }

    @Test
    void existsByEmailAndMessageId_ShouldReturnFalse() {
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);

        boolean result = messageReadService.existsByEmailAndMessageId("test@example.com", 1L);

        assertFalse(result);
    }

    @Test
    void createMessageRead_ShouldEvictCachesForOtherUser_InIndividualChat() {
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.save(any(MessageRead.class))).thenReturn(messageRead);

        messageReadService.createMessageRead("test@example.com", 1L);

        verify(messageService).evictMessagesCachesForUser("test@example.com");
        verify(messageService).evictMessagesCachesForUser("other@example.com");
    }

    @Test
    void createMessageRead_ShouldNotEvictCachesForOtherUser_WhenOtherUserNull() {
        individualChat.setMembers(Set.of(Member.builder().id(1L).user(user).build()));
        when(messageReadRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(messageReadRepository.save(any(MessageRead.class))).thenReturn(messageRead);

        messageReadService.createMessageRead("test@example.com", 1L);

        verify(messageService, never()).evictMessagesCachesForUser(otherUser.getEmail());
    }
}