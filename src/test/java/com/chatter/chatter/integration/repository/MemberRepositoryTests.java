package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.MemberRepository;
import com.chatter.chatter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User user2;
    private Chat chat;
    private Chat chat2;

    @BeforeEach
    public void setup() {
        user = userRepository.save(User.builder()
                .username("testUsername")
                .email("testEmail@example.com")
                .password("testPassword")
                .build());

        user2 = userRepository.save(User.builder()
                .username("testUsername2")
                .email("testEmail2@example.com")
                .password("testPassword2")
                .build());

        chat = chatRepository.save(Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build());

        chat2 = chatRepository.save(Chat.builder()
                .chatType(ChatType.GROUP)
                .build());
    }

    @Test
    void shouldSaveMember_WhenValid() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .build();
        Member createdMember = memberRepository.save(member);

        assertNotNull(createdMember.getId());
        assertNotNull(createdMember.getJoinedAt());
        assertEquals(MemberRole.MEMBER, createdMember.getMemberRole());
        assertEquals(member.getUser().getId(), createdMember.getUser().getId());
        assertEquals(member.getChat().getId(), createdMember.getChat().getId());
    }

    @Test
    void shouldThrow_WhenDuplicateUserAndChat() {
        Member member1 = Member.builder()
                .chat(chat)
                .user(user)
                .build();
        memberRepository.save(member1);

        Member member2 = Member.builder()
                .chat(chat)
                .user(user)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> memberRepository.save(member2));
    }

    @Test
    void shouldFindMemberByChatIdAndUserEmail_WhenExists() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .build();
        Member createdMember = memberRepository.save(member);

        Optional<Member> result = memberRepository.findByChatIdAndUserEmail(chat.getId(), user.getEmail());
        assertTrue(result.isPresent());
        assertEquals(createdMember.getId(), result.get().getId());
        assertNotNull(result.get().getUser());
        assertNotNull(result.get().getChat());
    }

    @Test
    void shouldNotFindMemberByChatIdAndUserEmail_WhenNotExists() {
        Optional<Member> result = memberRepository.findByChatIdAndUserEmail(999L, "nonexistent@example.com");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnTrue_WhenExistsByChatIdAndUserEmail_AndMemberExists() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .build();
        memberRepository.save(member);

        boolean exists = memberRepository.existsByChatIdAndUserEmail(chat.getId(), user.getEmail());
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByChatIdAndUserEmail_AndMemberNotExists() {
        boolean exists = memberRepository.existsByChatIdAndUserEmail(999L, "nonexistent@example.com");
        assertFalse(exists);
    }

    @Test
    void shouldReturnTrue_WhenExistsByChatIdAndUserEmailAndMemberRole_AndMatches() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.ADMIN)
                .build();
        memberRepository.save(member);

        boolean exists = memberRepository.existsByChatIdAndUserEmailAndMemberRole(
                chat.getId(), user.getEmail(), MemberRole.ADMIN);
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByChatIdAndUserEmailAndMemberRole_AndRoleDifferent() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();
        memberRepository.save(member);

        boolean exists = memberRepository.existsByChatIdAndUserEmailAndMemberRole(
                chat.getId(), user.getEmail(), MemberRole.ADMIN);
        assertFalse(exists);
    }

    @Test
    void shouldReturnTrue_WhenExistsByChatIdAndUserId_AndMemberExists() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .build();
        memberRepository.save(member);

        boolean exists = memberRepository.existsByChatIdAndUserId(chat.getId(), user.getId());
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByChatIdAndUserId_AndMemberNotExists() {
        boolean exists = memberRepository.existsByChatIdAndUserId(999L, 999L);
        assertFalse(exists);
    }

    @Test
    void shouldFindFirstMemberExcludingMember_WhenAdminExists() {
        Member member1 = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.OWNER)
                .build();
        memberRepository.save(member1);

        Member member2 = Member.builder()
                .user(user2)
                .chat(chat)
                .memberRole(MemberRole.ADMIN)
                .build();
        memberRepository.save(member2);

        Member member3 = Member.builder()
                .user(userRepository.save(User.builder()
                        .username("user3")
                        .email("user3@example.com")
                        .password("pass3")
                        .build()))
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();
        memberRepository.save(member3);

        Optional<Member> result = memberRepository.findFirstMemberExcludingMember(chat.getId(), member1.getId(), MemberRole.ADMIN);
        assertTrue(result.isPresent());
        assertEquals(member2.getId(), result.get().getId());
        assertEquals(MemberRole.ADMIN, result.get().getMemberRole());
    }

    @Test
    void shouldNotFindFirstMemberExcludingMember_WhenNoMatchingRole() {
        Member member1 = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.OWNER)
                .build();
        memberRepository.save(member1);

        Member member2 = Member.builder()
                .user(user2)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build();
        memberRepository.save(member2);

        Optional<Member> result = memberRepository.findFirstMemberExcludingMember(chat.getId(), member1.getId(), MemberRole.ADMIN);
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmpty_WhenFindFirstMemberExcludingMember_AndNoOtherMembers() {
        Member member = Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.OWNER)
                .build();
        memberRepository.save(member);

        Optional<Member> result = memberRepository.findFirstMemberExcludingMember(chat.getId(), member.getId(), MemberRole.ADMIN);
        assertFalse(result.isPresent());
    }
}