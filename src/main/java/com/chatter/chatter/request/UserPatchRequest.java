package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserPatchRequest {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "email is required")
    private String email;

    private MultipartFile image;

    private String bio;

    private Boolean showOnlineStatus;

    private Boolean showMessageReads;

}
