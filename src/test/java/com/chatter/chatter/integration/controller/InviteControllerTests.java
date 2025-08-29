package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.InviteRepository;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.InvitePostRequest;
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

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({AzureBlobStorageTestConfig.class, WebsocketTestConfiguration.class})
public class InviteControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private GroupChat groupChat;
    private Invite linkInvite;
    private Invite expiredInvite;
    private Invite messageInvite;
    private User user;
    private User adminUser;
    private Member member;
    private Member adminMember;
    private String userAccessToken;
    private String adminAccessToken;

    @BeforeEach
    public void setup() {
        inviteRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("user@example.com")
                .username("testuser")
                .password("password")
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .username("adminuser")
                .password("password")
                .build());

        groupChat = GroupChat.builder()
                .name("Test Group")
                .description("Test Description")
                .image("test.jpg")
                .chatType(ChatType.GROUP)
                .onlyAdminsCanInvite(false)
                .build();

        member = Member.builder()
                .user(user)
                .chat(groupChat)
                .memberRole(MemberRole.MEMBER)
                .build();

        adminMember = Member.builder()
                .user(adminUser)
                .chat(groupChat)
                .memberRole(MemberRole.ADMIN)
                .build();

        groupChat.addMember(member);
        groupChat.addMember(adminMember);
        groupChat = chatRepository.save(groupChat);

        linkInvite = inviteRepository.save(Invite.builder()
                .groupChat(groupChat)
                .canUseLink(true)
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build());

        expiredInvite = inviteRepository.save(Invite.builder()
                .groupChat(groupChat)
                .canUseLink(true)
                .expiresAt(Instant.now().minus(Duration.ofHours(1)))
                .build());

        messageInvite = inviteRepository.save(Invite.builder()
                .groupChat(groupChat)
                .canUseLink(false)
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build());

        userAccessToken = jwtService.generateToken(user.getEmail()).getAccessToken();
        adminAccessToken = jwtService.generateToken(adminUser.getEmail()).getAccessToken();
    }

    @Test
    void getInvite_ShouldReturnInvite() throws Exception {
        mockMvc.perform(get("/api/invites/" + linkInvite.getId())
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(linkInvite.getId()));
    }

    @Test
    void getInvite_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/invites/999")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createInvite_ShouldCreateInvite() throws Exception {
        InvitePostRequest request = new InvitePostRequest(
                groupChat.getId(),
                Instant.now().plus(Duration.ofDays(1)),
                true
        );

        mockMvc.perform(post("/api/invites")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canUseLink").value(true));
    }

    @Test
    void createInvite_ShouldCreateInviteWithNoExpiration() throws Exception {
        InvitePostRequest request = new InvitePostRequest(
                groupChat.getId(),
                null,
                false
        );

        mockMvc.perform(post("/api/invites")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canUseLink").value(false));
    }

    @Test
    void createInvite_ShouldReturnBadRequest_WhenPastExpiresAt() throws Exception {
        InvitePostRequest request = new InvitePostRequest(
                groupChat.getId(),
                Instant.now().minus(Duration.ofDays(1)),
                true
        );

        mockMvc.perform(post("/api/invites")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInvite_ShouldReturnForbidden_WhenOnlyAdminsCanInviteAndUserNotAdmin() throws Exception {
        groupChat.setOnlyAdminsCanInvite(true);
        chatRepository.save(groupChat);

        InvitePostRequest request = new InvitePostRequest(
                groupChat.getId(),
                Instant.now().plus(Duration.ofDays(1)),
                true
        );

        mockMvc.perform(post("/api/invites")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInvite_ShouldSucceedWhenOnlyAdminsCanInviteAndUserIsAdmin() throws Exception {
        groupChat.setOnlyAdminsCanInvite(true);
        chatRepository.save(groupChat);

        InvitePostRequest request = new InvitePostRequest(
                groupChat.getId(),
                Instant.now().plus(Duration.ofDays(1)),
                true
        );

        mockMvc.perform(post("/api/invites")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptInvite_ShouldAcceptLinkInvite() throws Exception {
        User nonMemberUser = userRepository.save(User.builder()
                        .username("testUsername1")
                        .email("testEmail1@example.com")
                        .password("testPassword1")
                        .build());
        mockMvc.perform(post("/api/invites/" + linkInvite.getId() + "/accept")
                        .header("Authorization", "Bearer " + jwtService.generateToken(nonMemberUser.getEmail()).getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invite accepted"));
    }

    @Test
    void acceptInvite_ShouldReturnBadRequest_WhenAlreadyMember() throws Exception {
        mockMvc.perform(post("/api/invites/" + linkInvite.getId() + "/accept")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvite_ShouldReturnBadRequest_WhenExpired() throws Exception {
        mockMvc.perform(post("/api/invites/" + expiredInvite.getId() + "/accept")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvite_ShouldReturnBadRequest_WhenCannotUseLink() throws Exception {
        mockMvc.perform(post("/api/invites/" + messageInvite.getId() + "/accept")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isBadRequest());
    }
}