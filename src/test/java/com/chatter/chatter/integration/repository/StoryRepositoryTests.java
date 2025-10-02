package com.chatter.chatter.integration.repository;

import com.chatter.chatter.dto.StoryStatusProjection;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class StoryRepositoryTests {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoryViewRepository storyViewRepository;

    @Autowired
    private BlockRepository blockRepository;

    private User user1;
    private User user2;
    private User user3;
    private Instant cutoff;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        blockRepository.deleteAll();
        memberRepository.deleteAll();
        storyRepository.deleteAll();
        chatRepository.deleteAll();
        storyViewRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .email("test1@example.com")
                .username("testUsername1")
                .password("testPassword1")
                .build());

        user2 = userRepository.save(User.builder()
                .email("test2@example.com")
                .username("testUsername2")
                .password("testPassword2")
                .build());

        user3 = userRepository.save(User.builder()
                .email("test3@example.com")
                .username("testUsername3")
                .password("testPassword3")
                .build());

        Chat individualChat = Chat.builder()
                .chatType(ChatType.INDIVIDUAL)
                .build();

        Member member1 = Member.builder()
                .user(user1)
                .chat(individualChat)
                .build();

        Member member2 = Member.builder()
                .user(user2)
                .chat(individualChat)
                .build();

        individualChat.addMember(member1);
        individualChat.addMember(member2);
        chatRepository.save(individualChat);
        cutoff = Instant.now().minusSeconds(3600);
    }

    @Test
    void shouldSaveTextStory_WhenValid() {
        TextStory story = TextStory.builder()
                .user(user1)
                .storyType(StoryType.TEXT)
                .content("Test text story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now())
                .build();

        TextStory savedStory = storyRepository.save(story);

        assertNotNull(savedStory.getId());
        assertNotNull(savedStory.getCreatedAt());
        assertEquals("Test text story", savedStory.getContent());
        assertInstanceOf(TextStory.class, savedStory);
        assertEquals("#000000", savedStory.getTextColor());
        assertEquals("#FFFFFF", savedStory.getBackgroundColor());
    }

    @Test
    void shouldSaveMediaStory_WhenValid() {
        MediaStory story = MediaStory.builder()
                .user(user1)
                .storyType(StoryType.IMAGE)
                .content("Test media story")
                .filePath("test.jpg")
                .createdAt(Instant.now())
                .build();

        MediaStory savedStory = storyRepository.save(story);

        assertNotNull(savedStory.getId());
        assertEquals("Test media story", savedStory.getContent());
        assertInstanceOf(MediaStory.class, savedStory);
        assertEquals("test.jpg", (savedStory).getFilePath());
    }

    @Test
    void saveStory_WithExcludedUsers_ShouldPersistRelationships() {
        TextStory story = TextStory.builder()
                .user(user1)
                .storyType(StoryType.TEXT)
                .content("Story with excluded users")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now())
                .build();

        story.getExcludedUsers().add(user2);
        story.getExcludedUsers().add(user3);

        Story savedStory = storyRepository.save(story);
        Story foundStory = storyRepository.findById(savedStory.getId()).orElseThrow();

        assertEquals(2, foundStory.getExcludedUsers().size());
        assertTrue(foundStory.getExcludedUsers().stream()
                .anyMatch(user -> user.getEmail().equals(user2.getEmail())));
        assertTrue(foundStory.getExcludedUsers().stream()
                .anyMatch(user -> user.getEmail().equals(user3.getEmail())));
    }

    @Test
    void saveStory_WithNullRequiredFields_ShouldThrowException() {
        TextStory story = TextStory.builder()
                .storyType(StoryType.TEXT)
                .content("Invalid story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now())
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> storyRepository.saveAndFlush(story));
    }

    @Test
    void findStories_ShouldReturnStories_WhenUserHasAccess() {
        TextStory story = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Test story content")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        List<Story> result = storyRepository.findStories(
                user1.getEmail(),
                ChatType.INDIVIDUAL,
                cutoff
        );

        assertEquals(1, result.size());
        assertEquals(story.getId(), result.getFirst().getId());
        assertEquals("Test story content", result.getFirst().getContent());
    }

    @Test
    void findStories_ShouldNotReturnStories_WhenUserIsBlocked() {
        storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Blocked story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);

        List<Story> result = storyRepository.findStories(
                user1.getEmail(),
                ChatType.INDIVIDUAL,
                cutoff
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findStories_ShouldNotReturnStories_WhenUserIsExcluded() {
        TextStory story = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Excluded story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        story.getExcludedUsers().add(user1);
        storyRepository.save(story);

        List<Story> result = storyRepository.findStories(
                user1.getEmail(),
                ChatType.INDIVIDUAL,
                cutoff
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findStoryStatus_ShouldReturnCorrectStatus() {
        TextStory story1 = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Story 1")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        TextStory story2 = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Story 2")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        storyViewRepository.save(StoryView.builder()
                .story(story1)
                .user(user1)
                .createdAt(Instant.now())
                .build());

        List<Long> storyIds = List.of(story1.getId(), story2.getId());
        List<StoryStatusProjection> result = storyRepository.findStoryStatus(user1.getEmail(), storyIds);

        assertEquals(2, result.size());

        StoryStatusProjection status1 = findStatusById(result, story1.getId());
        assertTrue(status1.getIsViewed());

        StoryStatusProjection status2 = findStatusById(result, story2.getId());
        assertFalse(status2.getIsViewed());
    }

    @Test
    void findStoryStatus_ShouldReturnEmpty_WhenNoStoriesFound() {
        List<StoryStatusProjection> result = storyRepository.findStoryStatus(
                user1.getEmail(), List.of(999L, 1000L)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findStoryStatus_ShouldHandleUserWithNoViewedStories() {
        TextStory story = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        List<StoryStatusProjection> result = storyRepository.findStoryStatus(
                "nonexistent@example.com", List.of(story.getId())
        );

        assertEquals(1, result.size());
        assertFalse(result.getFirst().getIsViewed());
    }

    @Test
    void findStoryById_ShouldReturnStory_WhenExistsAndAccessible() {
        TextStory story = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Specific story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        Optional<Story> result = storyRepository.findStoryById(
                user1.getEmail(),
                ChatType.INDIVIDUAL,
                cutoff,
                story.getId()
        );

        assertTrue(result.isPresent());
        assertEquals(story.getId(), result.get().getId());
        assertEquals("Specific story", result.get().getContent());
    }

    @Test
    void findStoryById_ShouldReturnEmpty_WhenStoryNotAccessible() {
        TextStory story = storyRepository.save(TextStory.builder()
                .user(user2)
                .storyType(StoryType.TEXT)
                .content("Blocked story")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);

        Optional<Story> result = storyRepository.findStoryById(
                user1.getEmail(),
                ChatType.INDIVIDUAL,
                cutoff,
                story.getId()
        );

        assertFalse(result.isPresent());
    }

    @Test
    void findStoriesByUserEmail_ShouldReturnUserStories() {
        storyRepository.save(TextStory.builder()
                .user(user1)
                .storyType(StoryType.TEXT)
                .content("User1 story 1")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(1800))
                .build());

        storyRepository.save(TextStory.builder()
                .user(user1)
                .storyType(StoryType.TEXT)
                .content("User1 story 2")
                .textColor("#000000")
                .backgroundColor("#FFFFFF")
                .createdAt(Instant.now().minusSeconds(900))
                .build());

        List<Story> result = storyRepository.findStoriesByUserEmail(user1.getEmail());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.getContent().equals("User1 story 1")));
        assertTrue(result.stream().anyMatch(s -> s.getContent().equals("User1 story 2")));
        assertNotNull(result.getFirst().getUser());
        assertNotNull(result.getFirst().getExcludedUsers());
    }

    private StoryStatusProjection findStatusById(List<StoryStatusProjection> statusProjections, Long storyId) {
        return statusProjections.stream()
                .filter(status -> status.getId().equals(storyId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Status not found for story ID: " + storyId));
    }
}