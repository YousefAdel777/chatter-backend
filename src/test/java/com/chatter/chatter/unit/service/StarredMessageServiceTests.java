package com.chatter.chatter.unit.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.StarredMessage;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.StarredMessageRepository;
import com.chatter.chatter.service.MessageService;
import com.chatter.chatter.service.StarredMessageService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StarredMessageServiceTests {

    @Mock
    private StarredMessageRepository starredMessageRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private StarredMessageService starredMessageService;

    private User user;
    private Message message;
    private StarredMessage starredMessage;

    @BeforeEach
    public void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .build();

        message = Message.builder()
                .id(1L)
                .content("Test message")
                .build();

        starredMessage = StarredMessage.builder()
                .id(1L)
                .user(user)
                .message(message)
                .build();
    }

    @Test
    void starMessage_ShouldStarMessage_WhenValid() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(starredMessageRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(starredMessageRepository.save(any(StarredMessage.class))).thenReturn(starredMessage);

        StarredMessage result = starredMessageService.starMessage("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(message, result.getMessage());
        verify(starredMessageRepository).save(any(StarredMessage.class));
        verify(messageService).evictMessagesCachesForUser("test@example.com");
    }

    @Test
    void starMessage_ShouldThrowException_WhenAlreadyStarred() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(starredMessageRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            starredMessageService.starMessage("test@example.com", 1L);
        });

        verify(starredMessageRepository, never()).save(any(StarredMessage.class));
        verify(messageService, never()).evictMessagesCachesForUser(anyString());
    }

    @Test
    void starMessage_ShouldThrowException_WhenMessageServiceThrows() {
        when(messageService.getMessageEntity("test@example.com", 1L))
                .thenThrow(new BadRequestException("message", "not found"));

        assertThrows(BadRequestException.class, () -> {
            starredMessageService.starMessage("test@example.com", 1L);
        });

        verify(starredMessageRepository, never()).save(any(StarredMessage.class));
    }

    @Test
    void starMessage_ShouldThrowException_WhenUserServiceThrows() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(starredMessageRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com"))
                .thenThrow(new BadRequestException("user", "not found"));

        assertThrows(BadRequestException.class, () -> {
            starredMessageService.starMessage("test@example.com", 1L);
        });

        verify(starredMessageRepository, never()).save(any(StarredMessage.class));
    }

    @Test
    void unstarMessage_ShouldUnstarMessage_WhenValid() {
        when(starredMessageRepository.findByUserEmailAndMessageId("test@example.com", 1L))
                .thenReturn(Optional.of(starredMessage));

        starredMessageService.unstarMessage("test@example.com", 1L);

        verify(starredMessageRepository).delete(starredMessage);
        verify(messageService).evictMessagesCachesForUser("test@example.com");
    }

    @Test
    void unstarMessage_ShouldThrowException_WhenNotStarred() {
        when(starredMessageRepository.findByUserEmailAndMessageId("test@example.com", 1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> starredMessageService.unstarMessage("test@example.com", 1L));

        verify(starredMessageRepository, never()).delete(any(StarredMessage.class));
        verify(messageService, never()).evictMessagesCachesForUser(anyString());
    }

    @Test
    void unstarMessage_ShouldEvictCaches_AfterUnstarring() {
        when(starredMessageRepository.findByUserEmailAndMessageId("test@example.com", 1L))
                .thenReturn(Optional.of(starredMessage));

        starredMessageService.unstarMessage("test@example.com", 1L);

        verify(messageService).evictMessagesCachesForUser("test@example.com");
    }

    @Test
    void starMessage_ShouldEvictCaches_AfterStarring() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(starredMessageRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(starredMessageRepository.save(any(StarredMessage.class))).thenReturn(starredMessage);

        starredMessageService.starMessage("test@example.com", 1L);

        verify(messageService).evictMessagesCachesForUser("test@example.com");
    }

    @Test
    void starMessage_ShouldBuildCorrectStarredMessage() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(starredMessageRepository.existsByUserEmailAndMessageId("test@example.com", 1L)).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(starredMessageRepository.save(any(StarredMessage.class))).thenReturn(starredMessage);

        starredMessageService.starMessage("test@example.com", 1L);

        verify(starredMessageRepository).save(argThat(starred ->
                starred.getUser().equals(user) && starred.getMessage().equals(message)
        ));
    }
}