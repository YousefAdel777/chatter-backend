package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.ChatDto;
import com.chatter.chatter.dto.ChatStatusProjection;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.ChatMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.request.GroupChatPatchRequest;
import com.chatter.chatter.request.GroupChatPostRequest;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.FileValidationService;
import com.chatter.chatter.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
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
    private FileValidationService fileValidationService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cursor<String> cursor;

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
        ReflectionTestUtils.setField(chatService, "maxImageSize", 1024L);
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
    void shouldCreateGroupChatWithImage() {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setGroupImage(multipartFile);

        when(fileValidationService.isImage(multipartFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(eq(multipartFile), any())).thenReturn(true);
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

        when(fileValidationService.isImage(multipartFile)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> chatService.createGroupChat(email, request));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void shouldThrowBadRequest_WhenOversizedImageInGroupChatCreation() {
        String email = "test@example.com";
        GroupChatPostRequest request = new GroupChatPostRequest();
        request.setName("Test Group");
        request.setGroupImage(multipartFile);

        when(fileValidationService.isImage(multipartFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(eq(multipartFile), any())).thenReturn(false);

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
        when(fileValidationService.isImage(multipartFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(eq(multipartFile), any())).thenReturn(true);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("/new/image/path.jpg");
        when(chatRepository.save(any(GroupChat.class))).thenReturn(groupChat);

        Chat result = chatService.updateGroupChat(email, 2L, request);

        assertNotNull(result);
        verify(fileUploadService).uploadFile(multipartFile);
    }

    @Test
    void shouldThrowBadRequest_WhenOversizedImageInGroupChatUpdate() {
        String email = "test@example.com";
        GroupChatPatchRequest request = new GroupChatPatchRequest();
        request.setGroupImage(multipartFile);

        when(chatRepository.findById(2L)).thenReturn(Optional.of(groupChat));
        when(memberService.getCurrentChatMemberEntity(email, 2L)).thenReturn(member);
        when(fileValidationService.isImage(multipartFile)).thenReturn(true);
        when(fileValidationService.isSizeValid(eq(multipartFile), any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> chatService.updateGroupChat(email, 2L, request));
        verify(chatRepository, never()).save(any());
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

        assertThrows(ForbiddenException.class, () -> chatService.deleteGroupChat(email, 2L));
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

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        chatService.evictChatCache(chat);

        verify(redisTemplate, times(2)).scan(any(ScanOptions.class));
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
        List<ChatStatusProjection> projections = Arrays.asList(mock(ChatStatusProjection.class), mock(ChatStatusProjection.class));
        List<ChatDto> chatDtos = Arrays.asList(new ChatDto(), new ChatDto());

        when(chatRepository.findAll(any(Specification.class))).thenReturn(chats);
        when(chatRepository.findChatStatus(Set.of(userEmail), Set.of(1L, 2L))).thenReturn(projections);
        when(chatMapper.toDtoList(chats, projections, userEmail)).thenReturn(chatDtos);

        List<ChatDto> result = chatService.getAllChatsByEmail(userEmail, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatRepository).findAll(any(Specification.class));
    }

    @Test
    void shouldGetChat() {
        String email = "test@example.com";
        ChatDto chatDto = new ChatDto();
        ChatStatusProjection projection = mock(ChatStatusProjection.class);

        when(chatRepository.findChatById(email, 1L)).thenReturn(Optional.of(chat));
        when(chatRepository.findChatStatus(Set.of(email), Set.of(1L))).thenReturn(List.of(projection));
        when(chatMapper.toDto(chat, projection, email)).thenReturn(chatDto);

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

        ChatStatusProjection projection1 = mock(ChatStatusProjection.class);
        ChatStatusProjection projection2 = mock(ChatStatusProjection.class);
        when(projection1.getUserId()).thenReturn(1L);
        when(projection1.getUserEmail()).thenReturn("user1@example.com");
        when(projection2.getUserId()).thenReturn(2L);
        when(projection2.getUserEmail()).thenReturn("user2@example.com");

        ChatDto chatDto1 = new ChatDto();
        ChatDto chatDto2 = new ChatDto();

        when(memberService.getMembersEntitiesByChat(chat.getId(), null, null)).thenReturn(Arrays.asList(member1, member2));
        when(chatRepository.findChatStatus(Set.of("user1@example.com", "user2@example.com"), Set.of(chat.getId()))).thenReturn(Arrays.asList(projection1, projection2));
        when(chatMapper.toDto(chat, projection1, "user1@example.com")).thenReturn(chatDto1);
        when(chatMapper.toDto(chat, projection2, "user2@example.com")).thenReturn(chatDto2);

        chatService.broadcastChatUpdate(chat);

        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.updated-chats", chatDto1);
        verify(simpMessagingTemplate).convertAndSend("/topic/users.2.updated-chats", chatDto2);
    }

    @Test
    void shouldBroadcastChatDelete() {
        chatService.broadcastChatDelete(user, chat);

        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.deleted-chats", 1L);
    }

    @Test
    void shouldBroadcastCreatedChat() {
        ChatStatusProjection projection = mock(ChatStatusProjection.class);
        ChatDto chatDto = new ChatDto();

        when(chatRepository.findChatStatus(Set.of(user.getEmail()), Set.of(chat.getId()))).thenReturn(List.of(projection));
        when(chatMapper.toDto(chat, projection, user.getEmail())).thenReturn(chatDto);

        chatService.broadcastCreatedChat(user, chat);
        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.created-chats", chatDto);
    }

    @Test
    void shouldGetChatStatusProjections() {
        Set<String> emails = Set.of("test@example.com");
        Set<Long> chatIds = Set.of(1L, 2L);
        List<ChatStatusProjection> projections = Arrays.asList(mock(ChatStatusProjection.class), mock(ChatStatusProjection.class));

        when(chatRepository.findChatStatus(emails, chatIds)).thenReturn(projections);

        List<ChatStatusProjection> result = chatService.getChatStatusProjections(emails, chatIds);

        assertEquals(projections, result);
    }
}