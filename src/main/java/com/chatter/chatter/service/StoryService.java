package com.chatter.chatter.service;

import com.chatter.chatter.creator.StoryCreator;
import com.chatter.chatter.dto.StoryDto;
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
        return storyRepository.findStoryById(email, ChatType.INDIVIDUAL, Instant.now().minus(Duration.ofHours(24)), storyId).orElseThrow(() -> new BadRequestException("story", "not found"));
    }

    @Cacheable(value = "stories", key = "'email:' + #email + ':storyId:' + #storyId")
    public StoryDto getStory(String email, Long storyId) {
        return storyMapper.toDto(getStoryEntity(email, storyId), email);
    }

    @Cacheable(value = "stories", key = "'email:' + #email")
    public List<StoryDto> getStories(String email) {
        return storyMapper.toDtoList(storyRepository.findStories(email, ChatType.INDIVIDUAL, Instant.now().minus(Duration.ofHours(24))), email);
    }

    @Cacheable(value = "currentUserStories", key = "'email:' + #email")
    public List<StoryDto> getCurrentUserStories(String email) {
        return storyMapper.toDtoList(storyRepository.findStoriesByUserEmail(email), email);
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
        evictStoriesCache(email, storyId);
        storyRepository.save(story);
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

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    @Async
    public void deleteExpiredStories() {
        List<Story> stories = storyRepository.findByCreatedAtBefore(Instant.now().minus(Duration.ofHours(24)));
        for (Story story : stories) {
            evictCurrentUserStoriesCache(story.getUser().getEmail());
            evictStoriesCache(story.getUser().getEmail(), story.getId());
        }
        storyRepository.deleteAll(stories);
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
