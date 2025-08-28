package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.MemberPatchRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.service.MemberService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private Cache cache;

    @InjectMocks
    private MemberService memberService;

    private Chat groupChat;
    private Chat individualChat;
    private User testUser;
    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        groupChat = Chat.builder().id(1L).chatType(ChatType.GROUP).build();
        individualChat = Chat.builder().id(2L).chatType(ChatType.INDIVIDUAL).build();
        testUser = User.builder().id(1L).email("test@example.com").build();
        adminUser = User.builder().id(2L).email("admin@example.com").build();
        memberUser = User.builder().id(3L).email("member@example.com").build();
    }

    @Test
    void createMemberEntity_WithUserId_ShouldCreateMember_WhenNotAlreadyExists() {
        when(memberRepository.existsByChatIdAndUserId(groupChat.getId(), testUser.getId())).thenReturn(false);
        when(userService.getUserEntity(testUser.getId())).thenReturn(testUser);

        Member result = memberService.createMemberEntity(testUser.getId(), groupChat, MemberRole.MEMBER);

        assertNotNull(result);
        assertEquals(groupChat, result.getChat());
        assertEquals(testUser, result.getUser());
        assertEquals(MemberRole.MEMBER, result.getMemberRole());
    }

    @Test
    void createMemberEntity_WithUserId_ShouldThrow_WhenAlreadyMember() {
        when(memberRepository.existsByChatIdAndUserId(groupChat.getId(), testUser.getId())).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                memberService.createMemberEntity(testUser.getId(), groupChat, MemberRole.MEMBER));
    }

    @Test
    void createMemberEntity_WithEmail_ShouldCreateMember_WhenNotAlreadyExists() {
        when(memberRepository.existsByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(false);
        when(userService.getUserEntityByEmail(testUser.getEmail())).thenReturn(testUser);

        Member result = memberService.createMemberEntity(testUser.getEmail(), groupChat, MemberRole.ADMIN);

        assertNotNull(result);
        assertEquals(groupChat, result.getChat());
        assertEquals(testUser, result.getUser());
        assertEquals(MemberRole.ADMIN, result.getMemberRole());
    }

    @Test
    void createMemberEntity_WithEmail_ShouldThrow_WhenAlreadyMember() {
        when(memberRepository.existsByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                memberService.createMemberEntity(testUser.getEmail(), groupChat, MemberRole.MEMBER));
    }

    @Test
    void createMember_ShouldSaveMember() {
        when(memberRepository.existsByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(false);
        when(userService.getUserEntityByEmail(testUser.getEmail())).thenReturn(testUser);
        when(memberRepository.save(any(Member.class))).thenReturn(Member.builder().chat(groupChat).user(testUser).build());

        Member result = memberService.createMember(testUser.getEmail(), groupChat, MemberRole.MEMBER);

        assertNotNull(result);
        verify(memberRepository).save(any(Member.class));
        verify(userService, never()).evictCurrentUserContactsCache(anyString());
    }

    @Test
    void createMember_ShouldEvictContactsCache_ForIndividualChat() {
        when(memberRepository.existsByChatIdAndUserEmail(individualChat.getId(), testUser.getEmail())).thenReturn(false);
        when(userService.getUserEntityByEmail(testUser.getEmail())).thenReturn(testUser);
        when(memberRepository.save(any(Member.class))).thenReturn(Member.builder().chat(individualChat).user(testUser).build());

        memberService.createMember(testUser.getEmail(), individualChat, MemberRole.MEMBER);

        verify(userService).evictCurrentUserContactsCache(testUser.getEmail());
    }

    @Test
    void updateMember_ShouldUpdateRole_WhenAdminAndValidConditions() {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);
        Member member = Member.builder().id(1L).chat(groupChat).user(memberUser).memberRole(MemberRole.MEMBER).build();
        Member admin = Member.builder().chat(groupChat).user(adminUser).memberRole(MemberRole.ADMIN).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), adminUser.getEmail())).thenReturn(Optional.of(admin));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        Member result = memberService.updateMember(adminUser.getEmail(), 1L, request);

        assertEquals(MemberRole.ADMIN, result.getMemberRole());
        verify(memberRepository).save(member);
    }

    @Test
    void updateMember_ShouldThrow_WhenNotAdmin() {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);
        Member member = Member.builder().id(1L).chat(groupChat).user(memberUser).build();
        Member nonAdmin = Member.builder().chat(groupChat).user(testUser).memberRole(MemberRole.MEMBER).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(Optional.of(nonAdmin));

        assertThrows(ForbiddenException.class, () ->
                memberService.updateMember(testUser.getEmail(), 1L, request));
    }

    @Test
    void updateMember_ShouldThrow_WhenTryingToChangeOwnerRole() {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.ADMIN);
        Member owner = Member.builder().id(1L).chat(groupChat).user(testUser).memberRole(MemberRole.OWNER).build();
        Member admin = Member.builder().chat(groupChat).user(adminUser).memberRole(MemberRole.ADMIN).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), adminUser.getEmail())).thenReturn(Optional.of(admin));

        assertThrows(ForbiddenException.class, () ->
                memberService.updateMember(adminUser.getEmail(), 1L, request));
    }

    @Test
    void updateMember_ShouldThrow_WhenAdminTriesToChangeOtherAdmin() {
        MemberPatchRequest request = new MemberPatchRequest(MemberRole.MEMBER);
        Member targetAdmin = Member.builder().id(1L).chat(groupChat).user(memberUser).memberRole(MemberRole.ADMIN).build();
        Member requestingAdmin = Member.builder().chat(groupChat).user(adminUser).memberRole(MemberRole.ADMIN).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetAdmin));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), adminUser.getEmail())).thenReturn(Optional.of(requestingAdmin));

        assertThrows(ForbiddenException.class, () ->
                memberService.updateMember(adminUser.getEmail(), 1L, request));
    }

    @Test
    void deleteMember_ShouldDelete_WhenSelfRemoval() {
        Member member = Member.builder().id(1L).chat(groupChat).user(testUser).memberRole(MemberRole.MEMBER).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.deleteMember(testUser.getEmail(), 1L);

        verify(memberRepository).delete(member);
        verify(simpMessagingTemplate).convertAndSend("/topic/users.1.deleted-chats", groupChat.getId());
    }

    @Test
    void deleteMember_ShouldDelete_WhenAdminRemovesOther() {
        Member member = Member.builder().id(1L).chat(groupChat).user(memberUser).memberRole(MemberRole.MEMBER).build();
        Member admin = Member.builder().chat(groupChat).user(adminUser).memberRole(MemberRole.ADMIN).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), adminUser.getEmail())).thenReturn(Optional.of(admin));

        memberService.deleteMember(adminUser.getEmail(), 1L);

        verify(memberRepository).delete(member);
    }

    @Test
    void deleteMember_ShouldThrow_ForIndividualChat() {
        Member member = Member.builder().id(1L).chat(individualChat).user(testUser).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThrows(BadRequestException.class, () ->
                memberService.deleteMember(testUser.getEmail(), 1L));
    }

    @Test
    void deleteMember_ShouldThrow_WhenNonAdminTriesToRemoveOther() {
        Member member = Member.builder().id(1L).chat(groupChat).user(memberUser).memberRole(MemberRole.MEMBER).build();
        Member nonAdmin = Member.builder().chat(groupChat).user(testUser).memberRole(MemberRole.MEMBER).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(Optional.of(nonAdmin));

        assertThrows(ForbiddenException.class, () ->
                memberService.deleteMember(testUser.getEmail(), 1L));
    }

    @Test
    void getMemberEntityById_ShouldReturnMember_WhenExists() {
        Member member = Member.builder().id(1L).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Member result = memberService.getMemberEntityById(1L);

        assertEquals(member, result);
    }

    @Test
    void getMemberEntityById_ShouldThrow_WhenNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                memberService.getMemberEntityById(1L));
    }

    @Test
    void getCurrentChatMemberEntity_ShouldReturnMember_WhenExists() {
        Member member = Member.builder().build();

        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail()))
                .thenReturn(Optional.of(member));

        Member result = memberService.getCurrentChatMemberEntity(testUser.getEmail(), groupChat.getId());

        assertEquals(member, result);
    }

    @Test
    void getCurrentChatMemberEntity_ShouldThrow_WhenNotFound() {
        when(memberRepository.findByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                memberService.getCurrentChatMemberEntity(testUser.getEmail(), groupChat.getId()));
    }

    @Test
    void isMember_ShouldReturnTrue_WhenMemberExists() {
        when(memberRepository.existsByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(true);

        boolean result = memberService.isMember(testUser.getEmail(), groupChat.getId());

        assertTrue(result);
    }

    @Test
    void isMember_ShouldReturnFalse_WhenMemberNotExists() {
        when(memberRepository.existsByChatIdAndUserEmail(groupChat.getId(), testUser.getEmail())).thenReturn(false);
        boolean result = memberService.isMember(testUser.getEmail(), groupChat.getId());
        assertFalse(result);
    }

    @Test
    void isAdmin_ShouldReturnTrue_ForAdminRole() {
        when(memberRepository.existsByChatIdAndUserEmailAndMemberRole(groupChat.getId(), adminUser.getEmail(), MemberRole.OWNER))
                .thenReturn(false);
        when(memberRepository.existsByChatIdAndUserEmailAndMemberRole(groupChat.getId(), adminUser.getEmail(), MemberRole.ADMIN))
                .thenReturn(true);
        boolean result = memberService.isAdmin(adminUser.getEmail(), groupChat.getId());
        assertTrue(result);
    }

    @Test
    void isAdmin_ShouldReturnTrue_ForOwnerRole() {
        when(memberRepository.existsByChatIdAndUserEmailAndMemberRole(groupChat.getId(), adminUser.getEmail(), MemberRole.OWNER))
                .thenReturn(true);

        boolean result = memberService.isAdmin(adminUser.getEmail(), groupChat.getId());

        assertTrue(result);
    }

    @Test
    void isAdmin_ShouldReturnFalse_ForMemberRole() {
        when(memberRepository.existsByChatIdAndUserEmailAndMemberRole(groupChat.getId(), testUser.getEmail(), MemberRole.ADMIN))
                .thenReturn(false);
        when(memberRepository.existsByChatIdAndUserEmailAndMemberRole(groupChat.getId(), testUser.getEmail(), MemberRole.OWNER))
                .thenReturn(false);

        boolean result = memberService.isAdmin(testUser.getEmail(), groupChat.getId());

        assertFalse(result);
    }

    @Test
    void replaceOwner_ShouldNotReplace_ForIndividualChat() {
        Member owner = Member.builder().chat(individualChat).memberRole(MemberRole.OWNER).build();

        memberService.replaceOwner(owner);

        verify(memberRepository, never()).findFirstMemberExcludingMember(any(), any(), any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void replaceOwner_ShouldNotReplace_WhenNotOwner() {
        Member nonOwner = Member.builder().chat(groupChat).memberRole(MemberRole.ADMIN).build();

        memberService.replaceOwner(nonOwner);

        verify(memberRepository, never()).findFirstMemberExcludingMember(any(), any(), any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void replaceOwner_ShouldPromoteAdmin_WhenAdminExists() {
        Member owner = Member.builder().user(memberUser).id(1L).chat(groupChat).memberRole(MemberRole.OWNER).build();
        Member admin = Member.builder().user(adminUser).id(2L).chat(groupChat).memberRole(MemberRole.ADMIN).build();

        when(memberRepository.findFirstMemberExcludingMember(groupChat.getId(), owner.getId(), MemberRole.ADMIN))
                .thenReturn(Optional.of(admin));

        memberService.replaceOwner(owner);

        assertEquals(MemberRole.OWNER, admin.getMemberRole());
        verify(memberRepository).save(admin);
    }

    @Test
    void replaceOwner_ShouldPromoteMember_WhenNoAdminExists() {
        Member owner = Member.builder().user(memberUser).id(1L).chat(groupChat).memberRole(MemberRole.OWNER).build();
        Member member = Member.builder().user(testUser).id(2L).chat(groupChat).memberRole(MemberRole.MEMBER).build();

        when(memberRepository.findFirstMemberExcludingMember(groupChat.getId(), owner.getId(), MemberRole.ADMIN))
                .thenReturn(Optional.empty());
        when(memberRepository.findFirstMemberExcludingMember(groupChat.getId(), owner.getId(), MemberRole.MEMBER))
                .thenReturn(Optional.of(member));

        memberService.replaceOwner(owner);

        assertEquals(MemberRole.OWNER, member.getMemberRole());
        verify(memberRepository).save(member);
    }

}