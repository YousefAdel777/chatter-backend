package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.mapper.ChatProjectionMapper;
import com.chatter.chatter.request.GroupChatPatchRequest;
import com.chatter.chatter.request.GroupChatPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.ChatMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTests {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private ChatProjectionMapper chatProjectionMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ChatService chatService;

    private User user;
    private Chat chat;
    private GroupChat groupChat;
    private Member member;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testUser")
                .build();

        chat = Chat.builder()
                .id(1L)
                .chatType(ChatType.INDIVIDUAL)
                .build();

        groupChat = GroupChat.builder()
                .id(2L)
                .name("Test Group")
                .description("Test Description")
                .chatType(ChatType.GROUP)
                .onlyAdminsCanSend(false)
                .onlyAdminsCanEditGroup(false)
                .onlyAdminsCanInvite(false)
                .onlyAdminsCanPin(false)
                .build();

        member = Member.builder()
                .id(1L)
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();
    }

    @Test
    void shouldCreateIndividualChat() {
        Set<Long> userIds = Set.of(1L, 2L);
        Member member1 = Member.builder().user(user).chat(chat).build();
        Member member2 = Member.builder().user(User.builder().id(2L).build()).chat(chat).build();

        when(memberService.createMemberEntity(eq(1L), any(Chat.class), eq(MemberRole.MEMBER))).thenReturn(member1);
        when(memberService.createMemberEntity(eq(2L), any(Chat.class), eq(MemberRole.MEMBER))).thenReturn(member2);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        Chat result = chatService.createChat(userIds);

        assertNotNull(result);
        assertEquals(ChatType.INDIVIDUAL, result.getChatType());
        verify(chatRepository).save(any(Chat.class));
        verify(memberService, times(2)).createMemberEntity(anyLong(), any(Chat.class), eq(MemberRole.MEMBER));
    }

    @Test
    void shouldCreateGroupChatWithoutImage() {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setDescription("Test Description");
        request.setOnlyAdminsCanSend(false);
        request.setOnlyAdminsCanEditGroup(false);
        request.setOnlyAdminsCanInvite(false);
        request.setOnlyAdminsCanPin(false);

        when(memberService.createMember(eq(email), any(GroupChat.class), eq(MemberRole.OWNER))).thenReturn(member);
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        GroupChat result = chatService.createGroupChat(email, request);

        assertNotNull(result);
        verify(chatRepository).save(any(GroupChat.class));
        verify(memberService).createMember(eq(email), any(GroupChat.class), eq(MemberRole.OWNER));
    }

    @Test
    void shouldCreateGroupChatWithImage() throws IOException {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setGroupImage(multipartFile);

        when(fileUploadService.isImage(multipartFile)).thenReturn(true);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("/path/to/image.jpg");
        when(memberService.createMember(eq(email), any(GroupChat.class), eq(MemberRole.OWNER))).thenReturn(member);
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        GroupChat result = chatService.createGroupChat(email, request);

        assertNotNull(result);
        verify(fileUploadService).uploadFile(multipartFile);
    }

    @Test
    void shouldThrowBadRequest_WhenInvalidImageInGroupChatCreation() {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setGroupImage(multipartFile);

        when(fileUploadService.isImage(multipartFile)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> chatService.createGroupChat(email, request));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void shouldUpdateGroupChat_WhenMember() {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setName("Updated Name");

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        Chat result = chatService.updateGroupChat(email, 2L, request);

        assertNotNull(result);
        verify(chatRepository).save(groupChat);
    }

    @Test
    void shouldUpdateGroupChat_WhenAdminUpdatesGroupChatWithRestrictions() {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setName("Updated Name");

        groupChat.setOnlyAdminsCanEditGroup(true);
        member.setMemberRole(MemberRole.ADMIN);

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        Chat result = chatService.updateGroupChat(email, 2L, request);

        assertNotNull(result);
        verify(chatRepository).save(groupChat);
    }


    @Test
    void shouldThrowForbidden_WhenNonAdminUpdatesGroupChatWithRestrictions() {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setName("Updated Name");

        groupChat.setOnlyAdminsCanEditGroup(true);

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);

        assertThrows(ForbiddenException.class, () -> chatService.updateGroupChat(email, 2L, request));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void shouldUpdateGroupChatImage() throws IOException {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setGroupImage(multipartFile);

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);
        when(fileUploadService.isImage(multipartFile)).thenReturn(true);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("/new/image/path.jpg");
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        Chat result = chatService.updateGroupChat(email, 2L, request);

        assertNotNull(result);
        verify(fileUploadService).uploadFile(multipartFile);
    }

    @Test
    void shouldDeleteGroupChat_WhenOwner() {
        member.setMemberRole(MemberRole.OWNER);
        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(member.getUser().getEmail(), 2L)).thenReturn(member);

        chatService.deleteGroupChat(member.getUser().getEmail(), 2L);

        verify(chatRepository).delete(groupChat);
    }

    @Test
    void shouldThrowForbidden_WhenNonOwnerDeletesGroupChat() {
        String email = "test@example.com";

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);

        assertThrows(ForbiddenException.class, () ->
                chatService.deleteGroupChat(email, 2L));
        verify(chatRepository, never()).delete(any(Chat.class));
    }

    @Test
    void shouldGetChatEntity() {
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        Chat result = chatService.getChatEntity(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowNotFoundException_WhenChatNotFound() {
        when(chatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> chatService.getChatEntity(999L));
    }

    @Test
    void shouldGetChatEntityIfMember() {
        String email = "test@example.com";
        when(chatRepository.findChatById(email, 1L)).thenReturn(Optional.of(chat));

        Chat result = chatService.getChatEntityIfMember(email, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldFindOrCreateChat_WhenChatExists() {
        Set<Long> userIds = Set.of(1L, 2L);

        when(chatRepository.findByUsers(2L, ChatType.INDIVIDUAL, userIds)).thenReturn(Optional.of(chat));

        Chat result = chatService.findOrCreateChat(userIds);

        assertNotNull(result);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void shouldFindOrCreateChat_WhenChatDoesNotExist() {
        Set<Long> userIds = Set.of(1L, 2L);
        Member member1 = Member.builder().user(user).chat(chat).build();
        Member member2 = Member.builder().user(User.builder().id(2L).build()).chat(chat).build();

        when(chatRepository.findByUsers(2L, ChatType.INDIVIDUAL, userIds)).thenReturn(Optional.empty());
        when(memberService.createMemberEntity(eq(1L), any(Chat.class), eq(MemberRole.MEMBER))).thenReturn(member1);
        when(memberService.createMemberEntity(eq(2L), any(Chat.class), eq(MemberRole.MEMBER))).thenReturn(member2);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        Chat result = chatService.findOrCreateChat(userIds);

        assertNotNull(result);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void shouldEvictChatCache() {
        User user1 = User.builder().id(1L).email("user1@example.com").build();
        User user2 = User.builder().id(2L).email("user2@example.com").build();
        Member member1 = Member.builder().user(user1).chat(chat).build();
        Member member2 = Member.builder().user(user2).chat(chat).build();

        chat.setMembers(Set.of(member1, member2));

        when(redisTemplate.keys("chats::email:user1@example.com*")).thenReturn(Set.of("key1"));
        when(redisTemplate.keys("chats::email:user2@example.com*")).thenReturn(Set.of("key2"));

        chatService.evictChatCache(chat);

        verify(redisTemplate).delete(Set.of("key1"));
        verify(redisTemplate).delete(Set.of("key2"));
    }

    @Test
    void shouldEvictChatCacheForUser() {
        when(redisTemplate.keys("chats::email:test@example.com*")).thenReturn(Set.of("key1", "key2"));

        chatService.evictChatCacheForUser("test@example.com");

        verify(redisTemplate).delete(Set.of("key1", "key2"));
    }

    @Test
    void shouldHandleIOExceptionInGroupChatCreation() throws IOException {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setGroupImage(multipartFile);

        when(fileUploadService.isImage(multipartFile)).thenReturn(true);
        when(fileUploadService.uploadFile(multipartFile)).thenThrow(new IOException("Upload failed"));

        assertThrows(RuntimeException.class, () -> chatService.createGroupChat(email, request));
    }

    @Test
    void shouldSetDefaultImage_WhenNoImageProvidedInUpdate() {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setName("Updated Name");

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        Chat result = chatService.updateGroupChat(email, 2L, request);

        assertNotNull(result);
        verify(chatRepository).save(groupChat);
    }

    @Test
    void shouldGetAllChatsByEmail() {
        String userEmail = "test@example.com";
        List<Chat> chats = Arrays.asList(chat, groupChat);
        List<ChatDto> chatDtos = Arrays.asList(new ChatDto(), new ChatDto());

        when(chatRepository.findAll(any(Specification.class))).thenReturn(chats);
        when(chatProjectionMapper.toDtoList(anyList())).thenReturn(chatDtos);

        List<ChatDto> result = chatService.getAllChatsByEmail(userEmail, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatRepository).findAll(any(Specification.class));
    }

    @Test
    void shouldGetChat() {
        String email = "test@example.com";
        ChatDto chatDto = new ChatDto();

        when(chatRepository.findChatById(email, 1L)).thenReturn(Optional.of(chat));
        when(chatMapper.toDto(chat, email)).thenReturn(chatDto);

        ChatDto result = chatService.getChat(email, 1L);

        assertNotNull(result);
        verify(chatRepository).findChatById(email, 1L);
    }

    @Test
    void shouldGetChatsByIds() {
        String email = "test@example.com";
        Set<Long> chatIds = Set.of(1L, 2L);
        List<Chat> chats = Arrays.asList(chat, groupChat);

        when(chatRepository.findChatsByIds(email, chatIds)).thenReturn(chats);

        List<Chat> result = chatService.getChats(email, chatIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatRepository).findChatsByIds(email, chatIds);
    }

    @Test
    void shouldBroadcastChatUpdate() {
        User user1 = User.builder().id(1L).email("user1@example.com").build();
        User user2 = User.builder().id(2L).email("user2@example.com").build();
        Member member1 = Member.builder().user(user1).chat(chat).build();
        Member member2 = Member.builder().user(user2).chat(chat).build();
        chat.setMembers(Set.of(member1, member2));

        ChatDto chatDto = new ChatDto();
        when(chatMapper.toDto(chat, "user1@example.com")).thenReturn(chatDto);
        when(chatMapper.toDto(chat, "user2@example.com")).thenReturn(chatDto);

        chatService.broadcastChatUpdate(chat);

        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.updated-chats", chatDto);
        verify(simpMessagingTemplate).convertAndSend("/topic/users.2.updated-chats", chatDto);
    }

    @Test
    void shouldBroadcastChatDelete() {
        chatService.broadcastChatDelete(user, chat);

        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.deleted-chats", 1L);
    }

    @Test
    void shouldBroadcastCreatedChat() {
        ChatDto chatDto = new ChatDto();
        when(chatMapper.toDto(chat, user.getEmail())).thenReturn(chatDto);

        chatService.broadcastCreatedChat(user, chat);

        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.created-chats", chatDto);
    }
}