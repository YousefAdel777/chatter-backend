package com.chatter.chatter.mapper;

import com.chatter.chatter.dto.UserDto;
import com.chatter.chatter.model.User;
import com.chatter.chatter.service.FileUploadService;
import io.netty.handler.codec.http.multipart.FileUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final FileUploadService fileUploadService;

    final String baseUrl = "http://localhost:8080";

    public UserDto toDto(User user) {
        if (user == null) return null;
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .bio(user.getBio())
                .lastOnline(user.getLastOnline())
                .showOnlineStatus(user.getShowOnlineStatus())
                .showMessageReads(user.getShowMessageReads())
                .build();

        if (user.getImage().contains("static")) {
            userDto.setImage(baseUrl + user.getImage().split("static")[1]);
        }
        else {
//            String[] splitted = user.getImage().split("/");
//            String filename = splitted[splitted.length - 1];
            userDto.setImage(fileUploadService.getFileUrl(user.getImage()));
        }
        return userDto;
    }

    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) return null;
        return users.stream().map(this::toDto).collect(Collectors.toList());
    }

}
