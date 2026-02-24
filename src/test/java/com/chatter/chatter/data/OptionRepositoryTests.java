package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class OptionRepositoryTests {

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private com.chatter.chatter.repository.VoteRepository voteRepository;

    private User user;
    private User otherUser;
    private Chat chat;
    private PollMessage pollMessage;
    private Option option1;
    private Option option2;
    private Option option3;

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
                .username("testUser")
                .password("testPassword")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .username("testUser2")
                .password("testPassword")
                .build());

        chat = chatRepository.save(Chat.builder()
                .chatType(ChatType.GROUP)
                .build());

        memberRepository.save(Member.builder()
                .user(user)
                .chat(chat)
                .memberRole(MemberRole.MEMBER)
                .build());

        pollMessage = messageRepository.save(PollMessage.builder()
                .chat(chat)
                .user(user)
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

        option3 = optionRepository.save(Option.builder()
                .pollMessage(pollMessage)
                .title("Option 3")
                .build());
    }

    @Test
    void shouldSaveOption_WhenValid() {
        Option newOption = Option.builder()
                .pollMessage(pollMessage)
                .title("New Option")
                .build();

        Option savedOption = optionRepository.save(newOption);

        assertNotNull(savedOption.getId());
        assertEquals("New Option", savedOption.getTitle());
        assertEquals(pollMessage, savedOption.getPollMessage());
        assertNotNull(savedOption.getVotes());
        assertTrue(savedOption.getVotes().isEmpty());
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnOptions_WhenUserHasNotVoted() {
        List<Option> result = optionRepository.findOptionsWithoutVotes(
                user.getId(),
                List.of(option1.getId(), option2.getId(), option3.getId())
        );

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(option1.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(option2.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(option3.getId())));
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnEmptyList_WhenUserHasVotedOnAll() {
        voteRepository.save(Vote.builder()
                .user(user)
                .option(option1)
                .build());

        voteRepository.save(Vote.builder()
                .user(user)
                .option(option2)
                .build());

        voteRepository.save(Vote.builder()
                .user(user)
                .option(option3)
                .build());

        List<Option> result = optionRepository.findOptionsWithoutVotes(
                user.getId(),
                List.of(option1.getId(), option2.getId(), option3.getId())
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnOnlyUnvotedOptions_WhenUserHasVotedOnSome() {
        voteRepository.save(Vote.builder()
                .user(user)
                .option(option1)
                .build());

        List<Option> result = optionRepository.findOptionsWithoutVotes(
                user.getId(),
                List.of(option1.getId(), option2.getId(), option3.getId())
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(option2.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(option3.getId())));
        assertFalse(result.stream().anyMatch(o -> o.getId().equals(option1.getId())));
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnEmptyList_WhenUserNotMemberOfChat() {
        List<Option> result = optionRepository.findOptionsWithoutVotes(
                otherUser.getId(),
                List.of(option1.getId(), option2.getId(), option3.getId())
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnEmptyList_WhenOptionsIdsEmpty() {
        List<Option> result = optionRepository.findOptionsWithoutVotes(
                user.getId(),
                List.of()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findOptionsWithoutVotes_ShouldReturnEmptyList_WhenOptionsNotExist() {
        List<Option> result = optionRepository.findOptionsWithoutVotes(user.getId(), List.of(999L, 1000L));
        assertTrue(result.isEmpty());
    }
}