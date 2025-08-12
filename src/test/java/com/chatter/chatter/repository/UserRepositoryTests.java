package com.chatter.chatter.repository;

import com.chatter.chatter.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void shouldSaveUser() {
        String username = "testUsername";
        String email = "testEmail@g.com";
        String password = "testPassword";
        User user = User.builder()
                .username(username)
                .password(password)
                .email(email)
                .build();

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals(savedUser.getUsername(), username);
        assertEquals(savedUser.getEmail(), email);
        assertEquals(savedUser.getPassword(), password);
        assertEquals(savedUser.getBio(), "");
        assertTrue(savedUser.getShowMessageReads());
        assertTrue(savedUser.getShowOnlineStatus());
        assertNull(savedUser.getLastOnline());
    }

    @Test
    void shouldNotAllowDuplicateEmails() {
        User user1 = User.builder()
                .username("test_username")
                .email("test1@example.com")
                .password("password")
                .build();

        User user2 = User.builder()
                .username("test_username")
                .email("test1@example.com")
                .password("password")
                .build();

        userRepository.save(user1);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void shouldFindUserByEmail() {
        User user = User.builder()
                .username("test_username")
                .email("test@example.com")
                .password("password")
                .build();

        userRepository.save(user);
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        assertThat(foundUser.isPresent()).isTrue();
        assertThat(foundUser.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldCheckIfEmailExists() {
        User user = User.builder()
                .username("test_username")
                .email("test@example.com")
                .password("password")
                .build();

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("test@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    public void shouldReturnFalseIfNoUserFoundWithEmail() {
        boolean exists = userRepository.existsByEmail("test@example.com");
        assertThat(exists).isFalse();
    }

    @Test
    public void testFindContacts() {
        User alice = userRepository.save(User.builder().username("test_user1").email("test1@example.com").password("pass").build());
        User bob = userRepository.save(User.builder().username("test_user2").email("test2@example.com").password("pass").build());
        User charlie = userRepository.save(User.builder().username("test_user3").email("test3@example.com").password("pass").build());

        List<User> contacts = userRepository.findContacts("test1@example.com", ChatType.INDIVIDUAL);
        assertThat(contacts).isEmpty();

        Chat chat1 = Chat.builder().chatType(ChatType.INDIVIDUAL).build();
        chatRepository.save(chat1);

        Member m1 = Member.builder().user(alice).chat(chat1).memberRole(MemberRole.MEMBER).build();
        Member m2 = Member.builder().user(bob).chat(chat1).memberRole(MemberRole.MEMBER).build();
        memberRepository.saveAll(List.of(m1, m2));

        Chat chat2 = Chat.builder().chatType(ChatType.INDIVIDUAL).build();
        chatRepository.save(chat2);

        Member m3 = Member.builder().user(alice).chat(chat2).memberRole(MemberRole.MEMBER).build();
        Member m4 = Member.builder().user(charlie).chat(chat2).memberRole(MemberRole.MEMBER).build();
        memberRepository.saveAll(List.of(m3, m4));

        contacts = userRepository.findContacts("test1@example.com", ChatType.INDIVIDUAL);

        assertEquals(2, contacts.size());
        assertTrue(contacts.contains(bob));
        assertTrue(contacts.contains(charlie));


    }

}
