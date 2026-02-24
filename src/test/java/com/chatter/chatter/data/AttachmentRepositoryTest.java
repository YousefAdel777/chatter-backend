package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class AttachmentRepositoryTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MemberRepository memberRepository;

    private User user;
    private Chat chat;
    private MediaMessage message;
    private Attachment attachment;

    @BeforeEach
    public void setup() {
        attachmentRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("user@example.com")
                .username("user")
                .password("password")
                .build());

        chat = chatRepository.save(Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build());

        memberRepository.save(Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        message = messageRepository.save(MediaMessage.builder()
                .chat(chat)
                .user(user)
                .messageType(MessageType.MEDIA)
                .createdAt(Instant.now())
                .build());

        attachment = attachmentRepository.save(Attachment.builder()
                .filePath("file.jpg")
                .attachmentType(AttachmentType.IMAGE)
                .message(message)
                .build());
    }

    @Test
    void shouldSaveAttachment_WhenValid() {
        Attachment newAttachment = Attachment.builder()
                .filePath("another.jpg")
                .attachmentType(AttachmentType.IMAGE)
                .message(message)
                .build();

        Attachment saved = attachmentRepository.save(newAttachment);

        assertNotNull(saved.getId());
        assertEquals("another.jpg", saved.getFilePath());
        assertEquals(AttachmentType.IMAGE, saved.getAttachmentType());
        assertEquals(message, saved.getMessage());
    }

    @Test
    void shouldFindAttachmentById() {
        Optional<Attachment> found = attachmentRepository.findById(attachment.getId());

        assertTrue(found.isPresent());
        assertEquals(attachment.getId(), found.get().getId());
        assertEquals("file.jpg", found.get().getFilePath());
    }

    @Test
    void shouldReturnEmpty_WhenAttachmentNotFound() {
        Optional<Attachment> found = attachmentRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindAllAttachments() {
        Attachment secondAttachment = attachmentRepository.save(Attachment.builder()
                .filePath("file2.png")
                .attachmentType(AttachmentType.IMAGE)
                .message(message)
                .build());

        List<Attachment> attachments = attachmentRepository.findAll();

        assertEquals(2, attachments.size());
        assertTrue(attachments.stream().anyMatch(a -> a.getId().equals(attachment.getId())));
        assertTrue(attachments.stream().anyMatch(a -> a.getId().equals(secondAttachment.getId())));
    }

    @Test
    void shouldUpdateAttachment() {
        attachment.setFilePath("/updated/path.jpg");
        Attachment updated = attachmentRepository.save(attachment);

        assertEquals("/updated/path.jpg", updated.getFilePath());
        assertEquals(attachment.getId(), updated.getId());
    }

    @Test
    void shouldDeleteAttachment() {
        attachmentRepository.delete(attachment);

        Optional<Attachment> found = attachmentRepository.findById(attachment.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldCascadeDelete_WhenMessageIsDeleted() {
        attachmentRepository.deleteAll();
        messageRepository.delete(message);

        Optional<Attachment> found = attachmentRepository.findById(attachment.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldSaveAttachmentWithDifferentTypes() {
        attachmentRepository.save(Attachment.builder()
                .filePath("video.mp4")
                .attachmentType(AttachmentType.VIDEO)
                .message(message)
                .build());

        attachmentRepository.save(Attachment.builder()
                .filePath("test.jpg")
                .attachmentType(AttachmentType.IMAGE)
                .message(message)
                .build());

        List<Attachment> attachments = attachmentRepository.findAll();

        assertEquals(3, attachments.size());
        assertTrue(attachments.stream().anyMatch(a -> a.getAttachmentType() == AttachmentType.VIDEO));
        assertTrue(attachments.stream().anyMatch(a -> a.getAttachmentType() == AttachmentType.IMAGE));
    }

    @Test
    void shouldSaveMultipleAttachmentsForSameMessage() {
        attachmentRepository.save(Attachment.builder()
                .filePath("second.jpg")
                .attachmentType(AttachmentType.IMAGE)
                .message(message)
                .build());

        List<Attachment> attachments = attachmentRepository.findAll();

        assertEquals(2, attachments.size());
        assertEquals(message, attachments.get(0).getMessage());
        assertEquals(message, attachments.get(1).getMessage());
    }

    @Test
    void shouldSaveAttachmentsForDifferentMessages() {
        MediaMessage secondMessage = messageRepository.save(MediaMessage.builder()
                .chat(chat)
                .user(user)
                .messageType(MessageType.MEDIA)
                .createdAt(Instant.now())
                .build());

        attachmentRepository.save(Attachment.builder()
                .filePath("second.jpg")
                .attachmentType(AttachmentType.IMAGE)
                .message(secondMessage)
                .build());

        List<Attachment> attachments = attachmentRepository.findAll();

        assertEquals(2, attachments.size());
        assertTrue(attachments.stream().anyMatch(a -> a.getMessage().equals(message)));
        assertTrue(attachments.stream().anyMatch(a -> a.getMessage().equals(secondMessage)));
    }

    @Test
    void shouldDeleteAllAttachments() {
        attachmentRepository.deleteAll();

        List<Attachment> attachments = attachmentRepository.findAll();
        assertTrue(attachments.isEmpty());
    }
}