package com.chatter.chatter.unit.service;

import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.service.OnlineUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OnlineUserServiceTests {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private OnlineUserService onlineUserService;

    @Test
    void userConnected_ShouldAddUserToOnlineSet_WhenUserExistsAndShowsOnlineStatus() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("online_users", "1")).thenReturn(1L);

        onlineUserService.userConnected("test@example.com");

        verify(setOperations).add("online_users", "1");
        verify(userRepository).save(argThat(u -> u.getLastOnline() == null));
        verify(simpMessagingTemplate).convertAndSend("/topic/users.1", true);
    }

    @Test
    void userConnected_ShouldNotAddUser_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        onlineUserService.userConnected("nonexistent@example.com");

        verify(redisTemplate, never()).opsForSet();
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userConnected_ShouldNotAddUser_WhenUserHidesOnlineStatus() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(false)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        onlineUserService.userConnected("test@example.com");

        verify(redisTemplate, never()).opsForSet();
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userConnected_ShouldNotBroadcast_WhenUserAlreadyOnline() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("online_users", "1")).thenReturn(0L);

        onlineUserService.userConnected("test@example.com");

        verify(setOperations).add("online_users", "1");
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userDisconnected_ShouldRemoveUserFromOnlineSet_WhenUserExists() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("online_users", "1")).thenReturn(1L);

        onlineUserService.userDisconnected("test@example.com");

        verify(setOperations).remove("online_users", "1");
        verify(userRepository).save(argThat(u -> u.getLastOnline() != null));
        verify(simpMessagingTemplate).convertAndSend("/topic/users.1", false);
    }

    @Test
    void userDisconnected_ShouldNotRemoveUser_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        onlineUserService.userDisconnected("nonexistent@example.com");

        verify(redisTemplate, never()).opsForSet();
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userDisconnected_ShouldNotRemoveUser_WhenUserNotInOnlineSet() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("online_users", "1")).thenReturn(0L);

        onlineUserService.userDisconnected("test@example.com");

        verify(setOperations).remove("online_users", "1");
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userDisconnected_ShouldSetLastOnlineTimestamp() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("online_users", "1")).thenReturn(1L);

        onlineUserService.userDisconnected("test@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertNotNull(userCaptor.getValue().getLastOnline());
        assertTrue(userCaptor.getValue().getLastOnline().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(userCaptor.getValue().getLastOnline().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    void isOnline_ShouldReturnTrue_WhenUserIsOnline() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("online_users", "1")).thenReturn(true);

        boolean result = onlineUserService.isOnline(1L);

        assertTrue(result);
        verify(setOperations).isMember("online_users", "1");
    }

    @Test
    void isOnline_ShouldReturnFalse_WhenUserIsOffline() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("online_users", "1")).thenReturn(false);

        boolean result = onlineUserService.isOnline(1L);

        assertFalse(result);
        verify(setOperations).isMember("online_users", "1");
    }

    @Test
    void isOnline_ShouldReturnFalse_WhenRedisReturnsNull() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("online_users", "1")).thenReturn(null);

        boolean result = onlineUserService.isOnline(1L);

        assertFalse(result);
        verify(setOperations).isMember("online_users", "1");
    }

    @Test
    void userConnected_ShouldHandleNullAddedCount() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("online_users", "1")).thenReturn(null);

        onlineUserService.userConnected("test@example.com");

        verify(setOperations).add("online_users", "1");
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userDisconnected_ShouldHandleNullRemovedCount() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("online_users", "1")).thenReturn(null);

        onlineUserService.userDisconnected("test@example.com");

        verify(setOperations).remove("online_users", "1");
        verify(userRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), anyBoolean());
    }

    @Test
    void userConnected_ShouldBroadcastOnlineStatus_ForDifferentUserIds() {
        User user = User.builder()
                .id(999L)
                .email("user999@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("user999@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("online_users", "999")).thenReturn(1L);

        onlineUserService.userConnected("user999@example.com");

        verify(simpMessagingTemplate).convertAndSend("/topic/users.999", true);
    }

    @Test
    void userDisconnected_ShouldBroadcastOfflineStatus_ForDifferentUserIds() {
        User user = User.builder()
                .id(999L)
                .email("user999@example.com")
                .showOnlineStatus(true)
                .build();

        when(userRepository.findByEmail("user999@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("online_users", "999")).thenReturn(1L);

        onlineUserService.userDisconnected("user999@example.com");

        verify(simpMessagingTemplate).convertAndSend("/topic/users.999", false);
    }
}