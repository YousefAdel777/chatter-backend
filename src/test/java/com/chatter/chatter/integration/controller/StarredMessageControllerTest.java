package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({AzureBlobStorageTestConfig.class, WebsocketTestConfiguration.class})
public class StarredMessageControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StarredMessageRepository starredMessageRepository;

    private User user1;
    private User user2;
    private Chat chat;
    private Message message;
    private StarredMessage starredMessage;
    private String user1AccessToken;
    private String user2AccessToken;

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

        chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        Member member1 = Member.builder()
                .user(user1)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();

        Member member2 = Member.builder()
                .user(user2)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();

        message = TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("Test message")
                .messageType(MessageType.TEXT)
                .build();

        chat.addMessage(message);
        chat.addMember(member1);
        chat.addMember(member2);
        chatRepository.save(chat);

        starredMessage = starredMessageRepository.save(StarredMessage.builder()
                .user(user1)
                .message(message)
                .build());

        user1AccessToken = "Bearer " + jwtService.generateToken(user1.getEmail()).getAccessToken();
        user2AccessToken = "Bearer " + jwtService.generateToken(user2.getEmail()).getAccessToken();
    }

    @Test
    void starMessage_ShouldStarMessage() throws Exception {
        mockMvc.perform(post("/api/starred-messages/" + message.getId() + "/star")
                        .header("Authorization", user2AccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void starMessage_ShouldReturnBadRequest_WhenAlreadyStarred() throws Exception {
        mockMvc.perform(post("/api/starred-messages/" + message.getId() + "/star")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void starMessage_ShouldReturnForbidden_WhenNotMember() throws Exception {
        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(post("/api/starred-messages/" + message.getId() + "/star")
                        .header("Authorization", user3AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void starMessage_ShouldReturnNotFound_WhenMessageNotExists() throws Exception {
        mockMvc.perform(post("/api/starred-messages/999/star")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unstarMessage_ShouldUnstarMessage() throws Exception {
        mockMvc.perform(delete("/api/starred-messages/" + message.getId() + "/unstar")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void unstarMessage_ShouldReturnNotFound_WhenNotStarred() throws Exception {
        mockMvc.perform(delete("/api/starred-messages/" + message.getId() + "/unstar")
                        .header("Authorization", user2AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unstarMessage_ShouldReturnNotFound_WhenNotMember() throws Exception {
        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(delete("/api/starred-messages/" + message.getId() + "/unstar")
                        .header("Authorization", user3AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unstarMessage_ShouldReturnNotFound_WhenMessageNotStarred() throws Exception {
        mockMvc.perform(delete("/api/starred-messages/999/unstar")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void starMessage_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(post("/api/starred-messages/" + message.getId() + "/star"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unstarMessage_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(delete("/api/starred-messages/" + message.getId() + "/unstar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void starMessage_ShouldReturnCreatedStatus() throws Exception {
        Message newMessage = messageRepository.save(TextMessage.builder()
                .chat(chat)
                .user(user1)
                .content("New message")
                .messageType(MessageType.TEXT)
                .build());

        mockMvc.perform(post("/api/starred-messages/" + newMessage.getId() + "/star")
                        .header("Authorization", user2AccessToken))
                .andExpect(status().isCreated());
    }

    @Test
    void unstarMessage_ShouldReturnNoContentStatus() throws Exception {
        mockMvc.perform(delete("/api/starred-messages/" + message.getId() + "/unstar")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNoContent());
    }
}