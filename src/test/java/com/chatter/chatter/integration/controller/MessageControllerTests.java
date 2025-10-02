package com.chatter.chatter.integration.controller;

import com.chatter.chatter.config.WebsocketTestConfiguration;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import com.chatter.chatter.request.*;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({WebsocketTestConfiguration.class})
public class MessageControllerTests extends BaseIntegrationTest {

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
    private MessageReadRepository messageReadRepository;

    @Autowired
    private InviteRepository inviteRepository;

    private User user1;
    private User user2;
    private Chat individualChat;
    private GroupChat groupChat;
    private Message message;
    private String user1AccessToken;
    private String user2AccessToken;
    private Invite invite;

    @BeforeEach
    public void setup() {
        messageReadRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
        inviteRepository.deleteAll();

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

        individualChat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        groupChat = GroupChat.builder()
                .chatType(ChatType.GROUP)
                .name("Test Group")
                .description("Test Group")
                .image("test.jpg")
                .onlyAdminsCanSend(false)
                .onlyAdminsCanPin(false)
                .build();

        Member individualChatMember1 = Member.builder()
                .user(user1)
                .chat(individualChat)
                .memberRole(MemberRole.MEMBER)
                .build();

        Member individualChatMember2 = Member.builder()
                .user(user2)
                .chat(individualChat)
                .memberRole(MemberRole.MEMBER)
                .build();

        Member groupMember = Member.builder()
                .user(user1)
                .chat(groupChat)
                .memberRole(MemberRole.ADMIN)
                .build();

        message = TextMessage.builder()
                .chat(individualChat)
                .user(user1)
                .content("Test message")
                .messageType(MessageType.TEXT)
                .build();

        groupChat.addMember(groupMember);
        groupChat = chatRepository.save(groupChat);

        individualChat.addMessage(message);
        individualChat.addMember(individualChatMember1);
        individualChat.addMember(individualChatMember2);
        individualChat = chatRepository.save(individualChat);

        invite = inviteRepository.save(Invite.builder()
                .groupChat(groupChat)
                .build());

        user1AccessToken = "Bearer " + jwtService.generateToken(user1.getEmail()).getAccessToken();
        user2AccessToken = "Bearer " + jwtService.generateToken(user2.getEmail()).getAccessToken();
    }

    @Test
    void getAllMessages_ShouldReturnMessages() throws Exception {
        mockMvc.perform(get("/api/messages")
                        .param("chatId", individualChat.getId().toString())
                        .param("size", "20")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[0].content").value("Test message"))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andExpect(jsonPath("$.previousCursor").exists());
    }

    @Test
    void getAllMessages_ShouldReturnBadRequest_WhenBothAfterAndBeforeProvided() throws Exception {
        mockMvc.perform(get("/api/messages")
                        .param("chatId", individualChat.getId().toString())
                        .param("after", "1")
                        .param("before", "2")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMessage_ShouldReturnMessage() throws Exception {
        mockMvc.perform(get("/api/messages/" + message.getId())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.getId()))
                .andExpect(jsonPath("$.content").value("Test message"));
    }

    @Test
    void getMessage_ShouldReturnNotFound_WhenMessageNotExists() throws Exception {
        mockMvc.perform(get("/api/messages/999")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMessage_ShouldCreateTextMessage() throws Exception {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setChatId(individualChat.getId());
        request.setContent("New message");
        request.setMessageType(MessageType.TEXT);

        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile messagePart = new MockMultipartFile("message", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/messages")
                        .file(messagePart)
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("New message"));
    }

    @Test
    void pinMessage_ShouldPinMessage() throws Exception {
        mockMvc.perform(patch("/api/messages/" + message.getId() + "/pin")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void unpinMessage_ShouldUnpinMessage() throws Exception {
        message.setPinned(true);
        messageRepository.save(message);

        mockMvc.perform(patch("/api/messages/" + message.getId() + "/unpin")
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void createMessages_ShouldCreateBatchMessages() throws Exception {
        BatchMessageRequest request = new BatchMessageRequest();
        request.setChatsIds(Set.of(individualChat.getId(), groupChat.getId()));
        request.setContent("Batch message");
        request.setMessageType(MessageType.TEXT);

        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile messagePart = new MockMultipartFile("message", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/messages/batch")
                        .file(messagePart)
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].content").value("Batch message"));
    }

    @Test
    void forwardMessage_ShouldForwardMessage() throws Exception {
        ForwardMessagePostRequest request = new ForwardMessagePostRequest();
        request.setMessageId(message.getId());
        request.setChatIds(Set.of(groupChat.getId()));

        mockMvc.perform(post("/api/messages/forward-message")
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteMessage_ShouldDeleteMessage() throws Exception {
        mockMvc.perform(delete("/api/messages/" + message.getId())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMessage_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        Message otherUserMessage = messageRepository.save(TextMessage.builder()
                .chat(individualChat)
                .user(user2)
                .content("Other user message")
                .messageType(MessageType.TEXT)
                .build());

        mockMvc.perform(delete("/api/messages/" + otherUserMessage.getId())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMessage_ShouldUpdateMessage() throws Exception {
        MessagePatchRequest request = new MessagePatchRequest();
        request.setContent("Updated content");
        request.setContentJson("Updated contentJson");

        mockMvc.perform(patch("/api/messages/" + message.getId())
                        .header("Authorization", user1AccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.contentJson").value("Updated contentJson"));
    }

    @Test
    void acceptInvite_ShouldAcceptInvite() throws Exception {
        InviteMessage inviteMessage = messageRepository.save(InviteMessage.builder()
                .chat(groupChat)
                .user(user1)
                .invite(invite)
                .messageType(MessageType.INVITE)
                .build());

        mockMvc.perform(post("/api/messages/accept-invite/" + inviteMessage.getId())
                        .header("Authorization", user2AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invite accepted"));
    }

    @Test
    void acceptInvite_ShouldReturnBadRequest_WhenNotInviteMessage() throws Exception {
        mockMvc.perform(post("/api/messages/accept-invite/" + message.getId())
                        .header("Authorization", user1AccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMessage_ShouldReturnNotFound_WhenNotMember() throws Exception {
        SingleMessageRequest request = new SingleMessageRequest();
        request.setChatId(groupChat.getId());
        request.setContent("Test message");
        request.setMessageType(MessageType.TEXT);

        String requestJson = objectMapper.writeValueAsString(request);
        MockMultipartFile messagePart = new MockMultipartFile("message", "",
                MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/messages")
                        .file(messagePart)
                        .header("Authorization", user2AccessToken))
                .andExpect(status().isNotFound());
    }
}