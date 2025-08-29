package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class VoteRepositoryTest {

    @Autowired
    private VoteRepository voteRepository;

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

    private User user1;
    private User user2;
    private Chat chat;
    private PollMessage pollMessage;
    private Option option1;
    private Option option2;
    private Vote vote1;
    private Vote vote2;

    @BeforeEach
    public void setup() {
        voteRepository.deleteAll();
        optionRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

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

        chat = chatRepository.save(Chat.builder()
                .chatType(ChatType.GROUP)
                .build());

        memberRepository.save(Member.builder()
                .user(user1)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        memberRepository.save(Member.builder()
                .user(user2)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        pollMessage = messageRepository.save(PollMessage.builder()
                .chat(chat)
                .user(user1)
                .messageType(MessageType.POLL)
                .title("Test poll title")
                .build());

        option1 = optionRepository.save(Option.builder()
                .pollMessage(pollMessage)
                .title("Option 1")
                .build());

        option2 = optionRepository.save(Option.builder()
                .pollMessage(pollMessage)
                .title("Option 2")
                .build());

        vote1 = voteRepository.save(Vote.builder()
                .user(user1)
                .option(option1)
                .build());

        vote2 = voteRepository.save(Vote.builder()
                .user(user2)
                .option(option2)
                .build());
    }

    @Test
    void deleteByOptionPollMessageIdAndUserEmail_ShouldDeleteVotes() {
        assertTrue(voteRepository.existsById(vote1.getId()));

        voteRepository.deleteByOptionPollMessageIdAndUserEmail(pollMessage.getId(), user1.getEmail());

        assertFalse(voteRepository.existsById(vote1.getId()));
        assertTrue(voteRepository.existsById(vote2.getId()));
    }

    @Test
    void deleteByOptionPollMessageIdAndUserEmail_ShouldDoNothing_WhenNoMatchingVotes() {
        long initialCount = voteRepository.count();

        voteRepository.deleteByOptionPollMessageIdAndUserEmail(999L, "nonexistent@example.com");

        assertEquals(initialCount, voteRepository.count());
    }

    @Test
    void existsByOptionPollMessageIdAndUserEmail_ShouldReturnTrue_WhenVoteExists() {
        boolean exists = voteRepository.existsByOptionPollMessageIdAndUserEmail(pollMessage.getId(), user1.getEmail());

        assertTrue(exists);
    }

    @Test
    void existsByOptionPollMessageIdAndUserEmail_ShouldReturnFalse_WhenVoteDoesNotExist() {
        boolean exists = voteRepository.existsByOptionPollMessageIdAndUserEmail(pollMessage.getId(), "nonexistent@example.com");
        assertFalse(exists);
    }

    @Test
    void existsByOptionPollMessageIdAndUserEmail_ShouldReturnFalse_WhenMessageDoesNotExist() {
        boolean exists = voteRepository.existsByOptionPollMessageIdAndUserEmail(999L, user1.getEmail());
        assertFalse(exists);
    }

    @Test
    void shouldPreventDuplicateVotes_ForSameUserAndOption() {
        Vote duplicateVote = Vote.builder()
                .user(user1)
                .option(option1)
                .build();

        assertThrows(Exception.class, () -> voteRepository.saveAndFlush(duplicateVote));
    }

    @Test
    void shouldAllowVotes_ForSameUserDifferentOptions() {
        Vote newVote = Vote.builder()
                .user(user1)
                .option(option2)
                .build();

        Vote savedVote = voteRepository.save(newVote);

        assertNotNull(savedVote.getId());
        assertEquals(user1, savedVote.getUser());
        assertEquals(option2, savedVote.getOption());
    }

    @Test
    void shouldAllowVotes_ForDifferentUsersSameOption() {
        Vote newVote = Vote.builder()
                .user(user2)
                .option(option1)
                .build();

        Vote savedVote = voteRepository.save(newVote);

        assertNotNull(savedVote.getId());
        assertEquals(user2, savedVote.getUser());
        assertEquals(option1, savedVote.getOption());
    }
}