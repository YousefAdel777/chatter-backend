package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GroupChatPostRequest {

    @NotBlank(message = "name is required")
    @NotNull(message = "name is required")
    private String name;

    @NotBlank(message = "description is required")
    @NotNull(message = "description is required")
    private String description;

    private MultipartFile groupImage;

    private Boolean onlyAdminsCanSend = false;

    private Boolean onlyAdminsCanEditGroup = true;

    private Boolean onlyAdminsCanInvite = true;

    private Boolean onlyAdminsCanPin = true;

}
