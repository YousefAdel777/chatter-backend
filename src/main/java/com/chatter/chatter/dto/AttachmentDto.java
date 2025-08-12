package com.chatter.chatter.dto;

import com.chatter.chatter.model.AttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AttachmentDto {

    @NotNull
    private Long id;

    @NotNull
    @NotBlank
    private String filePath;

    @NotNull
    private AttachmentType attachmentType;

    @NotNull
    private Long messageId;
}
