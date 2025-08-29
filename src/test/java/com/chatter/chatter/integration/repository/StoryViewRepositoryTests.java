package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryType;
import com.chatter.chatter.model.StoryView;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.StoryRepository;
import com.chatter.chatter.repository.StoryViewRepository;
import com.chatter.chatter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class StoryViewRepositoryTests {

    @Autowired
    private StoryViewRepository storyViewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    private User user1;
    private User user2;
    private Story story1;
    private Story story2;

    @BeforeEach
    public void setup() {
        storyViewRepository.deleteAll();
        storyRepository.deleteAll();
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

        story1 = storyRepository.save(Story.builder()
                .content("Story 1 content")
                .user(user1)
                .storyType(StoryType.TEXT)
                .createdAt(Instant.now().minusSeconds(3600))
                .build());

        story2 = storyRepository.save(Story.builder()
                .content("Story 2 content")
                .user(user1)
                .storyType(StoryType.IMAGE)
                .createdAt(Instant.now().minusSeconds(1800))
                .build());
    }

    @Test
    void shouldSaveStoryView_WhenValid() {
        StoryView storyView = StoryView.builder()
                .user(user1)
                .story(story1)
                .build();

        StoryView savedStoryView = storyViewRepository.save(storyView);

        assertNotNull(savedStoryView);
        assertNotNull(savedStoryView.getId());
        assertEquals(user1, savedStoryView.getUser());
        assertEquals(story1, savedStoryView.getStory());
        assertNotNull(savedStoryView.getCreatedAt());
    }

    @Test
    void shouldFindByStoryId_WhenStoryViewsExist() {
        StoryView view1 = storyViewRepository.save(StoryView.builder()
                .user(user1)
                .story(story1)
                .build());

        StoryView view2 = storyViewRepository.save(StoryView.builder()
                .user(user2)
                .story(story1)
                .build());

        storyViewRepository.save(StoryView.builder()
                .user(user1)
                .story(story2)
                .build());

        List<StoryView> story1Views = storyViewRepository.findByStoryId(story1.getId());

        assertEquals(2, story1Views.size());
        assertTrue(story1Views.stream().anyMatch(v -> v.getId().equals(view1.getId())));
        assertTrue(story1Views.stream().anyMatch(v -> v.getId().equals(view2.getId())));
    }

    @Test
    void shouldReturnEmptyList_WhenFindByStoryId_AndNoViewsExist() {
        List<StoryView> views = storyViewRepository.findByStoryId(999L);
        assertTrue(views.isEmpty());
    }

    @Test
    void shouldReturnTrue_WhenExistsByUserEmailAndStoryId_AndViewExists() {
        storyViewRepository.save(StoryView.builder()
                .user(user1)
                .story(story1)
                .build());

        boolean exists = storyViewRepository.existsByUserEmailAndStoryId(user1.getEmail(), story1.getId());
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByUserEmailAndStoryId_AndViewDoesNotExist() {
        boolean exists = storyViewRepository.existsByUserEmailAndStoryId("nonexistent@example.com", story1.getId());
        assertFalse(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByUserEmailAndStoryId_AndUserNotViewed() {
        storyViewRepository.save(StoryView.builder()
                .user(user2)
                .story(story1)
                .build());

        boolean exists = storyViewRepository.existsByUserEmailAndStoryId(user1.getEmail(), story1.getId());
        assertFalse(exists);
    }

    @Test
    void shouldReturnFalse_WhenExistsByUserEmailAndStoryId_AndStoryNotViewed() {
        storyViewRepository.save(StoryView.builder()
                .user(user1)
                .story(story2)
                .build());

        boolean exists = storyViewRepository.existsByUserEmailAndStoryId(user1.getEmail(), story1.getId());
        assertFalse(exists);
    }

    @Test
    void shouldPreventDuplicateViews_WhenSameUserAndStory() {
        StoryView view1 = StoryView.builder()
                .user(user1)
                .story(story1)
                .build();

        StoryView view2 = StoryView.builder()
                .user(user1)
                .story(story1)
                .build();

        storyViewRepository.save(view1);
        assertThrows(DataIntegrityViolationException.class, () -> storyViewRepository.saveAndFlush(view2));
    }
}