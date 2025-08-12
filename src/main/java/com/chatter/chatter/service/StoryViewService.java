package com.chatter.chatter.service;

import com.chatter.chatter.dto.StoryViewDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.mapper.StoryMapper;
import com.chatter.chatter.mapper.StoryViewMapper;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryView;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.StoryViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryViewService {

    private final StoryViewRepository storyViewRepository;
    private final StoryService storyService;
    private final UserService userService;
    private final StoryViewMapper storyViewMapper;

    @Cacheable(value = "storyViews", key = "'storyId:' + #storyId")
    public List<StoryViewDto> getStoryViews(String email, Long storyId) {
        Story story = storyService.getStoryEntity(email, storyId);
        if (!email.equals(story.getUser().getEmail())) {
            throw new ForbiddenException("Only story creator can see story views");
        }
        return storyViewMapper.toDtoList(storyViewRepository.findByStoryId(storyId));
    }

    @Transactional
    @CacheEvict(value = "storyViews", key = "'storyId:' + #storyId")
    public StoryView createStoryView(String email, Long storyId) {
        Story story = storyService.getStoryEntity(email, storyId);
        if (email.equals(story.getUser().getEmail())) {
            throw new BadRequestException("user", "User cannot create view for their own story.");
        }
        if (storyViewRepository.existsByUserEmailAndStoryId(email, storyId)) {
            throw new BadRequestException("message", "Story view already exists.");
        }
        User user = userService.getUserEntityByEmail(email);
        StoryView storyView = StoryView.builder()
                .user(user)
                .story(story)
                .build();
        StoryView createdStoryView = storyViewRepository.save(storyView);
        storyService.evictStoriesCache(email, storyId);
        return createdStoryView;
    }

}
