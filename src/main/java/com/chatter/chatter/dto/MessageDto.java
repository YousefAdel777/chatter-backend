package com.chatter.chatter.dto;

import com.chatter.chatter.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MessageDto {

    @NotNull
    private Long id;

    @NotNull
    private UserDto user;

    @NotNull
    private Long chatId;

    private String content;

    private Instant createdAt;

    private MessageType messageType;

    @Builder.Default
    private Set<ReactDto> reacts = new HashSet<>();

    @Builder.Default
    private boolean isSeen = false;

    @Builder.Default
    private boolean isEdited = false;

    @Builder.Default
    private boolean isForwarded = false;

    @Builder.Default
    private List<AttachmentDto> attachments = new ArrayList<>();

    @NotBlank
    private String originalFileName;

    private Instant expiresAt;

    private Boolean missed;

    private Long duration;

    @NotBlank
    private String fileUrl;

    private Long fileSize;

    @NotBlank
    private String title;

    @Builder.Default
    private Boolean multiple = false;

    private List<OptionDto> options;

    private Instant endsAt;

    private StoryDto story;

    @Builder.Default
    private Boolean pinned = false;

    @Builder.Default
    private Boolean starred = false;

    private MessageDto replyMessage;

    private InviteDto invite;

}