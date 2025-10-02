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

    private String contentJson;

    private Instant createdAt;

    private MessageType messageType;

    @Builder.Default
    private List<ReactDto> reacts = new ArrayList<>();

    @Builder.Default
    private Boolean isSeen = false;

    @Builder.Default
    private Boolean isEdited = false;

    @Builder.Default
    private Boolean isForwarded = false;

    @Builder.Default
    private List<AttachmentDto> attachments = new ArrayList<>();

    @NotBlank
    private String originalFileName;

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

    private MessagePreviewDto replyMessage;

    private InviteDto invite;

    @Builder.Default
    private List<MentionDto> mentions = new ArrayList<>();

    @Builder.Default
    private Boolean isEveryoneMentioned = false;


}