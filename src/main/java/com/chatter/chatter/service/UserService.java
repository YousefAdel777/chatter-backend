package com.chatter.chatter.service;

import com.chatter.chatter.dto.*;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.UserPatchRequest;
import com.chatter.chatter.request.UserRegisterRequest;
import com.chatter.chatter.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.user.default-image}")
    private String defaultUserImage;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final OnlineUserService onlineUserService;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    @Cacheable(
            value = "usersSearch",
            key = "'search:' + (#username != null ? #username : 'null') + ':' + (#email != null ? #email : 'null') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + (#pageable.sort != null ? #pageable.sort : 'unsorted')"
    )
    public PageDto<UserDto> getAllUsers(String username, String email, Pageable pageable) {
        Specification<User> specification = UserSpecification.withFilters(username, email);
        Page<User> usersPage = userRepository.findAll(specification, pageable);
        List<UserDto> users = userMapper.toDtoList(usersPage.getContent());
        return new PageDto<>(users, usersPage.getTotalElements());
    }

    public User getUserEntity(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user", "not found"));
    }

    @Cacheable(value = "users", key = "'id:' + #userId")
    public UserDto getUser(Long userId) {
        return userMapper.toDto(getUserEntity(userId));
    }

    public List<User> getUsersEntities(Iterable<Long> ids) {
        return userRepository.findAllById(ids);
    }

    public List<User> getContactsEntities(String email) {
        return userRepository.findContacts(email, ChatType.INDIVIDUAL);
    }

    @Cacheable(value = "userContacts", key = "'email:' + #email")
    public List<UserDto> getContacts(String email) {
        return userMapper.toDtoList(getContactsEntities(email));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("user", "not found"));
    }

    @Cacheable(value = "users", key = "'email:' + #email")
    public UserDto getUserByEmail(String email) {
        return userMapper.toDto(getUserEntityByEmail(email));
    }

    @CacheEvict(value = "usersSearch", allEntries = true)
    @Transactional
    public User createUser(UserRegisterRequest userRegisterDto) {
        if (existsByEmail(userRegisterDto.getEmail())) {
            throw new BadRequestException("email", "A user with that email already exits");
        }
        User user = User.builder()
                .email(userRegisterDto.getEmail())
                .username(userRegisterDto.getUsername())
                .password(passwordEncoder.encode(userRegisterDto.getPassword()))
                .image(defaultUserImage)
                .build();
        return userRepository.save(user);
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "'id:' + @userService.getUserEntityByEmail(#email).id", condition = "#result != null"),
        @CacheEvict(value = "users", key = "'email:' + #email"),
        @CacheEvict(value = "usersSearch", allEntries = true)
    })
    @Transactional
    public User updateUser(String email, UserPatchRequest userUpdateDto) {
        User user =  getUserEntityByEmail(email);
        MultipartFile imageFile = userUpdateDto.getImage();
        if (imageFile != null) {
            try {
                if (!fileUploadService.isImage(imageFile)) {
                    throw new BadRequestException("image", "Invalid image file");
                }
                String imagePath = fileUploadService.uploadFile(imageFile);
                user.setImage(imagePath);
            }
            catch (IOException exception) {
                throw new RuntimeException("Failed to upload profile image: " + exception.getMessage());
            }
        }
        if (userUpdateDto.getUsername() != null) {
            user.setUsername(userUpdateDto.getUsername());
        }
        if (userUpdateDto.getBio() != null) {
            user.setBio(userUpdateDto.getBio());
        }
        if (userUpdateDto.getEmail() != null && !email.equals(userUpdateDto.getEmail())) {
            if (userRepository.existsByEmail(userUpdateDto.getEmail())) {
                throw new BadRequestException("email", "A user with that email already exists");
            }
            user.setEmail(userUpdateDto.getEmail());
        }
        if (userUpdateDto.getShowOnlineStatus() != null) {
            user.setShowOnlineStatus(userUpdateDto.getShowOnlineStatus());
            if (user.getShowOnlineStatus()) {
                onlineUserService.userConnected(user.getEmail());
            }
            else {
                onlineUserService.userDisconnected(user.getEmail());
            }
        }
        if (userUpdateDto.getShowMessageReads() != null) {
            user.setShowMessageReads(userUpdateDto.getShowMessageReads());
        }
        userRepository.save(user);
        evictUserContactsCache(email);
        return user;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'email:' + #email"),
            @CacheEvict(value = "users", key = "'contacts:' + #email"),
            @CacheEvict(value = "usersSearch", allEntries = true)
    })
    public void deleteUser(String email) {
        User user = getUserEntityByEmail(email);
        evictUserContactsCache(email);
        Cache cache = cacheManager.getCache("users");
        if (cache != null) cache.evict("id:" + user.getId());
        onlineUserService.userDisconnected(user.getEmail());
        userRepository.delete(user);
    }

    public boolean existsByEmail(String email) {
        if (email == null) throw new IllegalArgumentException("email cannot be null");
        return userRepository.existsByEmail(email);
    }

    public void evictUserContactsCache(String email) {
//        evictCurrentUserContactsCache(email);
        Cache userContactsCache = cacheManager.getCache("userContacts");
        if (userContactsCache == null) return;
        List<User> contactUsers = getContactsEntities(email);
        for (User contactUser : contactUsers) {
            userContactsCache.evict("email:" + contactUser.getEmail());
        }
    }

    public void evictCurrentUserContactsCache(String email) {
        Cache userContactsCache = cacheManager.getCache("userContacts");
        if (userContactsCache == null) return;
        userContactsCache.evict("email:" + email);
    }

}
