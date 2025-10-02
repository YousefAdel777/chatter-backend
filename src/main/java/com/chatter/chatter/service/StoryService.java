package com.chatter.chatter.service;

import com.chatter.chatter.creator.StoryCreator;
import com.chatter.chatter.dto.StoryDto;
import com.chatter.chatter.dto.StoryProjection;
import com.chatter.chatter.dto.StoryStatusProjection;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.StoryProjectionMapper;
import com.chatter.chatter.request.StoryPostRequest;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.factory.StoryFactory;
import com.chatter.chatter.mapper.StoryMapper;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.StoryRepository;
import com.chatter.chatter.request.StoryPatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final UserService userService;
    private final StoryFactory storyFactory;
    private final ChatService chatService;
    private final StoryMapper storyMapper;
    private final CacheManager cacheManager;

    public Story getStoryEntity(String email, Long storyId) {
        return storyRepository.findStoryById(email, ChatType.INDIVIDUAL, Instant.now().minus(Duration.ofHours(24)), storyId).orElseThrow(() -> new NotFoundException("story", "not found"));
    }

    @Cacheable(value = "stories", key = "'email:' + #email + ':storyId:' + #storyId")
    public StoryDto getStory(String email, Long storyId) {
        Story story = getStoryEntity(email, storyId);
        StoryStatusProjection statusProjection = getStoryStatusProjections(email, Set.of(storyId)).getFirst();
        return storyMapper.toDto(story, statusProjection, email);
    }

    @Cacheable(value = "stories", key = "'email:' + #email")
    public List<StoryDto> getStories(String email) {
        List<Story> stories = storyRepository.findStories(email, ChatType.INDIVIDUAL, Instant.now().minus(Duration.ofHours(24)));
        Set<Long> storiesIds =  stories.stream().map(Story::getId).collect(Collectors.toSet());
        List<StoryStatusProjection> projections = getStoryStatusProjections(email, storiesIds);
        return storyMapper.toDtoList(stories, projections, email);
    }

    @Cacheable(value = "currentUserStories", key = "'email:' + #email")
    public List<StoryDto> getCurrentUserStories(String email) {
        List<Story> stories = storyRepository.findStoriesByUserEmail(email);
        Set<Long> storiesIds = stories.stream().map(Story::getId).collect(Collectors.toSet());
        List<StoryStatusProjection> statusProjections = getStoryStatusProjections(email, storiesIds);
        return storyMapper.toDtoList(stories, statusProjections, email);
    }

    @Transactional
    @CacheEvict(value = "currentUserStories", key = "'email:' + #email")
    public Story createStory(
            String email,
            StoryPostRequest request
    ) {
        User user = userService.getUserEntityByEmail(email);
        StoryCreator storyCreator = storyFactory.getStoryCreator(request.getStoryType());
        Story story = storyCreator.createStory(request);
        story.setUser(user);
        if (request.getExcludedUserIds() != null) {
            excludeUsers(request.getExcludedUserIds(), story, user.getId());
        }
        storyRepository.save(story);
        evictStoriesCache(email, null);
        return story;
    }

    @Transactional
    @CacheEvict(value = "currentUserStories", key = "'email:' + #email")
    public Story updateStory(String email, Long storyId, StoryPatchRequest request) {
        Story story = getStoryEntity(email, storyId);
        if (!story.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("You are not allowed to update this story");
        }
        Set<Long> ids = request.getExcludedUsersIds();
        if (ids != null) {
            story.clearExcludedUsers();
            excludeUsers(ids, story, story.getUser().getId());
        }
        storyRepository.save(story);
        evictStoriesCache(email, storyId);
        return story;
    }

    @Transactional
    @CacheEvict(value = "currentUserStories", key = "'email:' + #email")
    public void deleteStory(String email, Long storyId) {
        Story story = getStoryEntity(email, storyId);
        if (!story.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("You are not allowed to delete this story");
        }
        evictStoriesCache(email, storyId);
        storyRepository.delete(story);
    }

    private void excludeUsers(Set<Long> excludedUsersIds, Story story, Long currentUserId) {
        List<User> users = userService.getUsersEntities(excludedUsersIds);
        for (User excludedUser : users) {
            Chat chat = chatService.getChatByUsers(new HashSet<>(List.of(excludedUser.getId(), currentUserId)));
            if (chat == null) {
                throw new BadRequestException("message", "User with id: " + excludedUser.getId() + "cannot be added to excluded users");
            }
            story.addExcludedUser(excludedUser);
        }
    }

    @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    @Transactional
    public void deleteExpiredStories() {
        List<Story> stories = storyRepository.findByCreatedAtBefore(Instant.now().minus(Duration.ofHours(24)));
        for (Story story : stories) {
            evictCurrentUserStoriesCache(story.getUser().getEmail());
            evictStoriesCache(story.getUser().getEmail(), story.getId());
        }
        storyRepository.deleteAll(stories);
    }

    public List<StoryStatusProjection> getStoryStatusProjections(String email, Set<Long> storyIds) {
        return storyRepository.findStoryStatus(email, storyIds);
    }

    public void evictStoriesCache(String email, Long storyId) {
        List<User> contactUsers = userService.getContactsEntities(email);
        Cache storiesCache = cacheManager.getCache("stories");
        if (storiesCache != null) {
            for (User contactUser : contactUsers) {
                storiesCache.evict("email:" + contactUser.getEmail());
                if (storyId != null) {
                    storiesCache.evict("email:" + contactUser.getEmail() + ":storyId:" + storyId);
                }
            }
        }
    }

    public void evictCurrentUserStoriesCache(String email) {
        Cache currentUserStoriesCache = cacheManager.getCache("currentUserStories");
        if (currentUserStoriesCache != null) {
            currentUserStoriesCache.evict("email:" + email);
        }
    }

}
