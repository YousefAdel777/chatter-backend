package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.StoryViewDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.mapper.StoryViewMapper;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.StoryView;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.StoryViewRepository;
import com.chatter.chatter.service.StoryService;
import com.chatter.chatter.service.StoryViewService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoryViewServiceTests {

    @Mock
    private StoryViewRepository storyViewRepository;

    @Mock
    private StoryService storyService;

    @Mock
    private UserService userService;

    @Mock
    private StoryViewMapper storyViewMapper;

    @InjectMocks
    private StoryViewService storyViewService;

    private User user;
    private User storyOwner;
    private Story story;
    private StoryView storyView;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .email("test@exaple.com")
                .username("testUser")
                .build();

        storyOwner = User.builder()
                .email("owner@example.com")
                .username("testOwner")
                .build();

        story = Story.builder()
                .id(1L)
                .user(storyOwner)
                .content("Test story")
                .build();

        storyView = StoryView.builder()
                .id(1L)
                .user(user)
                .story(story)
                .build();
    }

    @Test
    void getStoryViews_ShouldReturnViews_WhenUserIsStoryOwner() {
        when(storyService.getStoryEntity(storyOwner.getEmail(), 1L)).thenReturn(story);
        when(storyViewRepository.findByStoryId(1L)).thenReturn(List.of(storyView));
        when(storyViewMapper.toDtoList(any())).thenReturn(List.of(new StoryViewDto()));

        List<StoryViewDto> result = storyViewService.getStoryViews(storyOwner.getEmail(), 1L);

        assertNotNull(result);
        verify(storyService).getStoryEntity(storyOwner.getEmail(), 1L);
        verify(storyViewRepository).findByStoryId(1L);
        verify(storyViewMapper).toDtoList(any());
    }

    @Test
    void getStoryViews_ShouldThrowForbiddenException_WhenUserIsNotStoryOwner() {
        when(storyService.getStoryEntity(user.getEmail(), 1L)).thenReturn(story);

        assertThrows(ForbiddenException.class, () -> storyViewService.getStoryViews(user.getEmail(), 1L));

        verify(storyService).getStoryEntity(user.getEmail(), 1L);
        verifyNoInteractions(storyViewRepository, storyViewMapper);
    }

    @Test
    void createStoryView_ShouldCreateView_WhenValidRequest() {
        when(storyService.getStoryEntity(user.getEmail(), 1L)).thenReturn(story);
        when(storyViewRepository.existsByUserEmailAndStoryId(user.getEmail(), 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(storyViewRepository.save(any(StoryView.class))).thenReturn(storyView);

        StoryView result = storyViewService.createStoryView(user.getEmail(), 1L);

        assertNotNull(result);
        assertEquals(storyView, result);
        verify(storyService).getStoryEntity(user.getEmail(), 1L);
        verify(storyViewRepository).existsByUserEmailAndStoryId(user.getEmail(), 1L);
        verify(userService).getUserEntityByEmail(user.getEmail());
        verify(storyViewRepository).save(any(StoryView.class));
        verify(storyService).evictStoriesCache(user.getEmail(), 1L);
    }

    @Test
    void createStoryView_ShouldThrowBadRequestException_WhenUserIsStoryOwner() {
        when(storyService.getStoryEntity(storyOwner.getEmail(), 1L)).thenReturn(story);

        assertThrows(BadRequestException.class, () -> storyViewService.createStoryView(storyOwner.getEmail(), 1L));

        verify(storyService).getStoryEntity(storyOwner.getEmail(), 1L);
        verifyNoMoreInteractions(storyService);
        verifyNoInteractions(storyViewRepository, userService);
    }

    @Test
    void createStoryView_ShouldThrowBadRequestException_WhenViewAlreadyExists() {
        when(storyService.getStoryEntity(user.getEmail(), 1L)).thenReturn(story);
        when(storyViewRepository.existsByUserEmailAndStoryId(user.getEmail(), 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> storyViewService.createStoryView(user.getEmail(), 1L));

        verify(storyService).getStoryEntity(user.getEmail(), 1L);
        verify(storyViewRepository).existsByUserEmailAndStoryId(user.getEmail(), 1L);
        verifyNoMoreInteractions(storyService, storyViewRepository);
        verifyNoInteractions(userService);
    }

    @Test
    void createStoryView_ShouldCallEvictCache_WhenViewCreated() {
        when(storyService.getStoryEntity(user.getEmail(), 1L)).thenReturn(story);
        when(storyViewRepository.existsByUserEmailAndStoryId(user.getEmail(), 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(storyViewRepository.save(any(StoryView.class))).thenReturn(storyView);

        storyViewService.createStoryView(user.getEmail(), 1L);

        verify(storyService).evictStoriesCache(user.getEmail(), 1L);
    }
}