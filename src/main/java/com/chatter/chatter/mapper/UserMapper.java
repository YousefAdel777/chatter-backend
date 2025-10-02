package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.model.User;
import com.chatter.chatter.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final FileUploadService fileUploadService;

    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .bio(user.getBio())
                .lastOnline(user.getLastOnline())
                .showOnlineStatus(user.getShowOnlineStatus())
                .showMessageReads(user.getShowMessageReads())
                .image(fileUploadService.getFileUrl(user.getImage()))
                .build();
    }

    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) return null;
        return users.stream().map(this::toDto).collect(Collectors.toList());
    }

}
