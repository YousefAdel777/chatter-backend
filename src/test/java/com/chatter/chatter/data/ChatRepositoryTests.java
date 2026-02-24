package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class ChatRepositoryTests {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    private  User user1;
    private User user2;

    private Member member1;
    private Member member2;

    private Message message;

    @BeforeEach
    public void setup() {
        user1 = userRepository.save(User.builder()
                        .username("testUsername1")
                        .email("testId1@example.com")
                        .password("testPassword1")
                        .build());

        user2 = userRepository.save(User.builder()
                .username("testUsername2")
                .email("testId2@example.com")
                .password("testPassword2")
                .build());

        member1 = Member.builder().user(user1).build();
        member2 = Member.builder().user(user2).build();
        message = Message.builder()
                .user(user1)
                .content("testContent1")
                .messageType(MessageType.TEXT)
                .build();
    }

    @Test
    void shouldSaveIndividualChat() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .messages(List.of(message))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);
        message.setChat(chat);

        Chat savedChat = chatRepository.save(chat);
        assertNotNull(savedChat.getId());
        assertNotNull(savedChat.getCreatedAt());
        assertEquals(ChatType.INDIVIDUAL, savedChat.getChatType());
        assertEquals(Set.of(member1, member2), savedChat.getMembers());
        assertEquals(List.of(message), savedChat.getMessages());
    }

    @Test
    void shouldSaveGroupChat() {
        GroupChat chat = GroupChat.builder()
                .chatType(ChatType.GROUP)
                .members(Set.of(member1, member2))
                .messages(List.of(message))
                .name("testName")
                .description("testDescription")
                .image("testImage")
                .build();

        member1.setChat(chat);
        member2.setChat(chat);
        message.setChat(chat);

        GroupChat savedChat = chatRepository.save(chat);
        assertNotNull(savedChat.getId());
        assertNotNull(savedChat.getCreatedAt());
        assertEquals(ChatType.GROUP, savedChat.getChatType());
        assertEquals(Set.of(member1, member2), savedChat.getMembers());
        assertEquals(List.of(message), savedChat.getMessages());
        assertEquals(chat.getName(), savedChat.getName());
        assertEquals(chat.getDescription(), savedChat.getDescription());
        assertEquals(chat.getImage(), savedChat.getImage());
        assertTrue(savedChat.getOnlyAdminsCanPin());
        assertTrue(savedChat.getOnlyAdminsCanEditGroup());
        assertTrue(savedChat.getOnlyAdminsCanInvite());
        assertFalse(savedChat.getOnlyAdminsCanSend());
    }

    @Test
    void findByUsers_ShouldFindChatByUsersIdsAndType_WhenExists() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        chatRepository.save(chat);

        Chat result = chatRepository.findByUsers(2L, ChatType.INDIVIDUAL, List.of(member1.getUser().getId(), member2.getUser().getId())).orElse(null);
        assertNotNull(result);
    }

    @Test
    void findByUsers_ShouldNotFindChatByUsersIdsAndType_WhenTypeDoesNotMatch() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        chatRepository.save(chat);

        Chat result = chatRepository.findByUsers(2L, ChatType.GROUP, List.of(member1.getUser().getId(), member2.getUser().getId())).orElse(null);
        assertNull(result);
    }

    @Test
    void findByUsers_ShouldNotFindChatByUsersIdsAndType_WhenUsersIdsDoNotMatch() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        chatRepository.save(chat);

        Chat result = chatRepository.findByUsers(2L, ChatType.INDIVIDUAL, List.of(member1.getUser().getId(), 9999L)).orElse(null);
        assertNull(result);
    }

    @Test
    void findChatsByIds_ShouldFindChats_WhenUserIsMember() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        Chat createdChat = chatRepository.save(chat);

        List<Chat> results = chatRepository.findChatsByIds(member1.getUser().getId(), List.of(createdChat.getId()));
        assertEquals(results, List.of(createdChat));
    }

    @Test
    void findChatsByIds_ShouldNotFindChats_WhenUserIsNotMember() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        Chat createdChat = chatRepository.save(chat);

        List<Chat> results = chatRepository.findChatsByIds(999L, List.of(createdChat.getId()));
        assertTrue(results.isEmpty());
    }

    @Test
    void findChatById_ShouldFindChat_WhenUserIsMember() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        Chat createdChat = chatRepository.save(chat);

        Chat result = chatRepository.findChatById(member1.getUser().getId(), createdChat.getId()).orElse(null);
        assertNotNull(result);
    }

    @Test
    void findChatById_ShouldNotFindChat_WhenUserIsNotMember() {
        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .members(Set.of(member1, member2))
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        Chat createdChat = chatRepository.save(chat);

        Chat result = chatRepository.findChatById(999L, createdChat.getId()).orElse(null);
        assertNull(result);
    }

}
