package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.UserPatchRequest;
import com.chatter.chatter.request.UserRegisterRequest;
import com.chatter.chatter.service.FileUploadService;
import com.chatter.chatter.service.OnlineUserService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_ShouldReturnDtos() {
        User user = User.builder().id(1L).username("testUsername").build();
        UserDto dto = UserDto.builder().id(1L).username("testUsername").build();

        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDtoList(List.of(user))).thenReturn(List.of(dto));

        PageDto<UserDto> result = userService.getAllUsers("testUsername", "testEmail@example.com", PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("testUsername", result.getContent().getFirst().getUsername());
    }

    @Test
    void getAllUsers_ReturnsEmptyPage_WhenNoResults() {
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);

        PageDto<UserDto> result = userService.getAllUsers(null, null, PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllUsers_RespectsSorting() {
        PageRequest sortedPage = PageRequest.of(0, 10, Sort.by("username").descending());

        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        userService.getAllUsers(null, null, sortedPage);

        verify(userRepository).findAll(any(Specification.class), eq(sortedPage));
    }

    @Test
    void getUserEntity_ShouldReturnEntity_WhenUserFound() {
        User user = User.builder()
                .id(1L)
                .email("testEmail@example.com")
                .username("testUsername")
                .password("testPassword")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User result = userService.getUserEntity(1L);
        assertEquals("testEmail@example.com", result.getEmail());
        assertEquals("testUsername", result.getUsername());
        assertEquals(1L, result.getId());
        assertEquals("testPassword", result.getPassword());
    }

    @Test
    void getUserEntity_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserEntity(1L));
    }

    @Test
    void getUser_ShouldReturnDto_WhenFound() {
        User user = User.builder()
                .id(1L)
                .email("testEmail@example.com")
                .username("testUsername")
                .build();
        UserDto dto = UserDto.builder()
                .id(1L)
                .email("testEmail@example.com")
                .username("testUsername")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.getUser(1L);
        assertEquals("testEmail@example.com", result.getEmail());
    }

    @Test
    void getUser_ShouldThrow_WhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUser(1L));
    }

    @Test
    void getUsersEntities_ShouldReturnEntities() {
        User user1 = User.builder().id(1L).username("testUsername").build();
        User user2 = User.builder().id(2L).username("testUsername2").build();
        List<Long> ids = List.of(1L, 2L);
        when(userRepository.findAllById(ids)).thenReturn(List.of(user1, user2));

        userService.getUsersEntities(ids);
        verify(userRepository).findAllById(ids);
    }

    @Test
    void getContactsEntities_ShouldReturnEntities() {
        User user = User.builder().id(1L).email("testEmail1@example.com").build();
        User contact = User.builder().id(2L).email("testEmail2@example.com").build();

        when(userRepository.findContacts(user.getEmail(), ChatType.INDIVIDUAL)).thenReturn(List.of(contact));

        List<User> result = userService.getContactsEntities(user.getEmail());

        verify(userRepository).findContacts(user.getEmail(), ChatType.INDIVIDUAL);
        assertEquals(1, result.size());
        assertEquals(contact.getEmail(), result.getFirst().getEmail());
    }


    @Test
    void getContacts_ShouldReturnDtos() {
        User user = User.builder().id(1L).email("testEmail1@example.com").build();
        User contact = User.builder().id(1L).email("testEmail2@example.com").build();
        UserDto dto = UserDto.builder().id(1L).email(contact.getEmail()).build();

        when(userRepository.findContacts(user.getEmail(), ChatType.INDIVIDUAL)).thenReturn(List.of(user));
        when(userMapper.toDtoList(List.of(user))).thenReturn(List.of(dto));

        List<UserDto> result = userService.getContacts(user.getEmail());

        verify(userRepository).findContacts(user.getEmail(), ChatType.INDIVIDUAL);
        assertEquals(1, result.size());
        assertEquals(contact.getEmail(), result.getFirst().getEmail());
    }

    @Test
    void getUserEntityByEmail_ShouldReturnEntity_WhenUserFound() {
        User user = User.builder().
                id(1L)
                .username("testUsername")
                .email("testEmail@example.com")
                .build();
        when(userRepository.findByEmail("testEmail@example.com")).thenReturn(Optional.of(user));
        User result = userService.getUserEntityByEmail("testEmail@example.com");
        assertEquals("testUsername", result.getUsername());
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserEntityByEmail_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findByEmail("testEmail@example.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserEntityByEmail("testEmail@example.com"));
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenUserFound() {
        String email = "testEmail@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);
        boolean result = userService.existsByEmail(email);
        assertTrue(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenUserNotFound() {
        String nonExistingEmail = "testEmail@example.com";
        when(userRepository.existsByEmail(nonExistingEmail)).thenReturn(false);

        boolean result = userService.existsByEmail(nonExistingEmail);

        assertFalse(result);
        verify(userRepository).existsByEmail(nonExistingEmail);
    }

    @Test
    void existsByEmail_ShouldThrowException_WhenEmailIsNull() {
        String nullEmail = null;
        assertThrows(IllegalArgumentException.class, () -> {
            userService.existsByEmail(nullEmail);
        });
        verify(userRepository, never()).existsByEmail(any());
    }


    @Test
    void createUser_ShouldSave_WhenEmailNotUsed() {
        UserRegisterRequest req = new UserRegisterRequest("testEmail@example.com", "testUsername", "testPassword");

        when(userRepository.existsByEmail("testEmail@example.com")).thenReturn(false);
        when(passwordEncoder.encode("testPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        User result = userService.createUser(req);

        verify(userRepository).save(any(User.class));
        assertEquals("testEmail@example.com", result.getEmail());
        assertEquals("testUsername", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
    }

    @Test
    void createUser_ShouldThrow_WhenEmailExists() {
        UserRegisterRequest req = new UserRegisterRequest("testEmail@example.com", "testUsername", "p");
        when(userRepository.existsByEmail("testEmail@example.com")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> userService.createUser(req));
    }

    @Test
    void updateUser_ShouldUpdateFields_CallOnlineUserService_EvictCaches() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        Cache cache = mock(Cache.class);

        User user = User.builder().id(1L).email("testEmail@example.com").username("testUsername").build();
        User contact = User.builder().id(1L).email("testEmail2@example.com").username("testUsername2").build();
        UserPatchRequest req = new UserPatchRequest();
        req.setUsername("newTestUsername");
        req.setImage(image);
        req.setShowOnlineStatus(true);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(fileUploadService.isImage(image)).thenReturn(true);
        when(fileUploadService.uploadFile(image)).thenReturn("imagePath");
        when(userRepository.findContacts(user.getEmail(),  ChatType.INDIVIDUAL)).thenReturn(List.of(contact));
        when(cacheManager.getCache("userContacts")).thenReturn(cache);

        User updated = userService.updateUser(user.getEmail(), req);

        assertEquals("newTestUsername", updated.getUsername());
        assertEquals("imagePath", updated.getImage());
        verify(onlineUserService).userConnected("testEmail@example.com");
        verify(userRepository).save(user);
        verify(cache).evict("email:" + contact.getEmail());
    }

    @Test
    void updateUser_ShouldThrow_WhenInvalidImage() {
        MultipartFile image = mock(MultipartFile.class);
        User user = User.builder().id(1L).email("testEmail@example.com").build();
        UserPatchRequest req = new UserPatchRequest();
        req.setImage(image);

        when(userRepository.findByEmail("testEmail@example.com")).thenReturn(Optional.of(user));
        when(fileUploadService.isImage(image)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.updateUser("testEmail@example.com", req));
    }

    @Test
    void updateUser_ShouldThrowRuntimeException_WhenImageUploadFails() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        User user = User.builder().id(1L).email("testEmail@example.com").build();
        UserPatchRequest req = new UserPatchRequest();
        req.setImage(image);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        when(fileUploadService.isImage(image)).thenReturn(true);
        when(fileUploadService.uploadFile(image)).thenThrow(new IOException());

        assertThrows(RuntimeException.class, () -> userService.updateUser(user.getEmail(), req));
    }

    @Test
    void updateUser_ShouldThrow_WhenNewEmailAlreadyExists() {
        UserPatchRequest req = new UserPatchRequest();
        req.setEmail("existing@example.com");

        User user = User.builder().email("old@example.com").build();
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.updateUser("old@example.com", req));
    }

    @Test
    void deleteUser_ShouldDeleteUserAndClearTheirContactsCache() {
        User user = User.builder()
                .id(1L)
                .email("testEmail1@example.com")
                .build();
        User contact = User.builder()
                .id(2L)
                .email("testEmail2@example.com")
                .build();
        Cache cache = mock(Cache.class);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findContacts(user.getEmail(), ChatType.INDIVIDUAL)).thenReturn(List.of(contact));
        when(cacheManager.getCache("users")).thenReturn(cache);
        when(cacheManager.getCache("userContacts")).thenReturn(cache);

        userService.deleteUser(user.getEmail());

        verify(onlineUserService).userDisconnected(user.getEmail());
        verify(userRepository).delete(user);
        verify(cache).evict("id:1");
        verify(cache).evict("email:" + contact.getEmail());
    }

    @Test
    void evictCurrentUserContactsCache_ShouldClearContactsCache() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("userContacts")).thenReturn(cache);
        userService.evictCurrentUserContactsCache("testEmail@example.com");
        verify(cache).evict("email:testEmail@example.com");
    }

    @Test
    void evictUserContactsCache_ShouldClearContactsCache() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("userContacts")).thenReturn(cache);

        User contact1 = User.builder().email("contact1@example.com").build();
        User contact2 = User.builder().email("contact2@example.com").build();
        when(userRepository.findContacts("testEmail@example.com", ChatType.INDIVIDUAL))
                .thenReturn(List.of(contact1, contact2));

        userService.evictUserContactsCache("testEmail@example.com");

        verify(cache).evict("email:" + contact1.getEmail());
        verify(cache).evict("email:"  + contact2.getEmail());
    }

}

