package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class MessageReadRepositoryTest {

    @Autowired
    private MessageReadRepository messageReadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MemberRepository memberRepository;

    private User user1;
    private User user2;
    private Chat chat;
    private Message message;
    private MessageRead messageRead;

    @BeforeEach
    public void setup() {
        messageReadRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .email("user1@example.com")
                .username("user1")
                .password("password")
                .build());

        user2 = userRepository.save(User.builder()
                .email("user2@example.com")
                .username("user2")
                .password("password")
                .build());

        chat = chatRepository.save(Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build());

        memberRepository.save(Member.builder()
                .user(user1)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        message = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Test message")
                .messageType(MessageType.TEXT)
                .build());

        messageRead = messageReadRepository.save(MessageRead.builder()
                .user(user1)
                .message(message)
                .build());
    }

    @Test
    void shouldSaveMessageRead_WhenValid() {
        MessageRead newMessageRead = MessageRead.builder()
                .user(user2)
                .message(message)
                .build();

        MessageRead savedMessageRead = messageReadRepository.save(newMessageRead);

        assertNotNull(savedMessageRead.getId());
        assertEquals(user2, savedMessageRead.getUser());
        assertEquals(message, savedMessageRead.getMessage());
        assertTrue(savedMessageRead.getShowRead());
        assertNotNull(savedMessageRead.getCreatedAt());
    }

    @Test
    void existsByUserEmailAndMessageId_ShouldReturnTrue_WhenMessageReadExists() {
        boolean exists = messageReadRepository.existsByUserEmailAndMessageId(user1.getEmail(), message.getId());

        assertTrue(exists);
    }

    @Test
    void existsByUserEmailAndMessageId_ShouldReturnFalse_WhenUserNotReadMessage() {
        boolean exists = messageReadRepository.existsByUserEmailAndMessageId(user2.getEmail(), message.getId());

        assertFalse(exists);
    }

    @Test
    void existsByUserEmailAndMessageId_ShouldReturnFalse_WhenMessageNotExists() {
        boolean exists = messageReadRepository.existsByUserEmailAndMessageId(user1.getEmail(), 999L);

        assertFalse(exists);
    }

    @Test
    void findByMessageId_ShouldReturnMessageReads_WhenMessageHasReads() {
        List<MessageRead> result = messageReadRepository.findByMessageId(message.getId());

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(messageRead.getId(), result.getFirst().getId());
    }

    @Test
    void findByMessageId_ShouldReturnEmptyList_WhenMessageHasNoReads() {
        Message newMessage = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("New message")
                .messageType(MessageType.TEXT)
                .build());

        List<MessageRead> result = messageReadRepository.findByMessageId(newMessage.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByMessageId_ShouldReturnEmptyList_WhenMessageNotExists() {
        List<MessageRead> result = messageReadRepository.findByMessageId(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPreventDuplicateMessageReads_ForSameUserAndMessage() {
        MessageRead duplicateMessageRead = MessageRead.builder()
                .user(user1)
                .message(message)
                .build();

        assertThrows(Exception.class, () -> messageReadRepository.save(duplicateMessageRead));
    }

    @Test
    void shouldAllowMessageReads_ForSameUserDifferentMessages() {
        Message newMessage = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Another message")
                .messageType(MessageType.TEXT)
                .build());

        MessageRead newMessageRead = MessageRead.builder()
                .user(user1)
                .message(newMessage)
                .build();

        MessageRead savedMessageRead = messageReadRepository.save(newMessageRead);

        assertNotNull(savedMessageRead.getId());
        assertEquals(newMessage, savedMessageRead.getMessage());
    }

    @Test
    void shouldAllowMessageReads_ForDifferentUsersSameMessage() {
        MessageRead newMessageRead = MessageRead.builder()
                .user(user2)
                .message(message)
                .build();

        MessageRead savedMessageRead = messageReadRepository.save(newMessageRead);

        assertNotNull(savedMessageRead.getId());
        assertEquals(user2, savedMessageRead.getUser());
        assertEquals(message, savedMessageRead.getMessage());
    }

    @Test
    void shouldCascadeDeleteMessageReads_WhenMessageIsDeleted() {
        messageReadRepository.deleteAll();
        messageRepository.delete(message);

        boolean exists = messageReadRepository.existsById(messageRead.getId());
        assertFalse(exists);
    }

    @Test
    void shouldUpdateMessageReadShowRead() {
        messageRead.setShowRead(false);
        MessageRead updatedMessageRead = messageReadRepository.save(messageRead);

        assertFalse(updatedMessageRead.getShowRead());
        assertEquals(messageRead.getId(), updatedMessageRead.getId());
    }

    @Test
    void shouldDeleteMessageRead() {
        messageReadRepository.delete(messageRead);

        assertFalse(messageReadRepository.existsById(messageRead.getId()));
    }

    @Test
    void shouldSetShowReadAsTrue_ByDefault() {
        MessageRead newMessageRead = MessageRead.builder()
                .user(user2)
                .message(message)
                .build();

        MessageRead savedMessageRead = messageReadRepository.save(newMessageRead);

        assertTrue(savedMessageRead.getShowRead());
    }

    @Test
    void shouldSetShowReadAsFalse_WhenExplicitlySet() {
        MessageRead newMessageRead = MessageRead.builder()
                .user(user2)
                .message(message)
                .showRead(false)
                .build();

        MessageRead savedMessageRead = messageReadRepository.save(newMessageRead);

        assertFalse(savedMessageRead.getShowRead());
    }
}