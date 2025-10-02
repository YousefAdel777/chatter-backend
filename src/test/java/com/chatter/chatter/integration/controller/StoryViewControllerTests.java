package com.chatter.chatter.integration.controller;

import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.StoryRepository;
import com.chatter.chatter.repository.StoryViewRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.StoryViewPostRequest;
import com.chatter.chatter.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class StoryViewControllerTests extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StoryViewRepository storyViewRepository;

    @Autowired
    private ChatRepository chatRepository;

    private User user;
    private User storyOwner;
    private Story story;
    private StoryView storyView;
    private String userAccessToken;
    private String ownerAccessToken;

    @BeforeEach
    public void setup() {
        storyViewRepository.deleteAll();
        storyRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("user@example.com")
                .username("testuser")
                .password("password")
                .build());

        storyOwner = userRepository.save(User.builder()
                .email("owner@example.com")
                .username("storyowner")
                .password("password")
                .build());

        story = storyRepository.save(Story.builder()
                .content("Test story")
                .user(storyOwner)
                .storyType(StoryType.TEXT)
                .createdAt(Instant.now().minusSeconds(3600))
                .build());

        storyView = storyViewRepository.save(StoryView.builder()
                .user(user)
                .story(story)
                .build());

        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        Member storyOwnerMember = Member.builder()
                .chat(chat)
                .user(storyOwner)
                .build();

        Member userMember =  Member.builder()
                .chat(chat)
                .user(user)
                .build();

        chat.addMember(storyOwnerMember);
        chat.addMember(userMember);
        chatRepository.save(chat);

        userAccessToken = jwtService.generateToken(user.getEmail()).getAccessToken();
        ownerAccessToken = jwtService.generateToken(storyOwner.getEmail()).getAccessToken();
    }

    @Test
    void getStoryViews_ShouldReturnViews_WhenUserIsStoryOwner() throws Exception {
        mockMvc.perform(get("/api/story-views")
                        .param("storyId", story.getId().toString())
                        .header("Authorization", "Bearer " + ownerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(storyView.getId()));
    }

    @Test
    void getStoryViews_ShouldReturnForbidden_WhenUserIsNotStoryOwner() throws Exception {
        mockMvc.perform(get("/api/story-views")
                        .param("storyId", story.getId().toString())
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStoryViews_ShouldReturnNotFound_WhenStoryDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/story-views")
                        .param("storyId", "999")
                        .header("Authorization", "Bearer " + ownerAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStoryView_ShouldCreateView_WhenValidRequest() throws Exception {
        StoryViewPostRequest request = new StoryViewPostRequest(story.getId());
        storyViewRepository.delete(storyView);

        mockMvc.perform(post("/api/story-views")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createStoryView_ShouldReturnBadRequest_WhenUserIsStoryOwner() throws Exception {
        StoryViewPostRequest request = new StoryViewPostRequest(story.getId());

        mockMvc.perform(post("/api/story-views")
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStoryView_ShouldReturnBadRequest_WhenViewAlreadyExists() throws Exception {
        StoryViewPostRequest request = new StoryViewPostRequest(story.getId());

        mockMvc.perform(post("/api/story-views")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStoryView_ShouldReturnNotFound_WhenStoryDoesNotExist() throws Exception {
        StoryViewPostRequest request = new StoryViewPostRequest(999L);

        mockMvc.perform(post("/api/story-views")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStoryView_ShouldReturnBadRequest_WhenInvalidRequestBody() throws Exception {
        StoryViewPostRequest request = new StoryViewPostRequest();

        mockMvc.perform(post("/api/story-views")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}