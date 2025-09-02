package com.chatter.chatter.integration.repository;

import com.chatter.chatter.dto.MessageStatusProjection;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MessageReadRepository messageReadRepository;

    @Autowired
    private StarredMessageRepository starredMessageRepository;

    @Autowired
    private ReactRepository reactRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private StoryRepository storyRepository;

    private User user1;
    private User user2;
    private Chat chat;
    private Message message1;
    private Message message2;
    private Message message3;

    @BeforeEach
    public void setup() {
        starredMessageRepository.deleteAll();
        messageReadRepository.deleteAll();
        reactRepository.deleteAll();
        attachmentRepository.deleteAll();
        optionRepository.deleteAll();
        inviteRepository.deleteAll();
        storyRepository.deleteAll();
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
                .chatType(ChatType.GROUP)
                .build());

        memberRepository.save(Member.builder()
                .user(user1)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        memberRepository.save(Member.builder()
                .user(user2)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        message1 = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Message 1")
                .messageType(MessageType.TEXT)
                .build());

        message2 = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user2)
                .content("Message 2")
                .messageType(MessageType.TEXT)
                .build());

        message3 = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user2)
                .content("Message 3")
                .messageType(MessageType.TEXT)
                .build());
    }

    @Test
    void findMessageStatus_ShouldReturnCorrectStatus() {
        // Mark message2 as read by user1
        messageReadRepository.save(MessageRead.builder()
                .message(message2)
                .user(user1)
                .build());

        // Mark message3 as starred by user1
        starredMessageRepository.save(StarredMessage.builder()
                .message(message3)
                .user(user1)
                .build());

        List<Long> messageIds = List.of(message1.getId(), message2.getId(), message3.getId());
        List<MessageStatusProjection> statusProjections = messageRepository.findMessageStatus(user1.getEmail(), messageIds);

        assertEquals(3, statusProjections.size());

        // Verify message1: not read, not starred
        MessageStatusProjection status1 = findStatusById(statusProjections, message1.getId());
        assertFalse(status1.getIsSeen());
        assertFalse(status1.getIsStarred());

        // Verify message2: read, not starred
        MessageStatusProjection status2 = findStatusById(statusProjections, message2.getId());
        assertTrue(status2.getIsSeen());
        assertFalse(status2.getIsStarred());

        // Verify message3: not read, starred
        MessageStatusProjection status3 = findStatusById(statusProjections, message3.getId());
        assertFalse(status3.getIsSeen());
        assertTrue(status3.getIsStarred());
    }

    @Test
    void findMessageStatus_ShouldReturnEmpty_WhenNoMessagesFound() {
        List<MessageStatusProjection> statusProjections = messageRepository.findMessageStatus(
                user1.getEmail(), List.of(999L, 1000L)
        );

        assertTrue(statusProjections.isEmpty());
    }

    @Test
    void findMessageStatus_ShouldHandleUserWithNoReadOrStarredMessages() {
        List<Long> messageIds = List.of(message1.getId(), message2.getId(), message3.getId());
        List<MessageStatusProjection> statusProjections = messageRepository.findMessageStatus(
                "nonexistent@example.com", messageIds
        );

        assertEquals(3, statusProjections.size());
        statusProjections.forEach(status -> {
            assertFalse(status.getIsSeen());
            assertFalse(status.getIsStarred());
        });
    }

    @Test
    void findMessageStatus_ShouldReturnCorrectStatus_ForMixedUsers() {
        // user1 reads message2
        messageReadRepository.save(MessageRead.builder()
                .message(message2)
                .user(user1)
                .build());

        // user2 stars message3
        starredMessageRepository.save(StarredMessage.builder()
                .message(message3)
                .user(user2)
                .build());

        // Test for user1
        List<MessageStatusProjection> user1Status = messageRepository.findMessageStatus(
                user1.getEmail(), List.of(message2.getId(), message3.getId())
        );

        MessageStatusProjection user1Status2 = findStatusById(user1Status, message2.getId());
        assertTrue(user1Status2.getIsSeen()); // user1 read it
        assertFalse(user1Status2.getIsStarred()); // user1 didn't star it

        MessageStatusProjection user1Status3 = findStatusById(user1Status, message3.getId());
        assertFalse(user1Status3.getIsSeen()); // user1 didn't read it
        assertFalse(user1Status3.getIsStarred()); // user1 didn't star it

        // Test for user2
        List<MessageStatusProjection> user2Status = messageRepository.findMessageStatus(
                user2.getEmail(), List.of(message2.getId(), message3.getId())
        );

        MessageStatusProjection user2Status2 = findStatusById(user2Status, message2.getId());
        assertFalse(user2Status2.getIsSeen()); // user2 didn't read it
        assertFalse(user2Status2.getIsStarred()); // user2 didn't star it

        MessageStatusProjection user2Status3 = findStatusById(user2Status, message3.getId());
        assertFalse(user2Status3.getIsSeen()); // user2 didn't read it
        assertTrue(user2Status3.getIsStarred()); // user2 starred it
    }

    @Test
    void findAllWithSpecification_ShouldReturnMessagesWithEntityGraph() {
        Specification<Message> spec = (root, query, cb) ->
                cb.equal(root.get("user").get("email"), user1.getEmail());

        List<Message> result = messageRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(user1.getEmail(), result.getFirst().getUser().getEmail());
        // EntityGraph should ensure user is fetched
        assertNotNull(result.getFirst().getUser().getUsername());
    }

    @Test
    void findUnreadMessages_ShouldReturnUnreadMessages() {
        List<Message> result = messageRepository.findUnreadMessages(user1.getEmail(), chat.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(message2.getId())));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(message3.getId())));
    }

    @Test
    void findUnreadMessages_ShouldReturnEmpty_WhenUserNotMember() {
        List<Message> result = messageRepository.findUnreadMessages("nonexistent@example.com", chat.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findUnreadMessages_ShouldReturnEmpty_WhenAllMessagesRead() {
        messageReadRepository.save(MessageRead.builder()
                .message(message2)
                .user(user1)
                .build());

        messageReadRepository.save(MessageRead.builder()
                .message(message3)
                .user(user1)
                .build());

        List<Message> result = messageRepository.findUnreadMessages(user1.getEmail(), chat.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findUnreadMessagesByIds_ShouldReturnUnreadMessages() {
        List<Long> messageIds = List.of(message2.getId(), message3.getId());
        List<Message> result = messageRepository.findUnreadMessagesByIds(user1.getEmail(), messageIds);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(message2.getId())));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(message3.getId())));
    }

    @Test
    void findUnreadMessagesByIds_ShouldReturnEmpty_WhenMessagesRead() {
        messageReadRepository.save(MessageRead.builder()
                .message(message2)
                .user(user1)
                .build());

        List<Long> messageIds = List.of(message2.getId(), message3.getId());
        List<Message> result = messageRepository.findUnreadMessagesByIds(user1.getEmail(), messageIds);

        assertEquals(1, result.size());
        assertEquals(message3.getId(), result.getFirst().getId());
    }

    @Test
    void findUnreadMessagesByIds_ShouldReturnEmpty_WhenUserNotMember() {
        List<Long> messageIds = List.of(message2.getId(), message3.getId());
        List<Message> result = messageRepository.findUnreadMessagesByIds("nonexistent@example.com", messageIds);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnMessage_WhenUserIsOwner() {
        Optional<Message> result = messageRepository.findByIdAndUserEmail(message1.getId(), user1.getEmail());

        assertTrue(result.isPresent());
        assertEquals(message1.getId(), result.get().getId());
        assertEquals(user1.getEmail(), result.get().getUser().getEmail());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnEmpty_WhenUserNotOwner() {
        Optional<Message> result = messageRepository.findByIdAndUserEmail(message1.getId(), user2.getEmail());

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserEmail_ShouldReturnEmpty_WhenMessageNotFound() {
        Optional<Message> result = messageRepository.findByIdAndUserEmail(999L, user1.getEmail());

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndChat_ShouldReturnMessage_WhenMessageInChat() {
        Optional<Message> result = messageRepository.findByIdAndChat(message1.getId(), chat);

        assertTrue(result.isPresent());
        assertEquals(message1.getId(), result.get().getId());
        assertEquals(chat.getId(), result.get().getChat().getId());
    }

    @Test
    void findByIdAndChat_ShouldReturnEmpty_WhenMessageNotInChat() {
        Chat otherChat = chatRepository.save(Chat.builder()
                .chatType(ChatType.GROUP)
                .build());

        Optional<Message> result = messageRepository.findByIdAndChat(message1.getId(), otherChat);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldSaveMessage_WhenValid() {
        Message newMessage = TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("New message")
                .messageType(MessageType.TEXT)
                .build();

        Message savedMessage = messageRepository.save(newMessage);

        assertNotNull(savedMessage.getId());
        assertNotNull(savedMessage.getCreatedAt());
        assertEquals("New message", savedMessage.getContent());
        assertEquals(user1, savedMessage.getUser());
        assertEquals(chat, savedMessage.getChat());
        assertEquals(MessageType.TEXT, savedMessage.getMessageType());
    }

    @Test
    void shouldSaveDifferentMessageTypes() {
        MediaMessage mediaMessage = messageRepository.save(MediaMessage.builder()
                .chat(chat)
                .user(user1)
                .attachments(List.of())
                .messageType(MessageType.MEDIA)
                .build());

        FileMessage fileMessage = messageRepository.save(FileMessage.builder()
                .chat(chat)
                .user(user1)
                .messageType(MessageType.FILE)
                .filePath("file.txt")
                .originalFileName("file.txt")
                .fileSize(1024L)
                .build());

        AudioMessage audioMessage = messageRepository.save(AudioMessage.builder()
                .chat(chat)
                .user(user1)
                .messageType(MessageType.AUDIO)
                .fileUrl("audio.mp3")
                .build());

        assertNotNull(mediaMessage.getId());
        assertNotNull(fileMessage.getId());
        assertNotNull(audioMessage.getId());
        assertEquals(MessageType.MEDIA, mediaMessage.getMessageType());
        assertEquals(MessageType.FILE, fileMessage.getMessageType());
        assertEquals(MessageType.AUDIO, audioMessage.getMessageType());
    }

    private MessageStatusProjection findStatusById(List<MessageStatusProjection> statusProjections, Long messageId) {
        return statusProjections.stream()
                .filter(status -> status.getId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Status not found for message ID: " + messageId));
    }
}