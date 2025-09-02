package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import com.chatter.chatter.request.ReactPatchRequest;
import com.chatter.chatter.request.ReactPostRequest;
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
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({AzureBlobStorageTestConfig.class, WebsocketTestConfiguration.class})
public class ReactControllerTest extends BaseIntegrationTest {

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
    private ReactRepository reactRepository;

    private User user1;
    private User user2;
    private Chat chat;
    private Message message;
    private React react;
    private String user1AccessToken;
    private String user2AccessToken;

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

        react = reactRepository.save(React.builder()
                .emoji("testEmoji")
                .user(user1)
                .message(message)
                .build());

        user1AccessToken = "Bearer " + jwtService.generateToken(user1.getEmail()).getAccessToken();
        user2AccessToken = "Bearer " + jwtService.generateToken(user2.getEmail()).getAccessToken();
    }

    @Test
    void createReact_ShouldCreateReact() throws Exception {
        ReactPostRequest request = new ReactPostRequest();
        request.setMessageId(message.getId());
        request.setEmoji("newTestEmoji");

        mockMvc.perform(post("/api/reacts")
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emoji").value("newTestEmoji"));
    }

    @Test
    void createReact_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        ReactPostRequest request = new ReactPostRequest(); 

        mockMvc.perform(post("/api/reacts")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReact_ShouldReturnForbidden_WhenNotMember() throws Exception {
        ReactPostRequest request = new ReactPostRequest();
        request.setMessageId(message.getId());
        request.setEmoji("newTestEmoji");
        User user3 = userRepository.save(User.builder()
                .email("user3@example.com")
                .username("user3")
                .password("password")
                .build());

        mockMvc.perform(post("/api/reacts")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user3.getEmail()).getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReact_ShouldReturnBadRequest_WhenReactAlreadyExists() throws Exception {
        ReactPostRequest request = new ReactPostRequest();
        request.setMessageId(message.getId());
        request.setEmoji("testEmoji"); 

        mockMvc.perform(post("/api/reacts")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReact_ShouldDeleteReact() throws Exception {
        mockMvc.perform(delete("/api/reacts/" + react.getId())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReact_ShouldReturnNotFound_WhenReactNotExists() throws Exception {
        mockMvc.perform(delete("/api/reacts/999")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReact_ShouldReturnNotFound_WhenNotOwner() throws Exception {
        mockMvc.perform(delete("/api/reacts/" + react.getId())
                        .header("Authorization", user2AccessToken)) 
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReact_ShouldUpdateReact() throws Exception {
        ReactPatchRequest request = new ReactPatchRequest();
        request.setEmoji("updatedTestEmoji");

        mockMvc.perform(patch("/api/reacts/" + react.getId())
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emoji").value("updatedTestEmoji"));
    }

    @Test
    void updateReact_ShouldReturnNotFound_WhenReactNotExists() throws Exception {
        ReactPatchRequest request = new ReactPatchRequest();
        request.setEmoji("updatedTestEmoji");

        mockMvc.perform(patch("/api/reacts/999")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReact_ShouldReturnNotFound_WhenNotOwner() throws Exception {
        ReactPatchRequest request = new ReactPatchRequest();
        request.setEmoji("updatedTestEmoji");

        mockMvc.perform(patch("/api/reacts/" + react.getId())
                        .header("Authorization", user2AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReact_ShouldReturnNotFound_WhenMessageNotExists() throws Exception {
        ReactPostRequest request = new ReactPostRequest();
        request.setMessageId(999L); 
        request.setEmoji("testEmoji");

        mockMvc.perform(post("/api/reacts")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void allEndpoints_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        ReactPostRequest postRequest = new ReactPostRequest();
        postRequest.setMessageId(message.getId());
        postRequest.setEmoji("testEmoji");

        ReactPatchRequest patchRequest = new ReactPatchRequest();
        patchRequest.setEmoji("updatedTestEmoji");

        
        mockMvc.perform(post("/api/reacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(delete("/api/reacts/1"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(patch("/api/reacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isUnauthorized());
    }
}