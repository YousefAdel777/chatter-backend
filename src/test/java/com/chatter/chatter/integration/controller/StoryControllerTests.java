package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.request.StoryPatchRequest;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AzureBlobStorageTestConfig.class)
public class StoryControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryViewRepository storyViewRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private User user;
    private User otherUser;
    private Story textStory;
    private Story excludedUserStory;
    private String userAccessToken;
    private String otherUserAccessToken;

    @BeforeEach
    public void setup() {
        storyViewRepository.deleteAll();
        storyRepository.deleteAll();
        storyRepository.deleteAll();
        userRepository.deleteAll();
        chatRepository.deleteAll();

        Chat chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        user = userRepository.save(User.builder()
                .email("user@example.com")
                .username("testUsername")
                .password("testPassword")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("test2@example.com")
                .username("testUsername2")
                .password("testPassword2")
                .build());

        Member member1 = Member.builder()
                .user(user)
                .chat(chat)
                .build();

        Member member2 = Member.builder()
                .user(otherUser)
                .chat(chat)
                .build();

        chat.addMember(member1);
        chat.addMember(member2);
        chatRepository.save(chat);

        textStory = storyRepository.save(TextStory.builder()
                .content("Test text story")
                .textColor("#000000")
                .backgroundColor("#ffffff")
                .user(user)
                .storyType(StoryType.TEXT)
                .build());

        storyRepository.save(TextStory.builder()
                .user(otherUser)
                .content("Test text story 2")
                .textColor("#000000")
                .backgroundColor("#ffffff")
                .storyType(StoryType.TEXT)
                .build());

        storyRepository.save(MediaStory.builder()
                .content("Test media story")
                .user(user)
                .filePath("test.jpg")
                .storyType(StoryType.IMAGE)
                .build());

        excludedUserStory = storyRepository.save(TextStory.builder()
                .content("Excluded user story")
                .user(user)
                .textColor("#000000")
                .backgroundColor("#ffffff")
                .storyType(StoryType.TEXT)
                .createdAt(Instant.now().minusSeconds(7200)) 
                .build());

        excludedUserStory.getExcludedUsers().add(otherUser);
        storyRepository.save(excludedUserStory);

        userAccessToken = jwtService.generateToken(user.getEmail()).getAccessToken();
        otherUserAccessToken = jwtService.generateToken(otherUser.getEmail()).getAccessToken();
    }

    @Test
    void getStories_ShouldReturnStories() throws Exception {
        mockMvc.perform(get("/api/stories")
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test text story"))
                .andExpect(jsonPath("$[0].textColor").value("#000000"))
                .andExpect(jsonPath("$[0].backgroundColor").value("#ffffff"));
    }

    @Test
    void getStories_ShouldNotReturnExcludedStories() throws Exception {
        mockMvc.perform(get("/api/stories")
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.content == 'Excluded user story')]").doesNotExist());
    }

    @Test
    void getCurrentUserStories_ShouldReturnUserStories() throws Exception {
        mockMvc.perform(get("/api/stories/me")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test text story"))
                .andExpect(jsonPath("$[1].content").value("Test media story"))
                .andExpect(jsonPath("$[2].content").value("Excluded user story"));
    }

    @Test
    void getStory_ShouldReturnStory() throws Exception {
        mockMvc.perform(get("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(textStory.getId()))
                .andExpect(jsonPath("$.content").value("Test text story"));
    }

    @Test
    void getStory_ShouldReturnNotFound_WhenStoryDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/stories/999")
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStory_ShouldReturnNotFound_WhenUserExcluded() throws Exception {
        mockMvc.perform(get("/api/stories/" + excludedUserStory.getId())
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStory_ShouldCreateTextStory() throws Exception {
        StoryPostRequest request = StoryPostRequest.builder()
                .content("New text story")
                .storyType(StoryType.TEXT)
                .textColor("#000000")
                .backgroundColor("#ffffff")
                .build();

        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile storyPart = new MockMultipartFile("story", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/stories")
                        .file(storyPart)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("New text story"))
                .andExpect(jsonPath("$.textColor").value("#000000"))
                .andExpect(jsonPath("$.backgroundColor").value("#ffffff"));
    }

    @Test
    void createStory_ShouldCreateMediaStory() throws Exception {
        StoryPostRequest request = StoryPostRequest.builder()
                .content("New media story")
                .storyType(StoryType.IMAGE)
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg",
                MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());
        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile storyPart = new MockMultipartFile("story", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/stories")
                        .file(storyPart)
                        .file(file)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("New media story"));
    }

    @Test
    void createStory_ShouldReturnBadRequest_WhenInvalidStoryType() throws Exception {
        StoryPostRequest request = StoryPostRequest.builder()
                .content("New media story")
                .storyType(null)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile storyPart = new MockMultipartFile("story", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/stories")
                        .file(storyPart)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStory_ShouldUpdateStory() throws Exception {
        StoryPatchRequest request = new StoryPatchRequest(Set.of(otherUser.getId()));

        mockMvc.perform(patch("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excludedUsersIds.length()").value(1));
    }

    @Test
    void updateStory_ShouldReturnForbidden_WhenNotStoryOwner() throws Exception {
        StoryPatchRequest request = new StoryPatchRequest(Collections.emptySet());

        mockMvc.perform(patch("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteStory_ShouldDeleteStory() throws Exception {
        mockMvc.perform(delete("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStory_ShouldReturnForbidden_WhenNotStoryOwner() throws Exception {
        mockMvc.perform(delete("/api/stories/" + textStory.getId())
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isForbidden());
    }
}