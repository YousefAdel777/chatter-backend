package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.InviteDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.InviteMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.InviteRepository;
import com.chatter.chatter.request.InvitePostRequest;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.InviteService;
import com.chatter.chatter.service.MemberService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InviteServiceTests {

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private ChatService chatService;

    @Mock
    private InviteMapper inviteMapper;

    @InjectMocks
    private InviteService inviteService;

    private GroupChat groupChat;

    private Invite linkInvite;

    private Invite expiredInvite;

    private Invite messageInvite;

    private User user;

    private Member member;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testUsername")
                .password("testPassword")
                .build();
        groupChat = GroupChat.builder()
                .chatType(ChatType.GROUP)
                .id(1L)
                .name("test group")
                .description("test description")
                .image("test.jpg")
                .build();
        member = Member.builder()
                .id(1L)
                .user(user)
                .chat(groupChat)
                .build();
        linkInvite = Invite.builder()
                .id(1L)
                .canUseLink(true)
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .groupChat(groupChat)
                .build();
        expiredInvite = Invite.builder()
                .id(2L)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(30)))
                .groupChat(groupChat)
                .build();
        messageInvite = Invite.builder()
                .id(1L)
                .canUseLink(false)
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .groupChat(groupChat)
                .build();
    }


    @Test
    void createInvite_ShouldCreateInvite_WhenValidRequest() {
        InvitePostRequest request = new InvitePostRequest(1L, Instant.now().plus(Duration.ofDays(1)), true);

        when(chatService.getChatEntityIfMember(user.getEmail(), request.getInviteChatId())).thenReturn(groupChat);
        when(memberService.isAdmin(user.getEmail(), groupChat.getId())).thenReturn(true);
        when(inviteRepository.save(any(Invite.class))).thenReturn(linkInvite);

        Invite result = inviteService.createInvite(user.getEmail(), request);

        assertNotNull(result);
        verify(inviteRepository).save(any(Invite.class));
    }

    @Test
    void createInvite_ShouldThrow_WhenNotGroupChat() {
        Chat individualChat = Chat.builder().chatType(ChatType.INDIVIDUAL).build();
        InvitePostRequest request = new InvitePostRequest(1L, Instant.now().plus(Duration.ofDays(1)), true);

        when(chatService.getChatEntityIfMember(user.getEmail(), request.getInviteChatId())).thenReturn(individualChat);

        assertThrows(BadRequestException.class, () -> inviteService.createInvite(user.getEmail(), request));
    }

    @Test
    void createInvite_ShouldThrow_WhenOnlyAdminsCanInvite_AndUserNotAdmin() {
        groupChat.setOnlyAdminsCanInvite(true);
        InvitePostRequest request = new InvitePostRequest(1L, Instant.now().plus(Duration.ofDays(1)), true);

        when(chatService.getChatEntityIfMember(user.getEmail(), request.getInviteChatId())).thenReturn(groupChat);
        when(memberService.isAdmin(user.getEmail(), groupChat.getId())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> inviteService.createInvite(user.getEmail(), request));
    }

    @Test
    void createInvite_ShouldSucceed_WhenOnlyAdminsCanInvite_AndUserIsAdmin() {
        groupChat.setOnlyAdminsCanInvite(true);
        InvitePostRequest request = new InvitePostRequest(1L, Instant.now().plus(Duration.ofDays(1)), true);

        when(chatService.getChatEntityIfMember(user.getEmail(), request.getInviteChatId())).thenReturn(groupChat);
        when(memberService.isAdmin(user.getEmail(), groupChat.getId())).thenReturn(true);
        when(inviteRepository.save(any(Invite.class))).thenReturn(linkInvite);

        Invite result = inviteService.createInvite(user.getEmail(), request);

        assertNotNull(result);
        verify(inviteRepository).save(any(Invite.class));
    }


@Test
    void getInviteEntity_ShouldReturnEntity_WhenFound() {
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.of(linkInvite));

        Invite result = inviteService.getInviteEntity(linkInvite.getId());

        assertEquals(linkInvite.getId(), result.getId());
        verify(inviteRepository).findById(linkInvite.getId());
    }

    @Test
    void getInviteEntity_ShouldThrowNotFoundException_WhenNotFound() {
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> inviteService.getInviteEntity(linkInvite.getId()));
    }

    @Test
    void getInvite_ShouldReturnDto_WhenFound() {
        InviteDto dto = new InviteDto();
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.of(linkInvite));
        when(inviteMapper.toDto(eq(linkInvite))).thenReturn(dto);

        InviteDto result = inviteService.getInvite(linkInvite.getId());

        assertEquals(dto, result);
        verify(inviteRepository).findById(linkInvite.getId());
    }

    @Test
    void getInvite_ShouldThrowNotFoundException_WhenNotFound() {
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> inviteService.getInvite(linkInvite.getId()));
    }

    @Test
    void acceptInvite_ShouldSucceed_WhenFound_AndNotExpired() {
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.of(linkInvite));
        when(memberService.createMember(eq(user.getEmail()), eq(groupChat), eq(MemberRole.MEMBER))).thenReturn(member);

        inviteService.acceptInvite(user.getEmail(), linkInvite.getId(), false);
        verify(chatService).broadcastCreatedChat(eq(member.getUser()), eq(groupChat));
    }

    @Test
    void acceptInvite_ShouldSucceed_WhenMessageInvite_AndNotExpired() {
        when(inviteRepository.findById(messageInvite.getId())).thenReturn(Optional.of(messageInvite));
        when(memberService.createMember(eq(user.getEmail()), eq(groupChat), eq(MemberRole.MEMBER))).thenReturn(member);

        inviteService.acceptInvite(user.getEmail(), messageInvite.getId(), true);

        verify(chatService).broadcastCreatedChat(eq(member.getUser()), eq(groupChat));
    }

    @Test
    void acceptInvite_ShouldThrowNotFoundException_WhenNotFound() {
        when(inviteRepository.findById(linkInvite.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> inviteService.acceptInvite(user.getEmail(), linkInvite.getId(), false));
    }

    @Test
    void acceptInvite_ShouldThrowBadRequestException_WhenExpired() {
        when(inviteRepository.findById(expiredInvite.getId())).thenReturn(Optional.of(expiredInvite));
        assertThrows(BadRequestException.class, () -> inviteService.acceptInvite(user.getEmail(), expiredInvite.getId(), false));
    }

    @Test
    void acceptInvite_ShouldThrowBadRequestException_WhenNotFromMessage_AndCannotUseLink() {
        when(inviteRepository.findById(messageInvite.getId())).thenReturn(Optional.of(messageInvite));
        assertThrows(BadRequestException.class, () -> inviteService.acceptInvite(user.getEmail(), messageInvite.getId(), false));
    }

}
