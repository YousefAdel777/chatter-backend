package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class StarredMessageRepositoryTest {

    @Autowired
    private StarredMessageRepository starredMessageRepository;

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
    private Message message1;
    private Message message2;
    private StarredMessage starredMessage;

    @BeforeEach
    public void setup() {
        starredMessageRepository.deleteAll();
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

        message1 = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Test message 1")
                .messageType(MessageType.TEXT)
                .build());

        message2 = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Test message 2")
                .messageType(MessageType.TEXT)
                .build());

        starredMessage = starredMessageRepository.save(StarredMessage.builder()
                .user(user1)
                .message(message1)
                .build());
    }

    @Test
    void shouldSaveStarredMessage_WhenValid() {
        StarredMessage newStarredMessage = StarredMessage.builder()
                .user(user2)
                .message(message2)
                .build();

        StarredMessage saved = starredMessageRepository.save(newStarredMessage);

        assertNotNull(saved.getId());
        assertEquals(user2, saved.getUser());
        assertEquals(message2, saved.getMessage());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByUserEmail_ShouldReturnStarredMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmail(user1.getEmail(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(starredMessage.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findByUserEmail_ShouldReturnEmpty_WhenNoStarredMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmail(user2.getEmail(), pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserEmailAndMessageChatId_ShouldReturnStarredMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmailAndMessageChatId(user1.getEmail(), chat.getId(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(starredMessage.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findByUserEmailAndMessageChatId_ShouldReturnEmpty_WhenNoStarredMessagesInChat() {
        Chat newChat = chatRepository.save(Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build());

        Pageable pageable = PageRequest.of(0, 10);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmailAndMessageChatId(user1.getEmail(), newChat.getId(), pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserEmailAndMessageId_ShouldReturnStarredMessage() {
        Optional<StarredMessage> result = starredMessageRepository.findByUserEmailAndMessageId(user1.getEmail(), message1.getId());

        assertTrue(result.isPresent());
        assertEquals(starredMessage.getId(), result.get().getId());
    }

    @Test
    void findByUserEmailAndMessageId_ShouldReturnEmpty_WhenNotStarred() {
        Optional<StarredMessage> result = starredMessageRepository.findByUserEmailAndMessageId(user1.getEmail(), message2.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void existsByUserEmailAndMessageId_ShouldReturnTrue_WhenStarred() {
        boolean exists = starredMessageRepository.existsByUserEmailAndMessageId(user1.getEmail(), message1.getId());

        assertTrue(exists);
    }

    @Test
    void existsByUserEmailAndMessageId_ShouldReturnFalse_WhenNotStarred() {
        boolean exists = starredMessageRepository.existsByUserEmailAndMessageId(user1.getEmail(), message2.getId());

        assertFalse(exists);
    }

    @Test
    void shouldPreventDuplicateStarredMessages() {
        StarredMessage duplicate = StarredMessage.builder()
                .user(user1)
                .message(message1)
                .build();

        assertThrows(Exception.class, () -> {
            starredMessageRepository.save(duplicate);
        });
    }

    @Test
    void shouldAllowDifferentUsersStarSameMessage() {
        StarredMessage user2Star = StarredMessage.builder()
                .user(user2)
                .message(message1)
                .build();

        StarredMessage saved = starredMessageRepository.save(user2Star);

        assertNotNull(saved.getId());
        assertEquals(user2, saved.getUser());
        assertEquals(message1, saved.getMessage());
    }

    @Test
    void shouldAllowSameUserStarDifferentMessages() {
        StarredMessage user1StarMessage2 = StarredMessage.builder()
                .user(user1)
                .message(message2)
                .build();

        StarredMessage saved = starredMessageRepository.save(user1StarMessage2);

        assertNotNull(saved.getId());
        assertEquals(user1, saved.getUser());
        assertEquals(message2, saved.getMessage());
    }

    @Test
    void shouldCascadeDelete_WhenMessageIsDeleted() {
        starredMessageRepository.deleteAll();
        messageRepository.delete(message1);

        boolean exists = starredMessageRepository.existsById(starredMessage.getId());
        assertFalse(exists);
    }

    @Test
    void findByUserEmail_ShouldReturnPagedResults() {
        StarredMessage secondStar = StarredMessage.builder()
                .user(user1)
                .message(message2)
                .build();
        starredMessageRepository.save(secondStar);

        Pageable pageable = PageRequest.of(0, 1);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmail(user1.getEmail(), pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByUserEmailAndMessageChatId_ShouldReturnPagedResults() {
        StarredMessage secondStar = StarredMessage.builder()
                .user(user1)
                .message(message2)
                .build();
        starredMessageRepository.save(secondStar);

        Pageable pageable = PageRequest.of(0, 1);
        Page<StarredMessage> result = starredMessageRepository.findByUserEmailAndMessageChatId(user1.getEmail(), chat.getId(), pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }
}