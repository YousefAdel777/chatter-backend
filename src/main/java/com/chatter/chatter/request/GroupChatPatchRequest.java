package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
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
