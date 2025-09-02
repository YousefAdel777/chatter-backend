package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class ReactRepositoryTest {

    @Autowired
    private ReactRepository reactRepository;

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
    private React react;

    @BeforeEach
    public void setup() {
        reactRepository.deleteAll();
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

        react = reactRepository.save(React.builder()
                .emoji("testEmoji")
                .user(user1)
                .message(message)
                .build());
    }

    @Test
    void shouldSaveReact_WhenValid() {
        React newReact = React.builder()
                .emoji("testEmoji")
                .user(user2)
                .message(message)
                .build();

        React savedReact = reactRepository.save(newReact);

        assertNotNull(savedReact.getId());
        assertEquals("testEmoji", savedReact.getEmoji());
        assertEquals(user2, savedReact.getUser());
        assertEquals(message, savedReact.getMessage());
        assertNotNull(savedReact.getCreatedAt());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnReact_WhenUserIsOwner() {
        Optional<React> result = reactRepository.findByIdAndUserEmail(react.getId(), user1.getEmail());

        assertTrue(result.isPresent());
        assertEquals(react.getId(), result.get().getId());
        assertEquals(user1.getEmail(), result.get().getUser().getEmail());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnEmpty_WhenUserNotOwner() {
        Optional<React> result = reactRepository.findByIdAndUserEmail(react.getId(), user2.getEmail());
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnEmpty_WhenReactNotFound() {
        Optional<React> result = reactRepository.findByIdAndUserEmail(999L, user1.getEmail());
        assertFalse(result.isPresent());
    }

    @Test
    void existsByMessageIdAndUserEmail_ShouldReturnTrue_WhenReactExists() {
        boolean exists = reactRepository.existsByMessageIdAndUserEmail(message.getId(), user1.getEmail());
        assertTrue(exists);
    }

    @Test
    void existsByMessageIdAndUserEmail_ShouldReturnFalse_WhenReactNotExists() {
        Boolean exists = reactRepository.existsByMessageIdAndUserEmail(message.getId(), user2.getEmail());

        assertFalse(exists);
    }

    @Test
    void existsByMessageIdAndUserEmail_ShouldReturnFalse_WhenMessageNotExists() {
        Boolean exists = reactRepository.existsByMessageIdAndUserEmail(999L, user1.getEmail());

        assertFalse(exists);
    }

    @Test
    void shouldPreventDuplicateReacts_ForSameUserAndMessage() {
        React duplicateReact = React.builder()
                .emoji("testEmoji")
                .user(user1)
                .message(message)
                .build();

        assertThrows(Exception.class, () -> {
            reactRepository.save(duplicateReact);
        });
    }

    @Test
    void shouldAllowReacts_ForSameUserDifferentMessages() {
        Message anotherMessage = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Another message")
                .messageType(MessageType.TEXT)
                .build());

        React newReact = React.builder()
                .emoji("😊")
                .user(user1)
                .message(anotherMessage)
                .build();

        React savedReact = reactRepository.save(newReact);

        assertNotNull(savedReact.getId());
        assertEquals(anotherMessage, savedReact.getMessage());
    }

    @Test
    void shouldAllowReacts_ForDifferentUsersSameMessage() {
        React newReact = React.builder()
                .emoji("testEmoji")
                .user(user2)
                .message(message)
                .build();

        React savedReact = reactRepository.save(newReact);

        assertNotNull(savedReact.getId());
        assertEquals(user2, savedReact.getUser());
        assertEquals(message, savedReact.getMessage());
    }

    @Test
    void shouldCascadeDeleteReacts_WhenMessageIsDeleted() {
        reactRepository.deleteAll();
        messageRepository.delete(message);

        Boolean exists = reactRepository.existsById(react.getId());
        assertFalse(exists);
    }

    @Test
    void shouldUpdateReactEmoji() {
        react.setEmoji("newEmoji");
        React updatedReact = reactRepository.save(react);

        assertEquals("newEmoji", updatedReact.getEmoji());
        assertEquals(react.getId(), updatedReact.getId());
    }

    @Test
    void shouldDeleteReact() {
        reactRepository.delete(react);

        assertFalse(reactRepository.existsById(react.getId()));
    }
}