package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.Block;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.BlockRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.BlockPostRequest;
import com.chatter.chatter.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AzureBlobStorageTestConfig.class)
public class BlockControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;

    private User user2;

    private String accessToken;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        blockRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("testUser1")
                .email("testEmail1@example.com")
                .password("testPassword1")
                .build());
        user2 = userRepository.save(User.builder()
                .username("testUser2")
                .email("testEmail2@example.com")
                .password("testPassword2")
                .build());

        accessToken = jwtService.generateToken(user1.getEmail()).getAccessToken();
    }

    @Test
    void shouldGetAllBlocksCreatedByUser() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user2)
                .blockedBy(user1)
                .build());
        mockMvc.perform(get("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(block.getId()))
                .andExpect(jsonPath("$.[0].blockedBy.id").value(user1.getId()))
                .andExpect(jsonPath("$.[0].blockedUser.id").value(user2.getId()));
    }

    @Test
    void shouldGetBlock_WhenUserIsBlockedByCurrentUser() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user2)
                .blockedBy(user1)
                .build());
        mockMvc.perform(get("/api/blocks/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(block.getId()))
                .andExpect(jsonPath("$.blockedBy.id").value(user1.getId()))
                .andExpect(jsonPath("$.blockedUser.id").value(user2.getId()));
    }

    @Test
    void shouldNotGetBlock_WhenUserIsNotBlockedByCurrentUser() throws Exception {
        mockMvc.perform(get("/api/blocks/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnTrue_WhenUserIsBlockedByCurrentUser() throws Exception {
        blockRepository.save(Block.builder()
                .blockedUser(user1)
                .blockedBy(user2)
                .build());
        mockMvc.perform(get("/api/blocks/blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    void shouldReturnFalse_WhenUserIsNotBlockedByCurrentUser() throws Exception {
        mockMvc.perform(get("/api/blocks/blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    void shouldDeleteBlock_WhenBlockExists() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user2)
                .blockedBy(user1)
                .build());
        mockMvc.perform(delete("/api/blocks/" + block.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldNotDeleteBlock_WhenCurrentUserIsNotBlockCreator() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user1)
                .blockedBy(user2)
                .build());
        mockMvc.perform(delete("/api/blocks/" + block.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotDeleteBlock_WhenBlockNotExists() throws Exception {
        mockMvc.perform(delete("/api/blocks/" + 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetBlock_WhenBlockExists() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user2)
                .blockedBy(user1)
                .build());
        mockMvc.perform(get("/api/blocks/" + block.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(block.getId()))
                .andExpect(jsonPath("$.blockedBy.id").value(user1.getId()))
                .andExpect(jsonPath("$.blockedUser.id").value(user2.getId()));
    }

    @Test
    void shouldNotGetBlock_WhenBlockNotExists() throws Exception {
        mockMvc.perform(get("/api/blocks/" + 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotGetBlock_WhenCurrentUserIsNotBlockCreator() throws Exception {
        Block block = blockRepository.save(Block.builder()
                .blockedUser(user1)
                .blockedBy(user2)
                .build());
        mockMvc.perform(get("/api/blocks/" + block.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", user2.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldCreateBlock_WhenNoBlockExists() throws Exception {
        blockRepository.deleteAll();
        BlockPostRequest request = new BlockPostRequest(user2.getId());
        mockMvc.perform(post("/api/blocks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.blockedBy.id").value(user1.getId()))
                .andExpect(jsonPath("$.blockedUser.id").value(user2.getId()));
    }

    @Test
    void shouldNotCreateBlock_WhenUserBlocksThemselves() throws Exception {
        BlockPostRequest request = new BlockPostRequest(user1.getId());
        mockMvc.perform(post("/api/blocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotCreateBlock_WhenBlockExists() throws Exception {
        blockRepository.save(Block.builder()
                .blockedUser(user2)
                .blockedBy(user1)
                .build());
        BlockPostRequest request = new BlockPostRequest(user2.getId());
        mockMvc.perform(post("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
