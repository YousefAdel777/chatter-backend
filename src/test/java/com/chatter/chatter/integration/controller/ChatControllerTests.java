package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.GroupChatPatchRequest;
import com.chatter.chatter.request.GroupChatPostRequest;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({WebsocketTestConfiguration.class})
public class ChatControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatRepository chatRepository;

    private Chat chat;
    private GroupChat groupChat;
    private Member member1;
    private Member member2;
    private User user1;
    private User user2;
    private Message message;
    private String accessToken;

    @BeforeEach
    public void setup() {
        chatRepository.deleteAll();
        memberRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("testUsername1")
                .email("testEmail1@example.com")
                .password("testPassword1")
                .build());

        user2 = userRepository.save(User.builder()
                .username("testUsername2")
                .email("testEmail2@example.com")
                .password("testPassword2")
                .build());

        Chat c = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        member1 = Member.builder()
                .chat(c)
                .user(user1)
                .memberRole(MemberRole.MEMBER)
                .build();

        member2 = Member.builder()
                .chat(c)
                .user(user2)
                .memberRole(MemberRole.MEMBER)
                .build();

        c.addMember(member1);
        c.addMember(member2);

        message = Message.builder()
                .messageType(MessageType.TEXT)
                .content("Test message")
                .user(user2)
                .chat(c)
                .build();

        c.addMessage(message);
        chat = chatRepository.save(c);

        groupChat = GroupChat.builder()
                .image("groupImage")
                .name("Test Group")
                .description("Test Description")
                .chatType(ChatType.GROUP)
                .onlyAdminsCanSend(false)
                .onlyAdminsCanEditGroup(false)
                .onlyAdminsCanInvite(false)
                .onlyAdminsCanPin(false)
                .build();

        Member groupMember = Member.builder()
                .chat(groupChat)
                .user(user1)
                .memberRole(MemberRole.OWNER)
                .build();

        groupChat.addMember(groupMember);
        groupChat = chatRepository.save(groupChat);

        accessToken = jwtService.generateToken(user1.getEmail()).getAccessToken();
    }

    @Test
    void shouldGetAllChats_WhenUserIsMember() throws Exception {
        mockMvc.perform(get("/api/chats")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].id").value(chat.getId()))
                .andExpect(jsonPath("$.[0].otherUser.id").value(user2.getId()))
                .andExpect(jsonPath("$.[0].lastMessage.id").value(message.getId()))
                .andExpect(jsonPath("$.[0].firstUnreadMessageId").value(message.getId()))
                .andExpect(jsonPath("$.[1].id").value(groupChat.getId()))
                .andExpect(jsonPath("$.[1].otherUser").doesNotExist())
                .andExpect(jsonPath("$.[1].membersCount").value(1));
    }

    @Test
    void shouldGetAllChats_WhenUserIsNotMember() throws Exception {
        User user3 = userRepository.save(User.builder()
                .username("testUsername3")
                .email("testEmail3@example.com")
                .password("testPassword3")
                .build());

        String user3Token = jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(get("/api/chats")
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetAllChats_WithNameAndDescriptionFilter() throws Exception {
        mockMvc.perform(get("/api/chats")
                        .param("name", "Group")
                        .param("description", "Test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Group"));
    }

    @Test
    void shouldGetChat_WhenUserIsMember() throws Exception {
        mockMvc.perform(get("/api/chats/{chatId}", chat.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chat.getId()))
                .andExpect(jsonPath("$.chatType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.otherUser.id").value(user2.getId()))
                .andExpect(jsonPath("$.lastMessage.id").value(message.getId()))
                .andExpect(jsonPath("$.firstUnreadMessageId").value(message.getId()))
                .andExpect(jsonPath("$.unreadMessagesCount").value(1))
                .andExpect(jsonPath("$.membersCount").value(2));
    }

    @Test
    void shouldGetChat_WhenUserIsNotMember() throws Exception {
        User user3 = userRepository.save(User.builder()
                .username("testUsername3")
                .email("testEmail3@example.com")
                .password("testPassword3")
                .build());

        String user3Token = jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(get("/api/chats/{chatId}", chat.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotGetChat_WhenChatDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/chats/{chatId}", 999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateGroupChat_WithValidData() throws Exception {
        GroupChatPostRequest request = new GroupChatPostRequest(
                "New Test Group",
                "New Test Description",
                null,
                false,
                false,
                false,
                false
        );

        MockMultipartFile group = new MockMultipartFile(
                "group",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(request).getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chats")
                        .file(group)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Test Group"));
    }

    @Test
    void shouldUpdateGroupChat_WhenUserIsOwner() throws Exception {

        GroupChatPatchRequest groupChatPatchRequest = GroupChatPatchRequest.builder()
                .name("Updated Group Name")
                .description("Updated Description")
                .onlyAdminsCanSend(true)
                .onlyAdminsCanEditGroup(true)
                .onlyAdminsCanInvite(true)
                .onlyAdminsCanPin(true)
                .build();

        MockMultipartFile group = new MockMultipartFile(
                "group",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(groupChatPatchRequest).getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chats/{chatId}", groupChat.getId())
                        .file(group)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Group Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.onlyAdminsCanSend").value(true))
                .andExpect(jsonPath("$.onlyAdminsCanEditGroup").value(true))
                .andExpect(jsonPath("$.onlyAdminsCanInvite").value(true))
                .andExpect(jsonPath("$.onlyAdminsCanPin").value(true));
    }

    @Test
    void shouldNotUpdateGroupChat_WhenUserIsNotAdminAndOnlyAdminsCanUpdateGroup() throws Exception {
        Member regularMember = Member.builder()
                .chat(groupChat)
                .user(user2)
                .memberRole(MemberRole.MEMBER)
                .build();
        groupChat.addMember(regularMember);
        groupChat.setOnlyAdminsCanEditGroup(true);
        chatRepository.save(groupChat);

        String user2Token = jwtService.generateToken(user2.getEmail()).getAccessToken();
        GroupChatPatchRequest groupChatPatchRequest = GroupChatPatchRequest.builder()
                .name("Updated Group Name")
                .build();

        MockMultipartFile group = new MockMultipartFile(
                "group",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(groupChatPatchRequest).getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chats/{chatId}", groupChat.getId())
                        .file(group)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteGroupChat_WhenUserIsOwner() throws Exception {
        mockMvc.perform(delete("/api/chats/{chatId}", groupChat.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/chats/{chatId}", groupChat.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotDeleteGroupChat_WhenUserIsNotOwner() throws Exception {
        Member adminMember = Member.builder()
                .chat(groupChat)
                .user(user2)
                .memberRole(MemberRole.ADMIN)
                .build();
        groupChat.addMember(adminMember);
        chatRepository.save(groupChat);

        String user2Token = jwtService.generateToken(user2.getEmail()).getAccessToken();

        mockMvc.perform(delete("/api/chats/{chatId}", groupChat.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotDeleteIndividualChat() throws Exception {
        mockMvc.perform(delete("/api/chats/{chatId}", chat.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_WhenInvalidImageFormat() throws Exception {

        MockMultipartFile invalidImage = new MockMultipartFile(
                "groupImage",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "invalid image content".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chats")
                        .file(invalidImage)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }
}