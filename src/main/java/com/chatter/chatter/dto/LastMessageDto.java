package com.chatter.chatter.dto;

import com.chatter.chatter.model.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LastMessageDto {

    @NotNull
    private Long id;

    private UserDto user;

    private MessageType messageType;

    private String content;

    private String title;

    private Instant createdAt;

    private Boolean missed;

    private Long duration;

    private String originalFileName;

    private Integer attachmentsCount;

}
