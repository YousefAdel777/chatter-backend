package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.AzureBlobStorageTestConfig;
import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.dto.VotePostRequest;
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
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({AzureBlobStorageTestConfig.class, WebsocketTestConfiguration.class})
public class VoteControllerTest extends BaseIntegrationTest {

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
    private MemberRepository memberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private VoteRepository voteRepository;

    private User user;
    private User otherUser;
    private Chat chat;
    private PollMessage pollMessage;
    private Option option1;
    private Option option2;
    private String otherUserAccessToken;

    @BeforeEach
    public void setup() {
        voteRepository.deleteAll();
        optionRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("user@example.com")
                .username("testUsername")
                .password("password")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .username("testOtherUsername")
                .password("password")
                .build());

        chat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        Member userMember = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();

        Member otherUserMember = memberRepository.save(Member.builder()
                .user(otherUser)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        chat.addMember(userMember);
        chat.addMember(otherUserMember);
        chatRepository.save(chat);

        pollMessage = PollMessage.builder()
                .chat(chat)
                .user(user)
                .title("Test poll title")
                .multiple(true)
                .messageType(MessageType.POLL)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();
        option1 = Option.builder()
                .pollMessage(pollMessage)
                .title("Option 1")
                .build();

        option2 = Option.builder()
                .pollMessage(pollMessage)
                .title("Option 2")
                .build();

        pollMessage.setOptions(List.of(option1, option2));
        messageRepository.save(pollMessage);

        otherUserAccessToken = jwtService.generateToken(otherUser.getEmail()).getAccessToken();
    }

    @Test
    void createVotes_ShouldCreateVotes_WhenValidRequest() throws Exception {
        VotePostRequest request = new VotePostRequest(Set.of(option1.getId(), option2.getId()));

        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].id").exists());
    }

    @Test
    void createVotes_ShouldReturnBadRequest_WhenNoOptions() throws Exception {
        VotePostRequest request = new VotePostRequest(Set.of());

        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVotes_ShouldReturnBadRequest_WhenSinglePollAndMultipleOptions() throws Exception {
        PollMessage singlePoll = PollMessage.builder()
                .chat(chat)
                .user(user)
                .title("Single poll title")
                .multiple(false)
                .messageType(MessageType.POLL)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();

        Option singleOption1 = Option.builder()
                .pollMessage(singlePoll)
                .title("Single Option 1")
                .build();

        Option singleOption2 = Option.builder()
                .pollMessage(singlePoll)
                .title("Single Option 2")
                .build();

        singlePoll.setOptions(List.of(singleOption1, singleOption2));
        messageRepository.save(singlePoll);

        VotePostRequest request = new VotePostRequest(Set.of(singleOption1.getId(), singleOption2.getId()));
        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteVotes_ShouldDeleteVotes_WhenValidRequest() throws Exception {
        voteRepository.save(Vote.builder()
                .user(otherUser)
                .option(option1)
                .build());

        mockMvc.perform(delete("/api/votes")
                        .param("messageId", pollMessage.getId().toString())
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteVotes_ShouldReturnBadRequest_WhenNotPollMessage() throws Exception {
        mockMvc.perform(delete("/api/votes")
                        .param("messageId", "999")
                        .header("Authorization", "Bearer " + otherUserAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVotes_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        String invalidJson = "{ invalid: json }";

        mockMvc.perform(post("/api/votes")
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}