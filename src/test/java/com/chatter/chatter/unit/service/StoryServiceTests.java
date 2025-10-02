package com.chatter.chatter.unit.service;

import com.chatter.chatter.creator.StoryCreator;
import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.dto.StoryProjection;
import com.chatter.chatter.dto.StoryStatusProjection;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.factory.StoryFactory;
import com.chatter.chatter.mapper.StoryMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.StoryRepository;
import com.chatter.chatter.request.StoryPatchRequest;
import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.service.ChatService;
import com.chatter.chatter.service.StoryService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoryServiceTests {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UserService userService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryFactory storyFactory;

    @Mock
    private ChatService chatService;

    @Mock
    private StoryMapper storyMapper;

    @Mock
    private Cache storiesCache;

    @Mock
    private Cache currentUserStoriesCache;

    @InjectMocks
    private StoryService storyService;

    private User user1;
    private User user2;
    private User user3;
    private Story story;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .email("test1@example.com")
                .username("testUsername1")
                .password("testPassword1")
                .build();

        user2 = User.builder()
                .id(2L)
                .email("test2@example.com")
                .username("testUsername2")
                .password("testPassword2")
                .build();

        user3 = User.builder()
                .id(3L)
                .email("test3@example.com")
                .username("testUsername3")
                .password("testPassword3")
                .build();

        story = TextStory.builder()
                .id(1L)
                .user(user1)
                .build();
    }

    @Test
    void getStoryEntity_ShouldReturnStory_WhenExists() {
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(story));

        Story result = storyService.getStoryEntity(user1.getEmail(), 1L);

        assertNotNull(result);
        assertEquals(story, result);
    }

    @Test
    void getStoryEntity_ShouldThrowException_WhenNotFound() {
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> storyService.getStoryEntity(user1.getEmail(), 1L));
    }

    @Test
    void getStory_ShouldReturnDto() {
        StoryStatusProjection statusProjection = mock(StoryStatusProjection.class);
        StoryDto storyDto = mock(StoryDto.class);
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(story));
        when(storyRepository.findStoryStatus(eq(user1.getEmail()), eq(Set.of(1L))))
                .thenReturn(List.of(statusProjection));
        when(storyMapper.toDto(story, statusProjection, user1.getEmail())).thenReturn(storyDto);

        StoryDto result = storyService.getStory(user1.getEmail(), 1L);

        assertEquals(storyDto, result);
    }

    @Test
    void getStories_ShouldReturnDtoList() {
        List<Story> stories = Arrays.asList(story, mock(Story.class));
        List<StoryStatusProjection> statusProjections = Arrays.asList(mock(StoryStatusProjection.class), mock(StoryStatusProjection.class));
        List<StoryDto> storyDtos = Arrays.asList(mock(StoryDto.class), mock(StoryDto.class));
        when(storyRepository.findStories(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class)))
                .thenReturn(stories);
        when(storyRepository.findStoryStatus(eq(user1.getEmail()), anySet()))
                .thenReturn(statusProjections);
        when(storyMapper.toDtoList(stories, statusProjections, user1.getEmail())).thenReturn(storyDtos);

        List<StoryDto> result = storyService.getStories(user1.getEmail());

        assertEquals(storyDtos, result);
    }

    @Test
    void getCurrentUserStories_ShouldReturnDtoList() {
        List<Story> stories = Arrays.asList(story, mock(Story.class));
        List<StoryStatusProjection> statusProjections = Arrays.asList(mock(StoryStatusProjection.class), mock(StoryStatusProjection.class));
        List<StoryDto> storyDtos = Arrays.asList(mock(StoryDto.class), mock(StoryDto.class));
        when(storyRepository.findStoriesByUserEmail(user1.getEmail())).thenReturn(stories);
        when(storyRepository.findStoryStatus(eq(user1.getEmail()), anySet()))
                .thenReturn(statusProjections);
        when(storyMapper.toDtoList(stories, statusProjections, user1.getEmail())).thenReturn(storyDtos);

        List<StoryDto> result = storyService.getCurrentUserStories(user1.getEmail());

        assertEquals(storyDtos, result);
    }

    @Test
    void createStory_ShouldCreateStory_WithoutExcludedUsers() {
        StoryPostRequest request = new StoryPostRequest();
        request.setStoryType(StoryType.TEXT);
        StoryCreator storyCreator = mock(StoryCreator.class);
        Story newStory = TextStory.builder().build();
        when(userService.getUserEntityByEmail(user1.getEmail())).thenReturn(user1);
        when(storyFactory.getStoryCreator(StoryType.TEXT)).thenReturn(storyCreator);
        when(storyCreator.createStory(request)).thenReturn(newStory);
        when(storyRepository.save(newStory)).thenReturn(newStory);
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);
        when(userService.getContactsEntities(user1.getEmail())).thenReturn(List.of(user2));

        Story result = storyService.createStory(user1.getEmail(), request);

        assertNotNull(result);
        verify(storyRepository).save(newStory);
        verify(storiesCache).evict("email:" + user2.getEmail());
    }

    @Test
    void createStory_ShouldCreateStory_WithExcludedUsers() {
        StoryPostRequest request = new StoryPostRequest();
        request.setStoryType(StoryType.TEXT);
        request.setExcludedUserIds(Set.of(user2.getId()));
        StoryCreator storyCreator = mock(StoryCreator.class);
        Story newStory = TextStory.builder().build();
        when(userService.getUserEntityByEmail(user1.getEmail())).thenReturn(user1);
        when(storyFactory.getStoryCreator(StoryType.TEXT)).thenReturn(storyCreator);
        when(storyCreator.createStory(request)).thenReturn(newStory);
        when(userService.getUsersEntities(anySet())).thenReturn(List.of(user2));
        when(chatService.getChatByUsers(anySet())).thenReturn(mock(Chat.class));
        when(storyRepository.save(newStory)).thenReturn(newStory);
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);

        Story result = storyService.createStory(user1.getEmail(), request);

        assertNotNull(result);
        verify(storyRepository).save(newStory);
    }

    @Test
    void createStory_ShouldThrowException_WhenExcludedUserNotInChat() {
        StoryPostRequest request = new StoryPostRequest();
        request.setStoryType(StoryType.TEXT);
        request.setExcludedUserIds(Set.of(user2.getId()));
        StoryCreator storyCreator = mock(StoryCreator.class);
        Story newStory = TextStory.builder().build();
        when(userService.getUserEntityByEmail(user1.getEmail())).thenReturn(user1);
        when(storyFactory.getStoryCreator(StoryType.TEXT)).thenReturn(storyCreator);
        when(storyCreator.createStory(request)).thenReturn(newStory);
        when(userService.getUsersEntities(anySet())).thenReturn(List.of(user2));
        when(chatService.getChatByUsers(anySet())).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> storyService.createStory(user1.getEmail(), request));
        assertTrue(exception.getMessage().contains("cannot be added to excluded users"));
    }

    @Test
    void updateStory_ShouldUpdateStory_WhenUserIsOwner() {
        StoryPatchRequest request = new StoryPatchRequest();
        request.setExcludedUsersIds(Set.of(user2.getId()));
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(story));
        when(userService.getUsersEntities(anySet())).thenReturn(List.of(user2));
        when(chatService.getChatByUsers(anySet())).thenReturn(mock(Chat.class));
        when(storyRepository.save(story)).thenReturn(story);
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);

        Story result = storyService.updateStory(user1.getEmail(), 1L, request);

        assertNotNull(result);
        verify(storyRepository).save(story);
    }

    @Test
    void updateStory_ShouldThrowException_WhenUserNotOwner() {
        StoryPatchRequest request = new StoryPatchRequest();
        Story otherUserStory = TextStory.builder().user(user2).build();
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(otherUserStory));

        assertThrows(ForbiddenException.class, () -> storyService.updateStory(user1.getEmail(), 1L, request));
    }

    @Test
    void deleteStory_ShouldDeleteStory_WhenUserIsOwner() {
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(story));
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);

        storyService.deleteStory(user1.getEmail(), 1L);

        verify(storyRepository).delete(story);
    }

    @Test
    void deleteStory_ShouldThrowException_WhenUserNotOwner() {
        Story otherUserStory = TextStory.builder().user(user2).build();
        when(storyRepository.findStoryById(eq(user1.getEmail()), eq(ChatType.INDIVIDUAL), any(Instant.class), eq(1L)))
                .thenReturn(Optional.of(otherUserStory));

        assertThrows(ForbiddenException.class, () -> storyService.deleteStory(user1.getEmail(), 1L));
    }

    @Test
    void deleteExpiredStories_ShouldDeleteExpiredStories() {
        List<Story> expiredStories = List.of(story);
        when(storyRepository.findByCreatedAtBefore(any(Instant.class))).thenReturn(expiredStories);
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);
        when(cacheManager.getCache("currentUserStories")).thenReturn(currentUserStoriesCache);

        storyService.deleteExpiredStories();

        verify(storyRepository).deleteAll(expiredStories);
    }

    @Test
    void evictStoriesCache_ShouldEvictCache() {
        when(userService.getContactsEntities(user1.getEmail())).thenReturn(List.of(user2, user3));
        when(cacheManager.getCache("stories")).thenReturn(storiesCache);

        storyService.evictStoriesCache(user1.getEmail(), 1L);

        verify(storiesCache).evict("email:" + user2.getEmail());
        verify(storiesCache).evict("email:" + user3.getEmail());
        verify(storiesCache).evict("email:" + user2.getEmail() + ":storyId:1");
        verify(storiesCache).evict("email:" + user3.getEmail() + ":storyId:1");
    }

    @Test
    void evictStoriesCache_ShouldNotEvict_WhenCacheNull() {
        when(userService.getContactsEntities(user1.getEmail())).thenReturn(List.of(user2, user3));
        when(cacheManager.getCache("stories")).thenReturn(null);

        assertDoesNotThrow(() -> storyService.evictStoriesCache(user1.getEmail(), 1L));
    }

    @Test
    void evictCurrentUserStoriesCache_ShouldEvictCache() {
        when(cacheManager.getCache("currentUserStories")).thenReturn(currentUserStoriesCache);

        storyService.evictCurrentUserStoriesCache(user1.getEmail());

        verify(currentUserStoriesCache).evict("email:" + user1.getEmail());
    }

    @Test
    void evictCurrentUserStoriesCache_ShouldNotEvict_WhenCacheNull() {
        when(cacheManager.getCache("currentUserStories")).thenReturn(null);

        assertDoesNotThrow(() -> storyService.evictCurrentUserStoriesCache(user1.getEmail()));
    }

    @Test
    void getStoryStatusProjections_ShouldReturnProjections() {
        Set<Long> storyIds = Set.of(1L, 2L);
        List<StoryStatusProjection> projections = Arrays.asList(mock(StoryStatusProjection.class), mock(StoryStatusProjection.class));
        when(storyRepository.findStoryStatus(user1.getEmail(), storyIds)).thenReturn(projections);

        List<StoryStatusProjection> result = storyService.getStoryStatusProjections(user1.getEmail(), storyIds);

        assertEquals(projections, result);
    }
}