package com.chatter.chatter.dto;

import com.chatter.chatter.model.ChatType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class GroupChatPreviewDto {

    @NotNull
    private Long id;

    private Long membersCount;

    private String name;

    private String description;

    private String image;

    private ChatType chatType;

    private Instant createdAt;

}
