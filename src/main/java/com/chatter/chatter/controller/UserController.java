package com.chatter.chatter.controller;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.dto.UserRegisterDto;
import com.chatter.chatter.dto.UserUpdateDto;
import com.chatter.chatter.mapper.UserMapper;
import com.chatter.chatter.model.User;
import com.chatter.chatter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserRegisterDto userRegisterDto) {
        User createdUser = userService.createUser(userRegisterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(createdUser));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getUsers(
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String email,
        Pageable pageable
    ) {
        List<UserDto> users = userService.getAllUsers(username, email, pageable);
        Page<UserDto> usersPage = new PageImpl<>(users, pageable, users.size());
        return ResponseEntity.ok(usersPage);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getUserByEmail(principal.getName()));
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateUser(
            Principal principal,
            @RequestPart(value = "user") UserUpdateDto userUpdateDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        userUpdateDto.setImage(profileImage);
        User user = userService.updateUser(principal.getName(), userUpdateDto);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/me/contacts")
    public ResponseEntity<List<UserDto>> getContacts(Principal principal) {
        return ResponseEntity.ok(userService.getContacts(principal.getName()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(Principal principal) {
        userService.deleteUser(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
