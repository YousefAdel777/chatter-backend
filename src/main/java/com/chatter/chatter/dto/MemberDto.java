package com.chatter.chatter.dto;

import com.chatter.chatter.model.MemberRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MemberDto {

    @NotNull
    private Long id;

    @NotNull
    private Long chatId;

    @NotNull
    private UserDto user;

    @NotNull
    private Instant joinedAt;

    @NotNull
    private MemberRole memberRole;

}