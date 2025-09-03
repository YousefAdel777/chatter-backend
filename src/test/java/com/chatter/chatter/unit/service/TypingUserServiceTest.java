package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.User;
import com.chatter.chatter.service.MemberService;
import com.chatter.chatter.service.TypingUserService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TypingUserServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TypingUserService typingUserService;

    @Test
    void addTypingUser_ShouldAddUserAndBroadcastDto_WhenUserIsMember() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(1L).email("test@example.com").build();
        UserDto userDto = UserDto.builder().id(1L).email("test@example.com").build();

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "1")).thenReturn(1L);

        typingUserService.addTypingUser(1L, "test@example.com");

        verify(setOperations).add("typing_users_chat:1", "1");
        verify(redisTemplate).expire("typing_users_chat:1", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.added-typing-users", userDto);
    }

    @Test
    void addTypingUser_ShouldNotAddUser_WhenUserIsNotMember() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(false);

        typingUserService.addTypingUser(1L, "test@example.com");

        verify(redisTemplate, never()).opsForSet();
        verify(userService, never()).getUserEntityByEmail("test@example.com");
        verify(simpMessagingTemplate, never()).convertAndSend(eq("/topic/chat.1.added-typing-users"), any(UserDto.class));
    }

    @Test
    void addTypingUser_ShouldNotBroadcast_WhenUserAlreadyTyping() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "1")).thenReturn(0L);

        typingUserService.addTypingUser(1L, "test@example.com");

        verify(setOperations).add("typing_users_chat:1", "1");
        verify(redisTemplate, never()).expire("typing_users_chat:1", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(userMapper, never()).toDto(user);
        verify(simpMessagingTemplate, never()).convertAndSend(eq("/topic/chat.1.added-typing-users"), any(UserDto.class));
    }

    @Test
    void addTypingUser_ShouldNotBroadcast_WhenAddedCountIsNull() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "1")).thenReturn(null);

        typingUserService.addTypingUser(1L, "test@example.com");

        verify(setOperations).add("typing_users_chat:1", "1");
        verify(redisTemplate, never()).expire("typing_users_chat:1", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(userMapper, never()).toDto(user);
        verify(simpMessagingTemplate, never()).convertAndSend(eq("/topic/chat.1.added-typing-users"), any(UserDto.class));
    }

    @Test
    void removeTypingUser_ShouldRemoveUserAndBroadcastUserId_WhenUserExists() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:1", "1")).thenReturn(1L);

        typingUserService.removeTypingUser(1L, "test@example.com");

        verify(setOperations).remove("typing_users_chat:1", "1");
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.removed-typing-users", 1L);
    }

    @Test
    void removeTypingUser_ShouldNotBroadcast_WhenUserNotTyping() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:1", "1")).thenReturn(0L);

        typingUserService.removeTypingUser(1L, "test@example.com");

        verify(setOperations).remove("typing_users_chat:1", "1");
        verify(simpMessagingTemplate, never()).convertAndSend("/topic/chat.1.removed-typing-users", 1L);
    }

    @Test
    void removeTypingUser_ShouldNotBroadcast_WhenRemovedCountIsNull() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:1", "1")).thenReturn(null);

        typingUserService.removeTypingUser(1L, "test@example.com");

        verify(setOperations).remove("typing_users_chat:1", "1");
        verify(simpMessagingTemplate, never()).convertAndSend("/topic/chat.1.removed-typing-users", 1L);
    }

    @Test
    void getTypingUsers_ShouldReturnUsers_WhenUserIsMember() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("typing_users_chat:1")).thenReturn(Set.of("1", "2"));

        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();
        when(userService.getUsersEntities(anyList())).thenReturn(List.of(user1, user2));

        List<User> result = typingUserService.getTypingUsers(1L, "test@example.com");

        assertEquals(2, result.size());
        verify(setOperations).members("typing_users_chat:1");
        verify(userService).getUsersEntities(anyList());
    }

    @Test
    void getTypingUsers_ShouldReturnEmptyList_WhenUserIsNotMember() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(false);

        List<User> result = typingUserService.getTypingUsers(1L, "test@example.com");

        assertTrue(result.isEmpty());
        verify(redisTemplate, never()).opsForSet();
        verify(userService, never()).getUsersEntities(List.of(1L, 2L));
    }

    @Test
    void getTypingUsers_ShouldReturnEmptyList_WhenEmailIsNull() {
        List<User> result = typingUserService.getTypingUsers(1L, null);

        assertTrue(result.isEmpty());
        verify(memberService, never()).isMember(anyString(), anyLong());
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void getTypingUsers_ShouldReturnEmptyList_WhenNoTypingUsers() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("typing_users_chat:1")).thenReturn(Collections.emptySet());

        List<User> result = typingUserService.getTypingUsers(1L, "test@example.com");

        assertTrue(result.isEmpty());
        verify(setOperations).members("typing_users_chat:1");
        verify(userService, never()).getUsersEntities(List.of(1L, 2L));
    }

    @Test
    void getTypingUsers_ShouldReturnEmptyList_WhenMembersIsNull() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("typing_users_chat:1")).thenReturn(null);

        List<User> result = typingUserService.getTypingUsers(1L, "test@example.com");

        assertTrue(result.isEmpty());
        verify(setOperations).members("typing_users_chat:1");
        verify(userService, never()).getUsersEntities(List.of(1L, 2L));
    }

    @Test
    void addTypingUser_ShouldHandleDifferentChatIds() {
        when(memberService.isMember("test@example.com", 999L)).thenReturn(true);

        User user = User.builder().id(1L).email("test@example.com").build();
        UserDto userDto = UserDto.builder().id(1L).email("test@example.com").build();

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:999", "1")).thenReturn(1L);

        typingUserService.addTypingUser(999L, "test@example.com");

        verify(setOperations).add("typing_users_chat:999", "1");
        verify(redisTemplate).expire("typing_users_chat:999", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.999.added-typing-users", userDto);
    }

    @Test
    void removeTypingUser_ShouldHandleDifferentChatIds() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:999", "1")).thenReturn(1L);

        typingUserService.removeTypingUser(999L, "test@example.com");

        verify(setOperations).remove("typing_users_chat:999", "1");
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.999.removed-typing-users", 1L);
    }

    @Test
    void addTypingUser_ShouldHandleDifferentUserIds() {
        when(memberService.isMember("user2@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(2L).email("user2@example.com").build();
        UserDto userDto = UserDto.builder().id(2L).email("user2@example.com").build();

        when(userService.getUserEntityByEmail("user2@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "2")).thenReturn(1L);

        typingUserService.addTypingUser(1L, "user2@example.com");

        verify(setOperations).add("typing_users_chat:1", "2");
        verify(redisTemplate).expire("typing_users_chat:1", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.added-typing-users", userDto);
    }

    @Test
    void removeTypingUser_ShouldHandleDifferentUserIds() {
        User user = User.builder().id(2L).email("user2@example.com").build();
        when(userService.getUserEntityByEmail("user2@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:1", "2")).thenReturn(1L);

        typingUserService.removeTypingUser(1L, "user2@example.com");

        verify(setOperations).remove("typing_users_chat:1", "2");
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.removed-typing-users", 2L);
    }

    @Test
    void addTypingUser_ShouldMapUserToDtoBeforeBroadcasting() {
        when(memberService.isMember("test@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(1L).email("test@example.com").username("testuser").build();
        UserDto userDto = UserDto.builder().id(1L).email("test@example.com").username("testuser").build();

        when(userService.getUserEntityByEmail("test@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "1")).thenReturn(1L);

        typingUserService.addTypingUser(1L, "test@example.com");

        verify(userMapper).toDto(user);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.added-typing-users", userDto);
    }

    @Test
    void addTypingUser_ShouldHandleDifferentEmails() {
        when(memberService.isMember("different@example.com", 1L)).thenReturn(true);

        User user = User.builder().id(3L).email("different@example.com").build();
        UserDto userDto = UserDto.builder().id(3L).email("different@example.com").build();

        when(userService.getUserEntityByEmail("different@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("typing_users_chat:1", "3")).thenReturn(1L);

        typingUserService.addTypingUser(1L, "different@example.com");

        verify(setOperations).add("typing_users_chat:1", "3");
        verify(redisTemplate).expire("typing_users_chat:1", 30, java.util.concurrent.TimeUnit.MINUTES);
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.added-typing-users", userDto);
    }

    @Test
    void removeTypingUser_ShouldHandleDifferentEmails() {
        User user = User.builder().id(3L).email("different@example.com").build();
        when(userService.getUserEntityByEmail("different@example.com")).thenReturn(user);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove("typing_users_chat:1", "3")).thenReturn(1L);

        typingUserService.removeTypingUser(1L, "different@example.com");

        verify(setOperations).remove("typing_users_chat:1", "3");
        verify(simpMessagingTemplate).convertAndSend("/topic/chat.1.removed-typing-users", 3L);
    }

    @Test
    void getTypingUsers_ShouldHandleDifferentEmails() {
        when(memberService.isMember("different@example.com", 1L)).thenReturn(true);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("typing_users_chat:1")).thenReturn(Set.of("1", "2"));

        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();
        when(userService.getUsersEntities(anyList())).thenReturn(List.of(user1, user2));

        List<User> result = typingUserService.getTypingUsers(1L, "different@example.com");

        assertEquals(2, result.size());
        verify(setOperations).members("typing_users_chat:1");
        verify(userService).getUsersEntities(anyList());
    }
}