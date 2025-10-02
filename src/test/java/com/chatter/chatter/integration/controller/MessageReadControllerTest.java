package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import com.chatter.chatter.request.MarkChatMessagesAsReadRequest;
import com.chatter.chatter.request.MessageReadBatchPostRequest;
import com.chatter.chatter.request.MessageReadPostRequest;
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
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({WebsocketTestConfiguration.class})
public class MessageReadControllerTest extends BaseIntegrationTest {

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
    private MessageReadRepository messageReadRepository;

    private User user1;
    private User user2;
    private Chat chat;
    private Message message;
    private MessageRead messageRead;
    private String user1AccessToken;
    private String user2AccessToken;

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
                .showMessageReads(true)
                .build());

        user2 = userRepository.save(User.builder()
                .email("user2@example.com")
                .username("user2")
                .password("password")
                .showMessageReads(true)
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
                .createdAt(Instant.now())
                .build();

        chat.addMessage(message);
        chat.addMember(member1);
        chat.addMember(member2);
        chatRepository.save(chat);

        messageRead = messageReadRepository.save(MessageRead.builder()
                .user(user1)
                .message(message)
                .showRead(true)
                .build());

        user1AccessToken = "Bearer " + jwtService.generateToken(user1.getEmail()).getAccessToken();
        user2AccessToken = "Bearer " + jwtService.generateToken(user2.getEmail()).getAccessToken();
    }

    @Test
    void getMessageReads_ShouldReturnMessageReads() throws Exception {
        mockMvc.perform(get("/api/message-reads")
                        .param("messageId", message.getId().toString())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageRead.getId()));
    }

    @Test
    void getMessageReads_ShouldReturnForbidden_WhenNotMember() throws Exception {
        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(get("/api/message-reads")
                        .param("messageId", message.getId().toString())
                        .header("Authorization", user3AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMessageRead_ShouldCreateMessageRead() throws Exception {
        MessageReadPostRequest request = new MessageReadPostRequest();
        request.setMessageId(message.getId());

        mockMvc.perform(post("/api/message-reads")
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createMessageRead_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        MessageReadPostRequest request = new MessageReadPostRequest();

        mockMvc.perform(post("/api/message-reads")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMessageRead_ShouldReturnForbidden_WhenNotMember() throws Exception {
        MessageReadPostRequest request = new MessageReadPostRequest();
        request.setMessageId(message.getId());

        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(post("/api/message-reads")
                        .header("Authorization", user3AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMessageRead_ShouldReturnBadRequest_WhenAlreadyRead() throws Exception {
        MessageReadPostRequest request = new MessageReadPostRequest();
        request.setMessageId(message.getId());

        mockMvc.perform(post("/api/message-reads")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readAllChatMessages_ShouldMarkChatMessagesAsRead() throws Exception {
        MarkChatMessagesAsReadRequest request = new MarkChatMessagesAsReadRequest();
        request.setChatId(chat.getId());

        mockMvc.perform(post("/api/message-reads/chat")
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void readAllChatMessages_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        MarkChatMessagesAsReadRequest request = new MarkChatMessagesAsReadRequest();

        mockMvc.perform(post("/api/message-reads/chat")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readAllChatMessages_ShouldNotCreateMessageReadsAndReturnEmptyList_WhenNotMember() throws Exception {
        MarkChatMessagesAsReadRequest request = new MarkChatMessagesAsReadRequest();
        request.setChatId(chat.getId());

        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(post("/api/message-reads/chat")
                        .header("Authorization", user3AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void batchCreateMessageReads_ShouldCreateMultipleMessageReads() throws Exception {
        MessageReadBatchPostRequest request = new MessageReadBatchPostRequest();
        request.setMessagesIds(Set.of(message.getId()));

        mockMvc.perform(post("/api/message-reads/batch")
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void batchCreateMessageReads_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        MessageReadBatchPostRequest request = new MessageReadBatchPostRequest();

        mockMvc.perform(post("/api/message-reads/batch")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchCreateMessageReads_ShouldNotCreateMessageReadsAndReturnEmptyList_WhenNotMember() throws Exception {
        MessageReadBatchPostRequest request = new MessageReadBatchPostRequest();
        request.setMessagesIds(Set.of(message.getId()));

        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        String user3AccessToken = "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken();

        mockMvc.perform(post("/api/message-reads/batch")
                        .header("Authorization", user3AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void allEndpoints_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        MessageReadPostRequest postRequest = new MessageReadPostRequest();
        postRequest.setMessageId(message.getId());

        MarkChatMessagesAsReadRequest chatRequest = new MarkChatMessagesAsReadRequest();
        chatRequest.setChatId(chat.getId());

        MessageReadBatchPostRequest batchRequest = new MessageReadBatchPostRequest();
        batchRequest.setMessagesIds(Set.of(message.getId()));

        mockMvc.perform(get("/api/message-reads")
                        .param("messageId", message.getId().toString()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/message-reads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/message-reads/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/message-reads/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMessageRead_ShouldReturnBadRequest_WhenOwnMessage() throws Exception {
        Message ownMessage = TextMessage.builder()
                .chat(chat)
                .user(user2)
                .content("Own message")
                .messageType(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        chat.addMessage(ownMessage);
        chatRepository.save(chat);

        MessageReadPostRequest request = new MessageReadPostRequest();
        request.setMessageId(ownMessage.getId());

        mockMvc.perform(post("/api/message-reads")
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}