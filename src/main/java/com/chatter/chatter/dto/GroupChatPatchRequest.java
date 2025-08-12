package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GroupChatPatchRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "description is required")
    private String description;

    private MultipartFile groupImage;

    private Boolean onlyAdminsCanSend;

    private Boolean onlyAdminsCanEditGroup;

    private Boolean onlyAdminsCanInvite;

    private Boolean onlyAdminsCanPin;

}
