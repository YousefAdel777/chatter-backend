package com.chatter.chatter.integration.controller;

import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.UserPatchRequest;
import com.chatter.chatter.request.UserRegisterRequest;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CacheManager cacheManager;

    private String accessToken;

    private User user1;

    private User user2;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        chatRepository.deleteAll();
        memberRepository.deleteAll();

        user1 = User.builder()
                .username("testUser1")
                .email("testEmail1@example.com")
                .password("testPassword1")
                .build();

        user2 = User.builder()
                .username("testUser2")
                .email("testEmail2@example.com")
                .password("testPassword2")
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        accessToken = jwtService.generateToken(user1.getEmail()).getAccessToken();
    }

    @Test
    void createUser_ShouldCreateUser_WhenValidRequest() throws Exception {
        String username = "testUser3";
        String password = "testPassword3";
        String email = "testEmail3@example.com";

        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenUsernameIsEmpty() throws Exception {
        String username = "";
        String password = "testPassword3";
        String email = "testEmail3@example.com";
        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailIsEmpty() throws Exception {
        String username = "testUser3";
        String password = "testPassword3";
        String email = "";
        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenPasswordIsEmpty() throws Exception {
        String username = "testUser3";
        String password = "";
        String email = "testEmail3@example.com";
        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailIsNotUnique() throws Exception {
        String username = "testUser3";
        String password = "testPassword3";
        String email = "testEmail2@example.com";
        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        String username = "testUser3";
        String password = "testPassword3";
        String email = "testEmail3";
        UserRegisterRequest request = new UserRegisterRequest(email, username, password);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getAllUsers_ShouldReturnFilteredUsers_WhenUsernameFilterProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("username", "testUser1")
                        .param("email", "testUser1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testUser1"));
    }

    @Test
    void getAllUsers_ShouldReturnFilteredUsers_WhenEmailFilterProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("email", "testEmail2@example.com")
                        .param("username", "testEmail2@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("testEmail2@example.com"));
    }

    @Test
    void getAllUsers_ShouldReturnPaginatedUsers_WhenPaginationParametersProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("size", "1")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsersMatchFilter() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("username", "nonexistentUser")
                        .param("email", "nonexistentEmail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() throws Exception {
        mockMvc.perform(get("/api/users/" + user1.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("testUser1"))
                .andExpect(jsonPath("$.email").value("testEmail1@example.com"));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/3").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentUser_ShouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser1"))
                .andExpect(jsonPath("$.email").value("testEmail1@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deleteCurrentUser_ShouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateCurrentUser_ShouldUpdateUser_WhenValidRequest() throws Exception {
        UserPatchRequest request = UserPatchRequest.builder()
                .username("newTestUser1")
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/users/me")
                        .file(userPart)
                        .with(requestPostProcessor -> {
                            requestPostProcessor.setMethod("PATCH");
                            return requestPostProcessor;
                        })
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newTestUser1"));
    }

    @Test
    void updateCurrentUser_ShouldReturnBadRequest_WhenEmailIsDuplicate() throws Exception {
        UserPatchRequest request = UserPatchRequest.builder()
                .email("testEmail2@example.com")
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/users/me")
                        .file(userPart)
                        .with(postProcessor -> {
                            postProcessor.setMethod("PATCH");
                            return postProcessor;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUserContacts_ShouldReturnContacts_WhenIndividualChatExists() throws Exception {
        Chat chat = Chat.builder().chatType(ChatType.INDIVIDUAL).build();
        Member member1 = Member.builder().chat(chat).user(userRepository.findByEmail("testEmail1@example.com").get()).build();
        Member member2 = Member.builder().chat(chat).user(userRepository.findByEmail("testEmail2@example.com").get()).build();
        chat.addMember(member1);
        chat.addMember(member2);
        chatRepository.save(chat);

        mockMvc.perform(get("/api/users/me/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("testUser2"));

        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    void getCurrentUserContacts_ShouldReturnEmptyList_WhenOnlyGroupChatsExist() throws Exception {
        Chat chat = Chat.builder().chatType(ChatType.GROUP).build();
        Member member1 = Member.builder().chat(chat).user(userRepository.findByEmail("testEmail1@example.com").get()).build();
        Member member2 = Member.builder().chat(chat).user(userRepository.findByEmail("testEmail2@example.com").get()).build();
        chat.addMember(member1);
        chat.addMember(member2);
        chatRepository.save(chat);

        mockMvc.perform(get("/api/users/me/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}