package com.chatter.chatter.service;

import com.chatter.chatter.config.RabbitMQConfig;
import com.chatter.chatter.dto.*;
import com.chatter.chatter.event.OtpEmailEvent;
import com.chatter.chatter.event.UserDeletedEvent;
import com.chatter.chatter.event.UserRegisteredEvent;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.ForbiddenException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.Story;
import com.chatter.chatter.model.User;
import com.chatter.chatter.model.UserStatus;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.UserPatchRequest;
import com.chatter.chatter.request.UserRegisterRequest;
import com.chatter.chatter.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.user.default-image}")
    private String defaultUserImage;

    @Value("${app.upload.max-image-size}")
    private Long maxImageSize;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OnlineUserService onlineUserService;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    private final FileValidationService fileValidationService;
    private final FileUploadService fileUploadService;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OtpService otpService;

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

    public User getReference(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    @Cacheable(value = "users", key = "'id:' + #userId")
    public UserDto getUser(Long userId) {
        return userMapper.toDto(getUserEntity(userId));
    }

    public List<User> getUsersEntities(Iterable<Long> ids) {
        return userRepository.findAllById(ids);
    }

    public List<User> getUserEntitiesByChatMembership(Long chatId, Set<Long> usersIds) {
        return userRepository.findUsersByIdInAndChatMembership(usersIds, chatId);
    }

    public List<User> getContactsEntities(Long userId) {
        return userRepository.findContacts(userId);
    }

    @Cacheable(value = "userContacts", key = "'userId:' + #userId")
    public List<UserDto> getContacts(Long userId) {
        return userMapper.toDtoList(getContactsEntities(userId));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmailAndStatusVerified(email).orElseThrow(() -> new NotFoundException("user", "not found"));
    }

    @Cacheable(value = "users", key = "'email:' + #email")
    public UserDto getUserByEmail(String email) {
        return userMapper.toDto(getUserEntityByEmail(email));
    }

    @CacheEvict(value = "usersSearch", allEntries = true)
    @Transactional
    public User createUser(UserRegisterRequest userRegisterDto) {
        User user = userRepository.findByEmail(userRegisterDto.getEmail()).orElse(null);
        User.UserBuilder userBuilder = User.builder()
                .email(userRegisterDto.getEmail())
                .username(userRegisterDto.getUsername())
                .password(passwordEncoder.encode(userRegisterDto.getPassword()))
                .image(defaultUserImage);
        if (user != null) {
            if (UserStatus.VERIFIED.equals(user.getStatus())) {
                throw new BadRequestException("email", "A user with that email already exits");
            }
            userBuilder.id(user.getId());
        }
        user = userBuilder.build();
        User createdUser = userRepository.save(user);
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), user.getUsername()));
        return createdUser;
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "'id:' + #userId"),
        @CacheEvict(value = "usersSearch", allEntries = true)
    })
    @Transactional
    public User updateUser(Long userId, UserPatchRequest request) {
        User user = getUserEntity(userId);
        MultipartFile imageFile = request.getImage();
        boolean sendEvent = false;
        if (imageFile != null) {
            if (!fileValidationService.isImage(imageFile)) {
                throw new BadRequestException("image", "Invalid image file");
            }
            if (!fileValidationService.isSizeValid(imageFile, maxImageSize)) {
                throw new BadRequestException("image", imageFile.getOriginalFilename() + " exceeds the maximum allowed size of " + (maxImageSize / (1024 * 1024)) + " MB.");
            }
            String imagePath = fileUploadService.uploadFile(imageFile);
            user.setImage(imagePath);
            sendEvent = true;
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
            sendEvent = true;
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmailAndStatusVerified(request.getEmail())) {
                throw new BadRequestException("email", "A user with that email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getShowOnlineStatus() != null) {
            user.setShowOnlineStatus(request.getShowOnlineStatus());
            if (user.getShowOnlineStatus()) {
                onlineUserService.userConnected(user.getEmail());
            }
            else {
                onlineUserService.userDisconnected(user.getEmail());
            }
        }
        if (request.getShowMessageReads() != null) {
            user.setShowMessageReads(request.getShowMessageReads());
        }
        User updatedUser = userRepository.save(user);
        evictUserCacheByEmail(user.getEmail());
        evictUserContactsCache(userId);
        if (sendEvent) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.UPDATED_USERS, userMapper.toDto(updatedUser));
        }
        return updatedUser;
    }

    @Transactional
    public void updateUserPassword(String token, String newPassword) {
//        String email = (String) redisTemplate.opsForValue().get(token);
//        if (email == null) {
//            throw new BadRequestException("token", "Invalid token");
//        }
        String email = otpService.getVerifiedEmailByToken(token);
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        int count = userRepository.updateUserPasswordByEmail(email, encodedNewPassword);
        if (count == 0) {
            throw new NotFoundException("user", "not found");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
//                redisTemplate.delete(token);
                otpService.deleteToken(token);
            }
        });
    }

    @Transactional
    public String updateUserStatus(String token, UserStatus status) {
        String email = otpService.getVerifiedEmailByToken(token);
        int count = userRepository.updateUserStatusByEmail(email, status);
        if (count == 0) {
            throw new NotFoundException("user", "not found");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                otpService.deleteToken(token);
            }
        });
        return email;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #userId"),
            @CacheEvict(value = "usersSearch", allEntries = true)
    })
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        evictUserContactsCache(userId);
        evictUserCacheByEmail(user.getEmail());
        onlineUserService.userDisconnected(user.getEmail());
        userRepository.delete(user);
        rabbitTemplate.convertAndSend(RabbitMQConfig.DELETED_USERS, new UserDeletedEvent(userId));
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(userRepository.existsByEmailAndStatusVerified(email));
    }

    public void evictUserContactsCache(Long userId) {
        Cache userContactsCache = cacheManager.getCache("userContacts");
        if (userContactsCache == null) return;
        List<User> contactUsers = getContactsEntities(userId);
        for (User contactUser : contactUsers) {
            userContactsCache.evict("userId:" + contactUser.getId());
        }
    }

    public void evictCurrentUserContactsCache(Long userId) {
        Cache userContactsCache = cacheManager.getCache("userContacts");
        if (userContactsCache == null) return;
        userContactsCache.evict("userId:" + userId);
    }

    private void evictUserCacheByEmail(String email) {
        Cache userCache = cacheManager.getCache("users");
        if (userCache == null) return;
        userCache.evict("email:" + email);
    }

    @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    @Transactional
    public void deleteUnverifiedUsers() {
        userRepository.deleteUnverifiedUsers();
    }

}
