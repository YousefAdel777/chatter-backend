package com.chatter.chatter.unit.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.React;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.ReactRepository;
import com.chatter.chatter.service.MessageService;
import com.chatter.chatter.service.ReactService;
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
public class ReactServiceTest {

    @Mock
    private ReactRepository reactRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReactService reactService;

    private User user;
    private Message message;
    private React react;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .build();

        message = Message.builder()
                .id(1L)
                .content("Test message")
                .build();

        react = React.builder()
                .id(1L)
                .emoji("testEmoji")
                .user(user)
                .message(message)
                .build();
    }

    @Test
    void createReact_ShouldCreateReact_WhenValid() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(reactRepository.existsByMessageIdAndUserEmail(1L, "test@example.com")).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(reactRepository.save(any(React.class))).thenReturn(react);

        React result = reactService.createReact("test@example.com", 1L, "testEmoji");

        assertNotNull(result);
        assertEquals("testEmoji", result.getEmoji());
        assertEquals(user, result.getUser());
        assertEquals(message, result.getMessage());
        verify(reactRepository).save(any(React.class));
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void createReact_ShouldThrowException_WhenReactAlreadyExists() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(reactRepository.existsByMessageIdAndUserEmail(1L, "test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            reactService.createReact("test@example.com", 1L, "testEmoji");
        });

        verify(reactRepository, never()).save(any(React.class));
        verify(messageService, never()).broadcastMessageUpdate(any());
    }

    @Test
    void deleteReact_ShouldDeleteReact_WhenValid() {
        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(react));

        reactService.deleteReact("test@example.com", 1L);

        verify(reactRepository).delete(react);
        verify(reactRepository).flush();
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void deleteReact_ShouldThrowException_WhenReactNotFound() {
        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            reactService.deleteReact("test@example.com", 1L);
        });

        verify(reactRepository, never()).delete(any(React.class));
        verify(messageService, never()).broadcastMessageUpdate(any());
    }

    @Test
    void updateReact_ShouldUpdateReact_WhenValid() {
        React updatedReact = React.builder()
                .id(1L)
                .emoji("updatedTestEmoji")
                .user(user)
                .message(message)
                .build();

        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(react));
        when(reactRepository.save(any(React.class))).thenReturn(updatedReact);

        React result = reactService.updateReact(1L, "test@example.com", "updatedTestEmoji");

        assertNotNull(result);
        assertEquals("updatedTestEmoji", result.getEmoji());
        verify(reactRepository).save(react);
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void updateReact_ShouldThrowException_WhenReactNotFound() {
        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            reactService.updateReact(1L, "test@example.com", "updatedTestEmoji");
        });

        verify(reactRepository, never()).save(any(React.class));
        verify(messageService, never()).broadcastMessageUpdate(any());
    }

    @Test
    void updateReact_ShouldNotUpdate_WhenEmojiIsNull() {
        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(react));
        when(reactRepository.save(any(React.class))).thenReturn(react);

        React result = reactService.updateReact(1L, "test@example.com", null);

        assertNotNull(result);
        assertEquals("testEmoji", result.getEmoji()); 
        verify(reactRepository).save(react);
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void updateReact_ShouldNotUpdate_WhenEmojiIsEmpty() {
        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(react));
        when(reactRepository.save(any(React.class))).thenReturn(react);

        React result = reactService.updateReact(1L, "test@example.com", "");

        assertNotNull(result);
        assertEquals("testEmoji", result.getEmoji()); 
        verify(reactRepository).save(react);
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void updateReact_ShouldUpdate_WhenEmojiIsBlankButNotNull() {
        React updatedReact = React.builder()
                .id(1L)
                .emoji("   ")
                .user(user)
                .message(message)
                .build();

        when(reactRepository.findByIdAndUserEmail(1L, "test@example.com")).thenReturn(Optional.of(react));
        when(reactRepository.save(any(React.class))).thenReturn(updatedReact);

        React result = reactService.updateReact(1L, "test@example.com", "   ");

        assertNotNull(result);
        assertEquals("   ", result.getEmoji());
        verify(reactRepository).save(react);
        verify(messageService).broadcastMessageUpdate(message);
        verify(messageService).evictMessageCaches(message);
    }

    @Test
    void createReact_ShouldThrowException_WhenMessageServiceThrows() {
        when(messageService.getMessageEntity("test@example.com", 1L))
                .thenThrow(new NotFoundException("message", "not found"));

        assertThrows(NotFoundException.class, () -> {
            reactService.createReact("test@example.com", 1L, "testEmoji");
        });

        verify(reactRepository, never()).save(any(React.class));
    }

    @Test
    void createReact_ShouldThrowException_WhenUserServiceThrows() {
        when(messageService.getMessageEntity("test@example.com", 1L)).thenReturn(message);
        when(reactRepository.existsByMessageIdAndUserEmail(1L, "test@example.com")).thenReturn(false);
        when(userService.getUserEntityByEmail("test@example.com"))
                .thenThrow(new NotFoundException("user", "not found"));

        assertThrows(NotFoundException.class, () -> {
            reactService.createReact("test@example.com", 1L, "testEmoji");
        });

        verify(reactRepository, never()).save(any(React.class));
    }
}