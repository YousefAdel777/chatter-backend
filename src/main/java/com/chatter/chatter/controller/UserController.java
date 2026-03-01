package com.chatter.chatter.controller;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.model.UserPrincipal;
import com.chatter.chatter.model.UserStatus;
import com.chatter.chatter.request.PasswordResetRequest;
import com.chatter.chatter.request.UserRegisterRequest;
import com.chatter.chatter.request.UserPatchRequest;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.User;
import com.chatter.chatter.request.UserVerificationRequest;
import com.chatter.chatter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserRegisterRequest userRegisterDto) {
        User createdUser = userService.createUser(userRegisterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(createdUser));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getUsers(
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String email,
        Pageable pageable
    ) {
        PageDto<UserDto> usersPageDto = userService.getAllUsers(username, email, pageable);
        Page<UserDto> usersPage = new PageImpl<>(usersPageDto.getContent(), pageable, usersPageDto.getTotalElements());
        return ResponseEntity.ok(usersPage);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getUser(principal.getUser().getId()));
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart(value = "user") UserPatchRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        request.setImage(profileImage);
        User user = userService.updateUser(principal.getUser().getId(), request);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/me/contacts")
    public ResponseEntity<List<UserDto>> getContacts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getContacts(principal.getUser().getId()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.deleteUser(principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(
        @Valid @RequestBody PasswordResetRequest request
    ) {
        userService.updateUserPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok().build();
    }

}
