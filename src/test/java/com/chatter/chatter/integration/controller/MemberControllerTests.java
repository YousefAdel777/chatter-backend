package com.chatter.chatter.integration.controller;

import com.chatter.chatter.dto.MemberPatchRequest;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MemberControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String testUserAccessToken;
    private String adminUserAccessToken;
    private User testUser;
    private User adminUser;
    private GroupChat groupChat;
    private Member testMember;
    private Member adminMember;
    private Member chatMember1;
    private Member chatMember2;

    @BeforeEach
    public void setup() {
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .username("testUsername")
                .password("testUserPassword")
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .username("testAdmin")
                .password("testAdminPassword")
                .build());

        GroupChat group = GroupChat.builder()
                .name("Test Group Name")
                .description("Test Group Description")
                .image("test.jpg")
                .chatType(ChatType.GROUP)
                .build();

        Chat chat = Chat.builder().build();

        testMember = Member.builder()
                .user(testUser)
                .chat(group)
                .memberRole(MemberRole.MEMBER)
                .build();

        adminMember = Member.builder()
                .user(adminUser)
                .chat(group)
                .memberRole(MemberRole.ADMIN)
                .build();

        chatMember1 = Member.builder()
                .user(testUser)
                .chat(chat)
                .build();

        chatMember2 = Member.builder()
                .user(adminUser)
                .chat(chat)
                .build();

        group.addMember(testMember);
        group.addMember(adminMember);
        groupChat = chatRepository.save(group);

        chat.addMember(chatMember1);
        chat.addMember(chatMember2);
        chatRepository.save(chat);

        testUserAccessToken = jwtService.generateToken(testUser.getEmail()).getAccessToken();
        adminUserAccessToken = jwtService.generateToken(adminUser.getEmail()).getAccessToken();
    }

    @Test
    void getMembers_ShouldReturnMembers() throws Exception {
        mockMvc.perform(get("/api/members")
                        .header("Authorization", "Bearer " + testUserAccessToken)
                        .param("chatId", groupChat.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMembers_ShouldReturnFilteredMembers() throws Exception {
        mockMvc.perform(get("/api/members")
                        .header("Authorization", "Bearer " + testUserAccessToken)
                        .param("chatId", groupChat.getId().toString())
                        .param("username", "testUser")
                        .param("email", "testUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCurrentMember_ShouldReturnCurrentMember() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + testUserAccessToken)
                        .param("chatId", groupChat.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberRole").value("MEMBER"));
    }

    @Test
    void getMember_ShouldReturnMember() throws Exception {
        mockMvc.perform(get("/api/members/" + testMember.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMember.getId()));
    }

    @Test
    void getMember_ShouldThrowNotFound_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/members/999")
                        .header("Authorization", "Bearer " + testUserAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMember_ShouldDelete_WhenSelfRemoval() throws Exception {
        mockMvc.perform(delete("/api/members/" + testMember.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMember_ShouldDelete_WhenAdminRemovesOther() throws Exception {
        mockMvc.perform(delete("/api/members/" + testMember.getId())
                        .header("Authorization", "Bearer " + adminUserAccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMember_ShouldThrow_WhenNonAdminRemovesOther() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testUsername")
                .email("testEmail@example.com")
                .password("testPassword")
                .build());
        Member otherMember = Member.builder()
                .user(user)
                .chat(groupChat)
                .memberRole(MemberRole.MEMBER)
                .build();
        groupChat.addMember(otherMember);
        chatRepository.save(groupChat);
        Member member = memberRepository.findByChatIdAndUserEmail(groupChat.getId(), otherMember.getUser().getEmail()).orElseThrow();

        mockMvc.perform(delete("/api/members/" + member.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMember_ShouldThrow_WhenChatIsNotGroup() throws Exception {
        mockMvc.perform(delete("/api/members/" + chatMember2.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMember_ShouldUpdateRole() throws Exception {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);

        mockMvc.perform(patch("/api/members/" + testMember.getId())
                        .header("Authorization", "Bearer " + adminUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberRole").value("ADMIN"));
    }

    @Test
    void updateMember_ShouldThrow_WhenNotAdmin() throws Exception {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);

        mockMvc.perform(patch("/api/members/" + adminMember.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMember_ShouldThrow_WhenChatIsNotGroup() throws Exception {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);

        mockMvc.perform(patch("/api/members/" + chatMember2.getId())
                        .header("Authorization", "Bearer " + testUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}