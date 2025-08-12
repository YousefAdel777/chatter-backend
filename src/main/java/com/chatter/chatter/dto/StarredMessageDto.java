package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StarredMessageDto {

    @NotNull
    private Long id;

    @NotNull
    private Long messageId;

    @NotNull
    private Long userId;

    @NotNull
    private Instant createdAt;

}
